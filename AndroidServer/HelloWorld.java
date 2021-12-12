package com.example.server;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello World");
        ServiceManager serviceManager = new ServiceManager();
        // 获取屏幕信息服务
        DisplayManager displayManager = serviceManager.getDisplayManager();
        // 获取屏幕的信息
        DisplayInfo displayInfo = displayManager.getDisplayInfo();
        final Size size = displayInfo.getSize();
        System.out.println("getDisplayInfo display=" + displayInfo);
        // 获取电源服务，判断是否亮屏
        PowerManager powerManager = serviceManager.getPowerManager();
        boolean screenOn = powerManager.isScreenOn();
        System.out.println("is screen on=" + screenOn);
        // 如果屏幕没有点亮，则点亮屏幕
        if (!screenOn) {
            clickPowerKey(serviceManager);
        }

        // 开始连接Socket
        LocalServerSocket serverSocket = null;
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StartRd startRd = null;
        Thread thread = null;

        try {
            // 使用unix domain name进行标识，方便获取file descriptor
            serverSocket = new LocalServerSocket("recorder");
            LocalSocket accept = serverSocket.accept();
            System.out.println("Connection Success！！");
            final FileDescriptor fileDescriptor = accept.getFileDescriptor();
            sendScreenInfo(size, buffer, fileDescriptor);

            // 开启屏幕录制线程
            startRd = new StartRd(size, fileDescriptor);
            thread = new Thread(startRd);
            thread.start();
            // 开启socket的事件监听线程
            boolean eof = false;
            startRd = event_loop(serviceManager, size, buffer, startRd, fileDescriptor, eof);
            System.out.println("break loop");
            // 停止线程时同时停止录制
            if (startRd != null) {
                startRd.requestStop();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ErrnoException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 事件监听
    private static StartRd event_loop(ServiceManager serviceManager, Size size, ByteBuffer buffer, StartRd startRd, FileDescriptor fileDescriptor, boolean eof) throws ErrnoException, InterruptedIOException, UnsupportedEncodingException {
        Thread thread;
        do {
            //  因为读到的数据，可能不是一次读完。所以需要保存读取的状态
            int read = Os.read(fileDescriptor, buffer);

            if (read == -1 || read == 0) {
                // 如果这个时候read 0 的话。就结束
                break;
            }
            else {
                // 将缓冲区中内容变为准备读取状态
                buffer.flip();
                // 读取buffer中第一位
                byte b = buffer.get(0);
                // 如果是0的话，就当作是Action
                if (b == 0 && read > 1) {
                    byte type = buffer.get(1);
                    // 点击事件 0 0/1 Xh Xl Yh Yl
                    if (type < 2 && read == 6) {
                        if(type == 0) System.out.println("click down event");
                        else System.out.println("click up event");
                        // 判断当前读取的数据是否够，不够还需要继续读取
                        buffer.position(1);
                        int x = buffer.get(2) << 8 | buffer.get(3) & 0xff;
                        int y = buffer.get(4) << 8 | buffer.get(5) & 0xff;
                        System.out.println("x=" + x);
                        System.out.println("y=" + y);
                        // 接受到事件进行处理
                        boolean key = injectClick(serviceManager, type, x, y);
                        System.out.println("click result = " + key);
                        buffer.clear();
                    }
                    // 滚轮事件 0 2 Xh Xl Yh Yl Hs[3] Hs[2] Hs[1] Hs[0] Vs[3] Vs[2] Vs[1] Vs[0]
                    else if (type == 2 && read == 14) {
                        System.out.println("remaining" + buffer.remaining());
                        buffer.position(1);
                        int x = buffer.get(2) << 8 | buffer.get(3) & 0xff;
                        int y = buffer.get(4) << 8 | buffer.get(5) & 0xff;
                        int hs = buffer.get(6) << 24 | buffer.get(7) << 16 | buffer.get(8) << 8 | buffer.get(9);
                        int vs = buffer.get(10) << 24 | buffer.get(11) << 16 | buffer.get(12) << 8 | buffer.get(13);
                        System.out.println("x=" + x);
                        System.out.println("y=" + y);
                        System.out.println("hs=" + hs);
                        System.out.println("vs=" + vs);
                        // 接受到事件进行处理
                        boolean key = injectScroll(serviceManager, x, y, hs, vs);
                        System.out.println("scroll result = " + key);
                        buffer.clear();
                    }
                    // 键盘输入事件: type = 3 control+ H = home  control+b = back
                    // 0 3(type) 0(up)/1(down) 3(home)/4(back)
                    else if (type == 3 && read == 4) {
                        System.out.println("enter key event");
                        int action = buffer.get(2) == 1 ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
                        int keyCode = buffer.get(3);
                        // 接受到事件进行处理
                        boolean key = injectKeyEvent(serviceManager, action, keyCode);
                        System.out.println("enter key result = " + key);
                        buffer.clear();
                    }
                    // 拖动事件 0 4 Xh Xl Yh Yl
                    else if(type == 4 && read == 6) {
                        System.out.println("finger move event");
                        // 从buffer中读取数据
                        int x = buffer.get(2) << 8 | buffer.get(3) & 0xff;
                        int y = buffer.get(4) << 8 | buffer.get(5) & 0xff;
                        System.out.println("x=" + x);
                        System.out.println("y=" + y);
                        // 接受到事件进行处理
                        boolean key = injectMove(serviceManager, x, y);
                        System.out.println("move result = " + key);
                        buffer.clear();
                    }
                }
                // 第一位非0，非法输入，清空buffer
                else {
                    System.out.println("illegal input");
                    buffer.clear();
                }
            }
        } while (!eof);
        return startRd;
    }

    // 发送屏幕的尺寸信息
    private static void sendScreenInfo(Size size, ByteBuffer buffer, FileDescriptor fileDescriptor) throws IOException {
        int width = size.getWidth();
        int height = size.getHeight();
        // 宽度编码
        byte wHigh = (byte) (width >> 8);
        byte wLow = (byte) (width & 0xff);
        // 高度编码
        byte hHigh = (byte) (height >> 8);
        byte hLow = (byte) (height & 0xff);
        // 在ByteBuffer中放入数据，发送
        buffer.put(wHigh);
        buffer.put(wLow);
        buffer.put(hHigh);
        buffer.put(hLow);
        // 计量信息长度
        byte[] buffer_size = new byte[4];
        buffer_size[0] = (byte) (width >> 8);
        buffer_size[1] = (byte) (width & 0xff);
        buffer_size[2] = (byte) (height >> 8);
        buffer_size[3] = (byte) (height & 0xff);
        writeFully(fileDescriptor, buffer_size, 0, buffer_size.length);
        System.out.println("发送屏幕尺寸 size result");
        buffer.clear();
    }

    // 注入键盘输入事件
    private static boolean injectKeyEvent(ServiceManager serviceManager, int action, int keyCode) {
        KeyEvent keyEvent = EventFactory.keyEvent(action, keyCode, 0, 0);
        return serviceManager.getInputManager().injectInputEvent(keyEvent, InputManager
                .INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    // 注入点击事件
    private static boolean injectClick(ServiceManager serviceManager, int type, int x, int y) {
        MotionEvent event = EventFactory.createClickEvent(type, x, y);
        return serviceManager.getInputManager().injectInputEvent(event, InputManager
                .INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private static boolean injectMove(ServiceManager serviceManager, int x, int y) {
        MotionEvent event = EventFactory.creatMoveEvent(x, y);
        return serviceManager.getInputManager().injectInputEvent(event, InputManager
                .INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    // 注入滚动事件
    private static boolean injectScroll(ServiceManager serviceManager, int x, int y, int hScroll, int vScroll) {
        MotionEvent event = EventFactory.createScrollEvent(x, y, hScroll, vScroll);
        return serviceManager.getInputManager().injectInputEvent(event, InputManager
                .INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    // 点亮手机屏幕
    private static void clickPowerKey(ServiceManager serviceManager) {
        System.out.println("enter screen KEYCODE_POWER");
        // 获取按电源键的点击事件列表(手势命令为MotionEvent)
        KeyEvent[] keyEvents = EventFactory.clickEvent(KeyEvent.KEYCODE_POWER);
        boolean result = true;
        // 依次注入命令，模拟按动电源的事件
        for (KeyEvent keyEvent : keyEvents) {
            result = result & serviceManager.getInputManager().injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
        System.out.println("key KEYCODE_POWER result = " + result);
    }

    // 真正实现Socket写入操作
    public static void writeFully(FileDescriptor fd, ByteBuffer from) throws IOException {
        int remaining = from.remaining();
        while (remaining > 0) {
            try {
                // w为写入的数据量
                int w = Os.write(fd, from);
                // 若w<0，表明出错
                if (w < 0) {
                    throw new AssertionError("Os.write() returned a negative value (" + w + ")");
                }
                remaining -= w;
            } catch (ErrnoException e) {
                if (e.errno != OsConstants.EINTR) {
                    throw new IOException(e);
                }
            }
        }
    }

    // 根据偏移量offset和数据长度len获取实际发送数据
    public static void writeFully(FileDescriptor fd, byte[] buffer, int offset, int len) throws IOException {
        writeFully(fd, ByteBuffer.wrap(buffer, offset, len));
    }
}


