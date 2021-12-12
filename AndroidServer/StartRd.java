package com.example.server;

import java.io.FileDescriptor;

public class StartRd implements Runnable {
    ScreenRecorder screenRecorder;
    private Size size;
    private FileDescriptor fileDescriptor;

    public StartRd(Size size, FileDescriptor fileDescriptor) {
        this.screenRecorder = new ScreenRecorder();
        this.size = size;
        this.fileDescriptor = fileDescriptor;
    }

    @Override
    public void run() {
        System.out.println("开始ScreenRecorder");
        screenRecorder = new ScreenRecorder();
        // 开始将数据不断的写入
        screenRecorder.record(size.getWidth(), size.getHeight(), fileDescriptor);
        System.out.println("结束ScreenRecorder");
    }

    public void requestStop() {
        screenRecorder.setStop(true);
    }
}

