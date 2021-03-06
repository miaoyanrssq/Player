package com.aliya.player.ui.control;

import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aliya.player.PlayerCallback;
import com.aliya.player.PlayerManager;
import com.aliya.player.R;
import com.aliya.player.ui.Controller;
import com.aliya.player.ui.PlayerView;
import com.aliya.player.utils.Utils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.aliya.player.utils.Utils.findViewById;
import static com.aliya.player.utils.Utils.setText;

/**
 * NavBarControl
 *
 * @author a_liYa
 * @date 2017/8/11 21:29.
 */
public class NavBarControl extends AbsControl {

    private ImageView ivPause;

    private SeekBar seekBar;

    private TextView tvPosition;

    private TextView tvDuration;

    private ImageView ivFullscreen;

    private View rootView;

    private int showTimeoutMs;
    private long hideAtMs;
    private boolean isAttachedToWindow;

    private ComponentListener componentListener;

    public static final int DEFAULT_SHOW_TIMEOUT_MS = 3000;

    private final Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            setVisibility(false);
        }
    };

    public NavBarControl(Controller controller) {
        super(controller);
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;
        componentListener = new ComponentListener();
    }

    public void onViewCreate(View view) {

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

    public void updateProgress() {

        CalcTime calcTime = controller.getCalcTime();

        if (tvPosition != null && !componentListener.seekBarIsDragging) {
            setText(tvPosition, calcTime.formatPosition());
        }
        if (tvDuration != null) {
            setText(tvDuration, calcTime.formatDuration());
        }

        if (seekBar != null && seekBar.getVisibility() == VISIBLE) {
            if (calcTime.duration > 0) {
                int progress = calcTime.calcProgress(seekBar.getMax());

                if (!componentListener.seekBarIsDragging) {
                    if (progress > seekBar.getMax()) {
                        progress = seekBar.getMax();
                    }
                    seekBar.setProgress(progress);
                }

                int bufferProgress = calcTime.calcSecondaryProgress(seekBar.getMax());

                if (bufferProgress > seekBar.getMax()) {
                    bufferProgress = seekBar.getMax();
                }
                seekBar.setSecondaryProgress(bufferProgress);
            }
        }

    }

    public void updateIcPlayPause(boolean playWhenReady) {
        if (ivPause != null) {
            ivPause.setImageResource(playWhenReady
                    ? R.mipmap.module_player_controls_pause : R.mipmap.module_player_controls_play);
        }
    }

    public void updateIcFullscreen() {
        if (ivFullscreen != null && getPlayerView() != null) {
            ivFullscreen.setImageResource(getPlayerView().isFullscreen()
                    ? R.mipmap.module_player_controls_retract : R.mipmap
                    .module_player_controls_spread);
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

    @Override
    public void setVisibility(boolean isVisible) {
        if (rootView != null) {
            boolean oldVisible = isVisible();
            rootView.setVisibility(isVisible ? VISIBLE : GONE);
            if (oldVisible != isVisible) {
                if (visibilityListener != null) {
                    visibilityListener.onVisibilityChange(this, isVisible);
                }
            }
            if (isVisible) {
                updateProgress();
                hideAfterTimeout();
            } else {
                rootView.removeCallbacks(hideAction);
                hideAtMs = C.TIME_UNSET;
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

        setVisibility(!isVisible());

        return isVisible();
    }

    @Override
    public boolean isVisible() {
        return rootView.getVisibility() == VISIBLE;
    }

    private final class ComponentListener implements View.OnClickListener,
            SeekBar.OnSeekBarChangeListener, View.OnAttachStateChangeListener {

        private boolean seekBarIsDragging;

        @Override
        public void onClick(View v) {
            final Player player = getPlayer();
            if (player != null) {
                if (v.getId() == R.id.player_play_pause) {
                    boolean playWhenReady = player.getPlayWhenReady();
                    player.setPlayWhenReady(!playWhenReady);

                    PlayerCallback callback = PlayerManager.getPlayerCallback(getParentView());
                    if (callback != null) {
                        if (playWhenReady) { // true -> false
                            callback.onPause(getPlayerView());
                        } else { // false -> true
                            callback.onPlay(getPlayerView());
                        }
                    }
                } else if (v.getId() == R.id.player_full_screen) {
                    PlayerView playerView = getPlayerView();
                    if (playerView != null) {
                        PlayerCallback callback = PlayerManager.getPlayerCallback(getParentView());
                        if (callback != null) {
                            callback.onFullscreenChange(!playerView.isFullscreen(), playerView);
                        }
                        playerView.switchFullScreen();
                    }
                }
            }
            hideAfterTimeout();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (tvPosition != null) {
                setText(tvPosition, Utils.formatTime(
                        controller.getCalcTime().duration * progress / seekBar.getMax()));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seekBarIsDragging = true;
            rootView.removeCallbacks(hideAction);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            seekBarIsDragging = false;
            controller.seekTo(
                    controller.getCalcTime().duration * seekBar.getProgress() / seekBar.getMax());
            // 暂停时，拖动进度，自动播放
            final Player player = getPlayer();
            if (player != null && !player.getPlayWhenReady()) {
                player.setPlayWhenReady(true);
            }
            hideAfterTimeout();
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            isAttachedToWindow = true;
            if (hideAtMs != C.TIME_UNSET) {
                long delayMs = hideAtMs - SystemClock.uptimeMillis();
                if (delayMs <= 0) {
                    setVisibility(false);
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
