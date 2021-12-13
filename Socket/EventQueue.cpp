#include "EventQueue.h"

SDL_bool EventQueue::init() {
    head = 0;
    tail = 0;
    size = 0;
    return SDL_TRUE;
}

SDL_bool EventQueue::push_event(SDL_Event event) {
    SDL_bool res = SDL_TRUE;
    if (size >= QUEUE_SIZE) {
        return SDL_FALSE;
    }

    if (is_full()) {
        printf("is is_full\n");
        res = SDL_FALSE;
    }
    else {
        queue[head] = event;
        head = (head + 1) % QUEUE_SIZE;
        size++;
    }

    return res;
}

SDL_bool EventQueue::take_event(SDL_Event* event) {

    if (size == 0) {
        return SDL_FALSE;
    }
    *event = queue[tail];
    tail = (tail + 1) % QUEUE_SIZE;
    size--;

    if (size < 0 || size > QUEUE_SIZE) {
        printf("when size is larger than max?? size = %ld ,max = %d \n", size, QUEUE_SIZE);
        abort();
    }

    return SDL_TRUE;
}

void EventQueue::destroy() {
}

int EventQueue::is_empty() {
    return head == tail;
}

int EventQueue::is_full() {
    return (head + 1) % QUEUE_SIZE == tail;
}

