#ifndef SOCKETCONNECTION_H
#define SOCKETCONNECTION_H

#define _WINSOCK_DEPRECATED_NO_WARNINGS

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

/**
 * 这里的方法均阻塞
 */
class SocketConnection {
public:
    SOCKET clientSocket;
    sockaddr_in sockaddr;

    SocketConnection();

    // 连接Socket
    bool connect_server(string ip);

    // 关闭Socket
    void close_client();

    // Socket 发送
    int send_to_(uint8_t* buf, int len);

    // Socket 接受
    int recv_from_(uint8_t* buf, int len);
};


#endif //SOCKETCONNECTION_H
