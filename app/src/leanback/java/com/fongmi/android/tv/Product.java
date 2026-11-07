package com.fongmi.android.tv;

import com.fongmi.android.tv.setting.PlayerSetting;

public class Product {

    public static int getDeviceType() {
        return 0;
    }

    public static int getColumn() {
        return Math.abs(PlayerSetting.getSize() - 7);
    }
}
