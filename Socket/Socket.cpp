#define _WINSOCK_DEPRECATED_NO_WARNINGS
#include <iostream>
#include "SocketConnection.h"
#include "SDL_Screen.h"
#include "FrameCache.h"
#include "FFmpegDecoder.h" 
#include "EventQueue.h"
#include "EventController.h"
#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <string>
#include <cstring>
#include <WS2tcpip.h>
#include <WinSock2.h>						
#include <Windows.h>
#pragma comment(lib, "ws2_32.lib") 			
using namespace std;

int main() {
	// 获取目标IP
	string IP = "127.0.0.1";
	cout << "IP = " << IP << endl;

	// 先进行Socket连接
	SocketConnection* socketConnection = new SocketConnection();
	if (!socketConnection->connect_server(IP)) {
		return -1;
	}
	printf("连接成功\n");

	int scale = 2;
	string name = "as_remote";
	char* c;
	const int len = name.length();
	c = new char[len + 1];
	strcpy(c, name.c_str());

	SDL_Screen* screen;
	FrameCache* cache;
	FFmpegDecoder* decoder;
	EventController* controller;
	// 获取屏幕信息
	uint8_t buf[4];
	socketConnection->recv_from_(buf, 4);
	int width = (buf[0] << 8) | buf[1];
	int height = (buf[2] << 8) | buf[3];
	printf("width = %d , height = %d \n", width, height);

	// 视频解码
	screen = new SDL_Screen(c, static_cast<int>(width / scale), static_cast<int>(height / scale));
	screen->init();
	cache = new FrameCache();
	cache->init();
	decoder = new FFmpegDecoder(socketConnection, cache, screen);
	decoder->async_start();
	
	// 事件监听
	controller = new EventController(screen, socketConnection);
	controller->init();
	controller->async_start();
	SDL_Event event;
	// 开启Event Loop
	for (;;) {
		SDL_WaitEvent(&event);
		if (event.type == EVENT_NEW_FRAME) {
			AVFrame* render = cache->render_frame;
			screen->uploadTexture(
				render->data[0], render->linesize[0],
				render->data[1], render->linesize[1],
				render->data[2], render->linesize[2]
			);
			cache->consume_render = SDL_TRUE;
		}
		else if (event.type == SDL_QUIT) {
			printf("rev event type=SDL_QUIT\n");
			break;
		}
		else {
			controller->push_event(event);
		}
	}

	// 结束进程
	controller->destroy();
	decoder->stop();
	decoder->destroy();
	screen->destroy();
	delete screen;
	delete controller;
	delete cache;
	delete decoder;
	// close connection
	socketConnection->close_client();
	delete socketConnection;

	return 0;
}