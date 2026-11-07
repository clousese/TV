package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;

public class RestartActivity extends Activity {

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, RestartActivity.class).putExtra("pid", Process.myPid()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pid = getIntent().getIntExtra("pid", -1);
        if (pid <= 0 || pid == Process.myPid()) {
            finish();
            return;
        }
        Process.killProcess(pid);
        startActivity(Intent.makeRestartActivityTask(new ComponentName(this, HomeActivity.class)));
        finish();
        Process.killProcess(Process.myPid());
    }

}
