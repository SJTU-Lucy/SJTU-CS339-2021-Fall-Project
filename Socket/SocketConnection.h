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
 * ����ķ���������
 */
class SocketConnection {
public:
    SOCKET clientSocket;
    sockaddr_in sockaddr;

    SocketConnection();

    // ����Socket
    bool connect_server(string ip);

    // �ر�Socket
    void close_client();

    // Socket ����
    int send_to_(uint8_t* buf, int len);

    // Socket ����
    int recv_from_(uint8_t* buf, int len);
};


#endif //SOCKETCONNECTION_H
