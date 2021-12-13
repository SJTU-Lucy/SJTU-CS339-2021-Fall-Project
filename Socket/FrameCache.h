#ifndef FRAMECACHE_H
#define FRAMECACHE_H

#include <SDL_stdinc.h>
#include <SDL_mutex.h>

extern "C"
{
#include "libavformat/avformat.h"
}

class FrameCache {
private:
    SDL_mutex* mutex;
public:
    AVFrame* decode_frame;
    AVFrame* render_frame;
    SDL_bool consume_render;

    SDL_bool init();

    SDL_bool product_frame();

    void frames_swap();
};

#endif //FRAMECACHE_H
