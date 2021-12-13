#include <exception>
#include "FFmpegDecoder.h"

FFmpegDecoder::FFmpegDecoder(SocketConnection* connection, FrameCache* cache, SDL_Screen* screen) : connection(
    connection), cache(cache), screen(screen) {}

// ��socket�ж�ȡ����
int read_socket_buffer(void* opaque, uint8_t* buf, int buf_size) {
    FFmpegDecoder* decoder = static_cast<FFmpegDecoder*>(opaque);
    if (decoder->request_stop == SDL_TRUE) {
        return -1;
    }
    int count = decoder->connection->recv_from_(buf, buf_size);
    if (count == 0) {
        return -1;
    }
    return count;
}

SDL_bool FFmpegDecoder::init() {
    avformat_network_init();
    format_ctx = avformat_alloc_context();
    unsigned char* buffer = static_cast<unsigned char*>(av_malloc(BUF_SIZE));
    avio_ctx = avio_alloc_context(buffer, BUF_SIZE, 0, this, read_socket_buffer, NULL, NULL);
    format_ctx->pb = avio_ctx;

    int ret = avformat_open_input(&format_ctx, NULL, NULL, NULL);
    if (ret < 0) {
        char errStr[256] = { 0 };
        av_strerror(ret, errStr, sizeof(errStr));
        printf("avformat_open_input error:%s\n", errStr);
        return SDL_FALSE;
    }

    AVCodec* codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (!codec) {
        printf("Did not find a video codec \n");
        return SDL_FALSE;
    }
    codec_ctx = avcodec_alloc_context3(codec);
    if (!codec_ctx) {
        printf("Did not alloc AVCodecContext \n");
        return SDL_FALSE;
    }
    codec_ctx->width = static_cast<int>(screen->screen_w);
    codec_ctx->height = static_cast<int>(screen->screen_h);

    ret = avcodec_open2(codec_ctx, codec, NULL);
    if (ret < 0) {
        char errStr[256] = { 0 };
        av_strerror(ret, errStr, sizeof(errStr));
        printf("avcodec_open2 error:%s\n", errStr);
        return SDL_FALSE;
    }
    printf("�ɹ��򿪱�����\n");

    packet = av_packet_alloc();
    request_stop = SDL_FALSE;
    return SDL_TRUE;
}


int decode_run(void* data) {
    FFmpegDecoder* decoder = static_cast<FFmpegDecoder*>(data);
    SDL_bool ret = decoder->init();
    if (ret == SDL_FALSE) {}
    else {
        try {
            decoder->_decode_loop();
        }
        catch (const std::exception & e) {
            printf("error = %d \n", e.what());
        }
    }
    return 0;
}


SDL_bool FFmpegDecoder::async_start() {
    decoder_tid = SDL_CreateThread(decode_run, "decoder", this);
    if (!decoder_tid) {
        perror("Could not start decoder thread");
        return SDL_FALSE;
    }
    return SDL_TRUE;
}

void FFmpegDecoder::_decode_loop() {
    int ret;
    while (av_read_frame(format_ctx, packet) >= 0) {
        if (request_stop == SDL_TRUE) {
            break;
        }

        while (true) {
            ret = avcodec_send_packet(codec_ctx, packet);
            if (ret == 0) {
                break;
            }
            else if (ret == AVERROR(EAGAIN)) {
                break;
            }
            else {
                char errStr[256] = { 0 };
                av_strerror(ret, errStr, sizeof(errStr));
                printf("avcodec_send_packet error:%s\n", errStr);
                av_packet_unref(packet);
                goto quit;
            }
        }
        // ����������ݣ��ŵ�decode_frame��
        ret = avcodec_receive_frame(codec_ctx, cache->decode_frame);
        if (ret != 0 && ret != AVERROR(EAGAIN)) {
            char errStr[256] = { 0 };
            av_strerror(ret, errStr, sizeof(errStr));
            printf("avcodec_receive_frame error:%s\n", errStr);
            goto quit;
        }
        SDL_bool consumer_preview = cache->product_frame();
        // �����һ����û��ʾ������Ҫ�����¼�������һ����ʾ���ٷ���
        if (consumer_preview != SDL_FALSE) {
            screen->push_frame_event();
        }

        // ���꣬�˳�ѭ��
        if (avio_ctx->eof_reached) {
            break;
        }
        av_packet_unref(packet);
    }
quit:
    printf("break out_of_decode_loop");
}

void FFmpegDecoder::destroy() {
    request_stop = SDL_FALSE;
    avformat_close_input(&format_ctx);
}

void FFmpegDecoder::stop() {
    request_stop = SDL_FALSE;
    // �ȴ�decoder�߳�ִ����.
    SDL_DetachThread(decoder_tid);
}
