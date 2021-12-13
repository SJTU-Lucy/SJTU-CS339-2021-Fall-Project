#ifndef EVENTQUEUE_H
#define EVENTQUEUE_H

#include "SDL_events.h"
#include "SDL_mutex.h"
#include <stdio.h>
#include <stdlib.h>

#define QUEUE_SIZE 64


class EventQueue {
private:
    SDL_Event queue[QUEUE_SIZE];
    int head;
    int tail;
    long size = 0;

public:
    SDL_bool init();

    void destroy();

    SDL_bool push_event(SDL_Event event);

    int is_empty();

    int is_full();

    SDL_bool take_event(SDL_Event* event);
};


#endif //EVENTCACHE_H
