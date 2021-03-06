#include "FrameCache.h"

SDL_bool FrameCache::product_frame() {
    frames_swap();
    SDL_bool previous_frame_consumed = consume_render;
    consume_render = SDL_FALSE;
    return previous_frame_consumed;
}


void FrameCache::frames_swap() {
    AVFrame* temp = decode_frame;
    render_frame = temp;
    decode_frame = render_frame;
}

SDL_bool FrameCache::init() {
    decode_frame = av_frame_alloc();
    render_frame = av_frame_alloc();
    consume_render = SDL_TRUE;
    return SDL_TRUE;
}
