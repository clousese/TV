package com.fongmi.android.tv.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.playback.ExternalPlayback;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.api.node.NodeRuntime;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.DLNARendererService;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.github.catvod.net.OkHttp;

// LIVE-ONLY: HomeActivity is no longer the VOD home page.
// It only exists so existing callers (ExternalActivity, RestartActivity,
// NodeRuntime, PlaybackService) keep compiling, and immediately forwards
// to LiveActivity. The VOD home UI is gone.
public class HomeActivity extends BaseActivity {

    private static volatile int instanceCount;

    public static boolean isRunning() {
        return instanceCount > 0;
    }

    @Override
    protected ViewBinding getBinding() {
        return ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instanceCount++;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_VIEW.equals(intent.getAction())) {
            ExternalPlayback.open(this, intent, this::loadLive);
        } else {
            LiveActivity.start(this);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != ExternalPlayback.REQUEST_CODE) return;
        setResult(resultCode, data);
        finish();
    }

    private void loadLive(String url) {
        if (isFinishing() || isDestroyed()) return;
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                if (getIntent().getBooleanExtra(ExternalPlayback.FORWARD_RESULT, false)) startActivityForResult(new Intent(HomeActivity.this, LiveActivity.class).putExtra("empty", LiveConfig.isEmpty()), ExternalPlayback.REQUEST_CODE);
                else LiveActivity.start(getActivity());
                if (!getIntent().getBooleanExtra(ExternalPlayback.FORWARD_RESULT, false)) finish();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                if (getIntent().getBooleanExtra(ExternalPlayback.FORWARD_RESULT, false)) finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (--instanceCount == 0 && !isChangingConfigurations()) {
            releaseResources();
        }
        super.onDestroy();
    }

    public static void releaseResources() {
        if (isRunning() || com.fongmi.android.tv.service.PlaybackService.isRunning()) return;
        DLNARendererService.stop(App.get());
        LiveConfig.get().clear();
        OkHttp.get().clear();
        Source.get().exit();
        Server.get().stop();
        NodeRuntime.get().releaseIfUnused();
    }
}
