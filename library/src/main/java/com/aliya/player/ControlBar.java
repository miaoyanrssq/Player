package com.aliya.player;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aliya.player.utils.VideoUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.aliya.player.utils.VideoUtils.findViewById;

/**
 * ControlBar
 *
 * @author a_liYa
 * @date 2017/8/11 21:29.
 */
public class ControlBar {

    private ImageView ivPause;

    private SeekBar seekBar;

    private TextView tvPosition;

    private TextView tvDuration;

    private ImageView ivFullscreen;

    private View rootView;

    private Controller controller;

    private TimeTool timeTool;
    private int showTimeoutMs;
    private long hideAtMs;
    private boolean isAttachedToWindow;

    private ComponentListener componentListener;

    public static final int DEFAULT_SHOW_TIMEOUT_MS = 3000;

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public ControlBar(Controller controller) {
        this.controller = controller;
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;
        timeTool = new TimeTool();
        componentListener = new ComponentListener();
    }

    public void bindView(View view) {

        if (rootView != null) {
            rootView.removeOnAttachStateChangeListener(componentListener);
        }

        rootView = view;

        rootView.addOnAttachStateChangeListener(componentListener);

        ivPause = findViewById(rootView, R.id.player_play_pause);
        seekBar = findViewById(rootView, R.id.player_seek_bar);
        tvPosition = findViewById(rootView, R.id.player_position);
        tvDuration = findViewById(rootView, R.id.player_duration);
        ivFullscreen = findViewById(rootView, R.id.player_full_screen);

        if (ivPause != null) {
            ivPause.setOnClickListener(componentListener);
        }
        if (ivFullscreen != null) {
            ivFullscreen.setOnClickListener(componentListener);
        }
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(componentListener);
        }

    }

    public void stopUpdateProgress() {
        if (rootView != null) {
            rootView.removeCallbacks(updateProgressAction);
        }
    }

    public void updateProgress() {

        Player player = getPlayer();
        if (player == null) return;

        timeTool.calcTime(player);

        if (tvPosition != null) {
            tvPosition.setText(VideoUtils.formatTime(timeTool.position));
        }
        if (tvDuration != null) {
            tvDuration.setText(VideoUtils.formatTime(timeTool.duration));
        }

        if (seekBar != null && seekBar.getVisibility() == VISIBLE) {
            if (timeTool.duration > 0) {
                int progress = (int)
                        (seekBar.getMax() * timeTool.position / timeTool.duration + 0.5f);

                if (!componentListener.seekBarIsDragging) {
                    if (progress > seekBar.getMax()) {
                        progress = seekBar.getMax();
                    }
                    seekBar.setProgress(progress);
                }

                int bufferProgress = (int)
                        (seekBar.getMax() * timeTool.bufferedPosition / timeTool.duration + 0.5f);
                if (bufferProgress > seekBar.getMax()) {
                    bufferProgress = seekBar.getMax();
                }
                seekBar.setSecondaryProgress(bufferProgress);
            }
        }

        // Cancel any pending updates and schedule a new one if necessary.
        stopUpdateProgress();

        int playbackState = player == null ? Player.STATE_IDLE : player.getPlaybackState();
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            long delayMs;
            if (player.getPlayWhenReady() && playbackState == Player.STATE_READY) {
                delayMs = VideoUtils.calcSyncPeriod(timeTool.position);
            } else {
                delayMs = 1000;
            }
            rootView.postDelayed(updateProgressAction, delayMs);
        }

    }

    public void updatePlayPause(boolean playWhenReady) {
        if (ivPause != null) {
            ivPause.setImageResource(playWhenReady
                    ? R.mipmap.module_player_controls_pause : R.mipmap.module_player_controls_play);
        }
    }

    public void hideAfterTimeout() {
        rootView.removeCallbacks(hideAction);

        if (getPlayer() != null && !getPlayer().getPlayWhenReady()) {
            hideAtMs = C.TIME_UNSET;
        } else if (showTimeoutMs > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMs;
            if (isAttachedToWindow) {
                rootView.postDelayed(hideAction, showTimeoutMs);
            }
        } else {
            hideAtMs = C.TIME_UNSET;
        }
    }

    public void hide() {
        if (isVisible()) {
            setVisibility(GONE);
            rootView.removeCallbacks(hideAction);
            hideAtMs = C.TIME_UNSET;
        }
    }

    public void setVisibility(int visibility) {
        if (rootView != null) {
            rootView.setVisibility(visibility);
            if (visibility != VISIBLE) {
                stopUpdateProgress();
            } else {
                updateProgress();
                hideAfterTimeout();
            }
        }
    }

    /**
     * 切换可见性 - 显示／隐藏
     *
     * @return true表示切换为显示; false表示切换为隐藏
     */
    public boolean switchVisibility() {
        if (rootView == null) return false;

        setVisibility(isVisible() ? GONE : VISIBLE);

        return isVisible();
    }

    public boolean isVisible() {
        return rootView.getVisibility() == VISIBLE;
    }

    private Player getPlayer() {
        return controller != null ? controller.getPlayer() : null;
    }

    private final class ComponentListener implements View.OnClickListener,
            SeekBar.OnSeekBarChangeListener, View.OnAttachStateChangeListener {

        private boolean seekBarIsDragging;

        @Override
        public void onClick(View v) {
            Player player = getPlayer();
            if (player != null) {
                if (v.getId() == R.id.player_play_pause) {
                    player.setPlayWhenReady(!player.getPlayWhenReady());
                } else if (v.getId() == R.id.player_full_screen) {
                    Log.e("TAG", "player_full_screen");
                }
            }
            hideAfterTimeout();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (timeTool != null && tvPosition != null) {
                tvPosition.setText(
                        VideoUtils.formatTime(timeTool.duration * progress / seekBar.getMax()));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seekBarIsDragging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            seekBarIsDragging = false;
            if (controller != null) {
                controller.seekTo(timeTool.duration * seekBar.getProgress() / seekBar.getMax());
            }
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            isAttachedToWindow = true;
            if (hideAtMs != C.TIME_UNSET) {
                long delayMs = hideAtMs - SystemClock.uptimeMillis();
                if (delayMs <= 0) {
                    hide();
                } else {
                    rootView.postDelayed(hideAction, delayMs);
                }
            }
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            isAttachedToWindow = false;
            rootView.removeCallbacks(hideAction);
        }
    }

}
