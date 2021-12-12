package com.example.server;

import android.os.IInterface;

public class DisplayManager {
    private final IInterface service;

    public DisplayManager(IInterface service) {
        this.service = service;
    }

    public DisplayInfo getDisplayInfo() {
        try {
            Object displayInfo = service.getClass().getMethod("getDisplayInfo", int.class)
                    .invoke(service, 0);
            Class<?> cls = displayInfo.getClass();
            // width and height already take the rotation into account
            int width = cls.getDeclaredField("logicalWidth").getInt(displayInfo);
            int height = cls.getDeclaredField("logicalHeight").getInt(displayInfo);
            int rotation = cls.getDeclaredField("rotation").getInt(displayInfo);
            return new DisplayInfo(new Size(width, height), rotation);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e);
        }
    }
}
