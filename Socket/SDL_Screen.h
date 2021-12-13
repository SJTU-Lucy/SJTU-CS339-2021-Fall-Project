#ifndef SDL_SCREEN_H
#define SDL_SCREEN_H

#include "SDL_render.h"
#include "SDL_video.h"
#include "SDL.h"
#include <algorithm>
#include <stdio.h>

#pragma comment(lib ,"sdl2.lib")
#pragma comment(lib ,"SDL2main.lib")
#define SDL_MAIN_HANDLED
#define EVENT_NEW_FRAME (SDL_USEREVENT + 1)

class SDL_Screen {
public:
    char* name;
    SDL_Window* sdl_window;
    SDL_Texture* sdl_texture;

    int screen_w;
    int screen_h;
    SDL_Renderer* sdl_renderer;

    SDL_Screen(char* name, int screen_w, int screen_h);

    ~SDL_Screen();

    SDL_bool init();

    void destroy();

    void uploadTexture(const Uint8* Yplane, int Ypitch,
        const Uint8* Uplane, int Upitch,
        const Uint8* Vplane, int Vpitch);

    void push_frame_event();
};

#endif //SDL_SCREEN_H
