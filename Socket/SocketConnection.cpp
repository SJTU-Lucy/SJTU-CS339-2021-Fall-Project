#include "SocketConnection.h"

SocketConnection::SocketConnection() {
    // 初始化
    WSADATA wsaData;
    WSAStartup(MAKEWORD(2, 2), &wsaData);
    clientSocket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
}

bool SocketConnection::connect_server(string ip) {
    const char* address = ip.c_str();
    // 补充连接信息
    sockaddr.sin_family = AF_INET;
    sockaddr.sin_addr.S_un.S_addr = inet_addr(address);
    // 设置要连接的IP和端口
    sockaddr.sin_port = htons(8888);
    // 进行连接
    int ret = connect(clientSocket, (SOCKADDR*)&sockaddr, sizeof(SOCKADDR));;
    if (ret < 0) {
        cout << "socket connect error!!\\n" << endl;
        return false;
    }
    return true;
}

void SocketConnection::close_client() {
    WSACleanup();
}

int SocketConnection::send_to_(uint8_t* buf, int len) {
    if (!clientSocket) {
        return 0;
    }
    return send(clientSocket, (char*)buf, len, 0);
}

int SocketConnection::recv_from_(uint8_t* buf, int len) {
    if (!clientSocket) {
        return 0;
    }
    int reLen = recv(clientSocket, (char*)buf, 1024, 0);
    return reLen;
}
