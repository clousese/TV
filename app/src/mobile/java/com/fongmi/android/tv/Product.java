package com.fongmi.android.tv;

import android.content.Context;
import android.os.Build;
import android.view.DisplayCutout;

import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static int getDeviceType() {
        return 1;
    }

    public static int getColumn(Context context) {
        int count = ResUtil.isLand(context) ? 7 : 5;
        count = count + (ResUtil.isPad() ? 1 : 0);
        return Math.abs(PlayerSetting.getSize() - count);
    }

    public static int[] getSpec(Context context) {
        int column = getColumn(context);
        int space = ResUtil.dp2px(32) + ResUtil.dp2px(16 * (column - 1)) + getCutout(context);
        int base = ResUtil.getScreenWidth(context) - space;
        int width = base / column;
        int height = (int) (width / 0.75f);
        return new int[]{width, height};
    }

    private static int getCutout(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0;
        DisplayCutout cutout = ResUtil.getDisplay(context).getCutout();
        if (cutout == null) return 0;
        int left = cutout.getSafeInsetLeft();
        int right = cutout.getSafeInsetRight();
        return left | right;
    }
}
