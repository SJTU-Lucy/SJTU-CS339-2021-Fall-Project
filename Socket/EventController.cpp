#include "EventController.h"

void EventController::init() {
    queue = new EventQueue();
    queue->init();

    mutex = SDL_CreateMutex();
    if (!mutex) {
        printf("EventQueue SDL_CreateMutex error\n");
        //        return SDL_FALSE;
    }

    cond = SDL_CreateCond();
    if (!cond) {
        printf("EventQueue SDL_CreateCond error\n");
        //        return SDL_FALSE;
    }
}

int event_run(void* data) {
    EventController* controller = static_cast<EventController*>(data);
    controller->stop = SDL_FALSE;
    controller->event_handle();
    return 0;
}

void EventController::async_start() {
    event_tid = SDL_CreateThread(event_run, "event_controller", this);
}

void EventController::destroy() {
    stop_loop();
    queue->destroy();

    //    SDL_CondSignal(cond);
    SDL_DestroyCond(cond);
    SDL_DestroyMutex(mutex);

}

// ����¼� 0 0/1 Xh Xl Yh Yl
void EventController::handleButtonEvent(SDL_Screen* screen, SDL_MouseButtonEvent* event) {
    cout << "Button Event" << endl;

    int width = screen->screen_w;
    int height = screen->screen_h;
    int x = event->x;
    int y = event->y;

    bool outside_device_screen = x < 0 || x >= width || y < 0 || y >= height;
    if (outside_device_screen) { return; }

    char buf[6];
    memset(buf, 0, sizeof(buf));
    printf(" event x =%d\n", event->x);
    printf(" event y =%d\n", event->y);
    buf[0] = 0;
    // down: type = 1
    // up: type = 0
    if (event->type == SDL_MOUSEBUTTONDOWN) { buf[1] = 1; }
    else { buf[1] = 0; }
    buf[2] = event->x >> 8;
    buf[3] = event->x & 0xff;
    buf[4] = event->y >> 8;
    buf[5] = event->y & 0xff;

    int result = connection->send_to_(reinterpret_cast<uint8_t*>(buf), 6);;
    printf("send result = %d\n", result);
}

// �϶��¼� 0 4 Xh Xl Yh Yl
void EventController::handleMoveEvent(SDL_Screen* screen, SDL_MouseMotionEvent* event) {
    cout << "Move Event" << endl;

    int width = screen->screen_w;
    int height = screen->screen_h;
    int x = event->x;
    int y = event->y;

    bool outside_device_screen = x < 0 || x >= width || y < 0 || y >= height;
    if (outside_device_screen) { return; }

    char buf[6];
    memset(buf, 0, sizeof(buf));
    printf(" event x =%d\n", event->x);
    printf(" event y =%d\n", event->y);
    buf[0] = 0;
    buf[1] = 4;
    buf[2] = event->x >> 8;
    buf[3] = event->x & 0xff;
    buf[4] = event->y >> 8;
    buf[5] = event->y & 0xff;

    int result = connection->send_to_(reinterpret_cast<uint8_t*>(buf), 6);;
    printf(" send result = %d\n", result);
}

// �����¼� 0 2 Xh Xl Yh Yl Hs[3] Hs[2] Hs[1] Hs[0] Vs[3] Vs[2] Vs[1] Vs[0] 
void EventController::handleScrollEvent(SDL_Screen* sc, SDL_MouseWheelEvent* event) {
    cout << "Scroll Event" << endl;

    int width = sc->screen_w;
    int height = sc->screen_h;
    int x_c;
    int y_c;
    int* x = &x_c;
    int* y = &y_c;
    SDL_GetMouseState(x, y);

    // �������λ��
    SDL_Rect viewport;
    float scale_x, scale_y;
    SDL_RenderGetViewport(sc->sdl_renderer, &viewport);
    SDL_RenderGetScale(sc->sdl_renderer, &scale_x, &scale_y);
    *x = (int)(*x / scale_x) - viewport.x;
    *y = (int)(*y / scale_y) - viewport.y;

    bool outside_device_screen = x_c < 0 || x_c >= width || y_c < 0 || y_c >= height;
    if (outside_device_screen) { return; }

    // ע���������ʱ��ˮƽ����Ӧ������ֱ����Ӧ���ϣ������෴
    int mul = event->direction == SDL_MOUSEWHEEL_NORMAL ? 1 : -1;
    int hs = -mul * event->x;
    int vs = mul * event->y;

    char buf[14];
    memset(buf, 0, sizeof(buf));
    printf(" x_c =%d\n", x_c);
    printf(" y_c =%d\n", y_c);
    printf(" hs =%d\n", hs);
    printf(" vs =%d\n", vs);
    buf[0] = 0;
    buf[1] = 2;
    buf[2] = x_c >> 8;
    buf[3] = x_c & 0xff;
    buf[4] = y_c >> 8;
    buf[5] = y_c & 0xff;
    buf[6] = hs >> 24;
    buf[7] = hs >> 16;
    buf[8] = hs >> 8;
    buf[9] = hs;
    buf[10] = vs >> 24;
    buf[11] = vs >> 16;
    buf[12] = vs >> 8;
    buf[13] = vs;

    int result = connection->send_to_(reinterpret_cast<uint8_t*>(buf), 14);
    printf("send result = %d\n", result);
}

int trans(int keyboard) {
    // number
    if (keyboard >= 97 && keyboard <= 122) return keyboard - 68;
    // character
    if (keyboard >= 48 && keyboard <= 57) return keyboard - 41;
    // enter
    if (keyboard == 13) return 66;
    // space
    if (keyboard == 32) return 62;
    // back
    if (keyboard == 8) return 67;
    // ��
    if (keyboard == 44) return 55;
    // ��
    if (keyboard == 46) return 56;
    return -1;
}

// ���������¼�: control+ H = home  control+b = back
// 0 3(type) 0(up)/1(down) 3(home)/4(back)/key_code 
void EventController::handleSDLKeyEvent(SDL_Screen* sc, SDL_KeyboardEvent* event) {
    cout << "Key Event" << endl;

    int ctrl = event->keysym.mod & (KMOD_LCTRL | KMOD_RCTRL);
    printf(" ctrl = %d,", ctrl);

    SDL_Keycode keycode = event->keysym.sym;
    printf(" keycode = %d, action type = %d\n", keycode, event->type);
    printf(" b = %d, action type = %d\n", SDLK_b, event->type);
    // �������¼�
    if (event->type == SDL_KEYDOWN) {
        // ����ctrl+h��ctrl+b�����
        if (ctrl != 0) {
            if (keycode == SDLK_h) {
                char buf[4];
                memset(buf, 0, sizeof(buf));
                buf[0] = 0;
                buf[1] = 3;
                buf[2] = 1;
                buf[3] = 3;
                int result = connection->send_to_(reinterpret_cast<uint8_t*>(buf), 4);
                printf("send result = %d\n", result);
            }
            else if (keycode == SDLK_b) {
                char buf[4];
                memset(buf, 0, sizeof(buf));
                buf[0] = 0;
                buf[1] = 3;
                buf[2] = 1;
                buf[3] = 4;
                int result = connection->send_to_(reinterpret_cast<uint8_t*>(buf), 4);
                printf("send result = %d\n", result);
            }
        }
        // �����������
        else {
            int key = trans(keycode);
            if (key == -1) return;
            else {
                char buf[4];
                memset(buf, 0, sizeof(buf));
                buf[0] = 0;
                buf[1] = 3;
                buf[2] = 1;
                buf[3] = key;
                int result = connection->send_to_(reinterpret_cast<uint8_t*>(buf), 4);
                printf("send result = %d\n", result);
            }
        }
    }
    // ����̧���¼�
    if (event->type == SDL_KEYUP && keycode != 0) {
        // ����ctrl+h��ctrl+b�����
        if (ctrl != 0) {
            if (keycode == SDLK_h) {
                char buf[4];
                memset(buf, 0, sizeof(buf));
                buf[0] = 0;
                buf[1] = 3;
                buf[2] = 0;
                buf[3] = 3;
                int result = connection->send_to_(reinterpret_cast<uint8_t*>(buf), 4);
                printf("send result = %d\n", result);
            }
            else if (keycode == SDLK_b) {
                char buf[4];
                memset(buf, 0, sizeof(buf));
                buf[0] = 0;
                buf[1] = 3;
                buf[2] = 0;
                buf[3] = 4;
                int result = connection->send_to_(reinterpret_cast<uint8_t*>(buf), 4);
                printf("send result = %d\n", result);
            }
        }
        // �����������
        else {
            int key = trans(keycode);
            if (key == -1) return;
            else {
                char buf[4];
                memset(buf, 0, sizeof(buf));
                buf[0] = 0;
                buf[1] = 3;
                buf[2] = 0;
                buf[3] = key;
                int result = connection->send_to_(reinterpret_cast<uint8_t*>(buf), 4);
                printf("send result = %d\n", result);
            }
        }
    }

}

void EventController::event_handle() {
    printf("event_handle");
    // ���ڼ�¼button״̬�������£�����move����
    bool buttonPush = false;
    for (;;) {
        if (stop) {
            break;
        }
        SDL_Event event;
        SDL_LockMutex(mutex);

        if (queue->is_empty()) {
            SDL_CondWait(cond, mutex);
        }
        SDL_bool sdl_bool = queue->take_event(&event);
        SDL_UnlockMutex(mutex);
        // ������갴���¼�������״̬
        if (event.type == SDL_MOUSEBUTTONDOWN) {
            handleButtonEvent(screen, &event.button);
            buttonPush = true;
        }
        // �������̧���¼�������״̬
        else if (event.type == SDL_MOUSEBUTTONUP) {
            handleButtonEvent(screen, &event.button);
            buttonPush = false;
        }
        // �����������¼�
        else if (event.type == SDL_KEYDOWN) {
            handleSDLKeyEvent(screen, &event.key);
        }
        // ������̧��ʱ��
        else if (event.type == SDL_KEYUP) {
            handleSDLKeyEvent(screen, &event.key);
        }
        // �������¼�
        else if (event.type == SDL_MOUSEWHEEL) {
            handleScrollEvent(screen, &event.wheel);
        }
        // �ڰ�����������£������϶��¼�
        else if (event.type == SDL_MOUSEMOTION && buttonPush) {
            handleMoveEvent(screen, &event.motion);
        }
    }
}

void EventController::stop_loop() {
    stop = SDL_TRUE;
    SDL_DetachThread(event_tid);
}

EventController::EventController(SDL_Screen* screen, SocketConnection* connection) 
    : screen(screen), connection(connection) {}

void EventController::push_event(SDL_Event event) {
    SDL_LockMutex(mutex);
    // �жϵ�ǰ�Ƿ�Ϊ��
    int empty = queue->is_empty();
    queue->push_event(event);
    // ���Ϊ�յĻ�������ȡ
    if (empty) {
        SDL_CondSignal(cond);
    }
    SDL_UnlockMutex(mutex);
}

