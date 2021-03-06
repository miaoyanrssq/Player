package com.aliya.player;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.lang.ref.SoftReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Player 助手
 *
 * @author a_liYa
 * @date 2017/8/13 21:51.
 */
public class PlayerHelper {

    private Context context;
    private Handler mainHandler;
    private String userAgent;

    public void setContext(Context context) {
        if (context == null) return;

        this.context = context.getApplicationContext();

        if (userAgent == null) {
            userAgent = Util.getUserAgent(context, getAppName());
        }
    }

    public Context getContext() {
        return context;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public MediaSource buildMediaSource(Uri uri, String overrideExtension, DefaultBandwidthMeter
            bandwidthMeter) {
        int type = TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
                : Util.inferContentType("." + overrideExtension);
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(bandwidthMeter),
                        new DefaultSsChunkSource.Factory(buildDataSourceFactory(bandwidthMeter)),
                        mainHandler, null);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(bandwidthMeter),
                        new DefaultDashChunkSource.Factory(buildDataSourceFactory(bandwidthMeter)),
                        mainHandler,
                        null);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, buildDataSourceFactory(bandwidthMeter), mainHandler,
                        null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, buildDataSourceFactory(bandwidthMeter), new
                        DefaultExtractorsFactory(),
                        mainHandler, null);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(context, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(getUserAgent(), bandwidthMeter);
    }

    private String getAppName() {
        if (context != null) {
            try {
                PackageManager packageManager = context.getPackageManager();
                PackageInfo pkg = packageManager.getPackageInfo(context.getPackageName(), 0);

                return pkg.applicationInfo.loadLabel(packageManager).toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return "Player";
    }


    private static SoftReference<ExecutorService> threadExecutorSoft;

    public static ExecutorService getThreadExecutor() {
        ExecutorService service;
        if (threadExecutorSoft == null || (service = threadExecutorSoft.get()) == null) {
            service = Executors.newSingleThreadExecutor();
            threadExecutorSoft = new SoftReference<>(service);
        }
        return service;
    }


}
