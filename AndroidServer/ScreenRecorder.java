package com.example.server;

import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.IBinder;
import android.view.Surface;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenRecorder {
    private static final int DEFAULT_FRAME_RATE = 60; // 帧率
    private static final int DEFAULT_I_FRAME_INTERVAL = 1; // I帧出现的时间间隔
    private static final int DEFAULT_BIT_RATE = 10 * 2400 * 1080; // 码率
    private static final int DEFAULT_TIME_OUT = 2 * 1000; // TimeOut时间

    private static final int REPEAT_FRAME_DELAY = 1;
    private static final int MICROSECONDS_IN_ONE_SECOND = 1_000_000;
    private static final int NO_PTS = -1;

    private boolean sendFrameMeta = false;

    private final ByteBuffer headerBuffer = ByteBuffer.allocate(12);
    private long ptsOrigin;

    private volatile boolean stop;
    private MediaCodec encoder;

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    // 进行录制的循环，其中录制得到的数据写入fd中进行发送
    public void record(int width, int height, FileDescriptor fd) {
        //对MediaCodec进行配置
        boolean alive;
        try {
            do {
                // 设置媒体流格式：比特流，帧率等
                MediaFormat mediaFormat = createMediaFormat(DEFAULT_BIT_RATE, DEFAULT_FRAME_RATE, DEFAULT_I_FRAME_INTERVAL);
                mediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
                mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
                // 创建AVC格式的编码器
                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                // 设置编码器
                encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                Surface inputSurface = encoder.createInputSurface();
                IBinder surfaceClient = setDisplaySurface(width, height, inputSurface);
                encoder.start();
                try {
                    alive = encode(encoder, fd);
                    alive = alive && !stop;
                    System.out.println("alive =" + alive + ", stop=" + stop);
                } finally {
                    System.out.println("encoder.stop");
                    System.out.println("destroyDisplaySurface");
                    destroyDisplaySurface(surfaceClient);
                    System.out.println("encoder release");
                    encoder.release();
                    System.out.println("inputSurface release");
                    inputSurface.release();
                    System.out.println("end");
                }
            } while (alive);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("end record");
    }

    // 创建录制的Surface, left与right为到左边框距离,top与bottom为到上边框距离
    private IBinder setDisplaySurface(int width, int height, Surface inputSurface) {
        Rect deviceRect = new Rect(0, 0, width * 2, height * 2);
        Rect displayRect = new Rect(0, 0, width, height);
        IBinder surfaceClient = SurfaceControl.createDisplay("recorder", false);
        // 设置和配置截屏的Surface
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(surfaceClient, inputSurface);
            SurfaceControl.setDisplayProjection(surfaceClient, 0, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(surfaceClient, 0);
        } finally {
            SurfaceControl.closeTransaction();
        }
        return surfaceClient;
    }

    private void destroyDisplaySurface(IBinder surfaceClient) {
        SurfaceControl.destroyDisplay(surfaceClient);
    }

    // 创建MediaFormat
    private MediaFormat createMediaFormat(int bitRate, int frameRate, int iFrameInterval) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
        mediaFormat.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, MICROSECONDS_IN_ONE_SECOND * REPEAT_FRAME_DELAY / frameRate);
        return mediaFormat;
    }

    // 进行encode
    private boolean encode(MediaCodec codec, FileDescriptor fd) throws IOException {
        System.out.println("encode");
        boolean eof = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!eof) {
            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, DEFAULT_TIME_OUT);
            eof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            try {
                if (stop) {
                    break;
                }
                // 将得到的数据，都发送给fd
                if (outputBufferId >= 0) {
                    ByteBuffer codecBuffer = codec.getOutputBuffer(outputBufferId);
                    if (sendFrameMeta) {
                        writeFrameMeta(fd, bufferInfo, codecBuffer.remaining());
                    }
                    IO.writeFully(fd, codecBuffer);
                }
            } finally {
                if (outputBufferId >= 0) {
                    codec.releaseOutputBuffer(outputBufferId, false);
                }
            }
        }
        return !eof;
    }

    private void writeFrameMeta(FileDescriptor fd, MediaCodec.BufferInfo bufferInfo, int packetSize) throws IOException {
        headerBuffer.clear();
        long pts;
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            pts = NO_PTS; // non-media data packet
        } else {
            if (ptsOrigin == 0) {
                ptsOrigin = bufferInfo.presentationTimeUs;
            }
            pts = bufferInfo.presentationTimeUs - ptsOrigin;
        }
        headerBuffer.putLong(pts);
        headerBuffer.putInt(packetSize);
        headerBuffer.flip();
        IO.writeFully(fd, headerBuffer);
    }
}
