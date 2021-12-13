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

string transform(char recv[]) {
	string ret = "";
	for (int i = 0; recv[i] != '\0'; i++) ret.append(1, recv[i]);
	return ret;
}

void adb_connect() {
	//	1 初始化
	WSADATA wsadata;
	WSAStartup(MAKEWORD(2, 2), &wsadata);	//make word,你把鼠标移到WSAStartup看看参数列表,是不是就是一个word啊

	//	2 创建服务器的套接字
	SOCKET serviceSocket;
	// socket(协议族,socket数据传输方式,某个协议)	我们默认为0,其实就是一个宏
	serviceSocket = socket(AF_INET, SOCK_STREAM, 0);
	if (SOCKET_ERROR == serviceSocket) {
		cout << "套接字创建失败!" << endl;
	}
	else {
		cout << "套接字创建成功!" << endl;
	}

	//一个绑定地址:有IP地址,有端口号,有协议族
	sockaddr_in socketAddr;
	socketAddr.sin_family = AF_INET;
	socketAddr.sin_addr.S_un.S_addr = htonl(INADDR_ANY);
	socketAddr.sin_port = htons(8888);
	//绑定注意的一点就是记得强制类型转换
	int bRes = bind(serviceSocket, (SOCKADDR*)&socketAddr, sizeof(SOCKADDR));
	if (SOCKET_ERROR == bRes) {
		cout << "绑定失败!" << endl;
	}
	else {
		cout << "绑定成功!" << endl;
	}

	// 监听的第二个参数就是:能存放多少个客户端请求
	int lLen = listen(serviceSocket, 5);
	if (SOCKET_ERROR == lLen) {
		cout << "监听失败!" << endl;
	}
	else {
		cout << "监听成功!" << endl;
	}

	sockaddr_in revClientAddr;
	//初始化一个接受的客户端socket
	SOCKET recvClientSocket = INVALID_SOCKET;
	int _revSize = sizeof(sockaddr_in);
	recvClientSocket = accept(serviceSocket, (SOCKADDR*)&revClientAddr, &_revSize);
	if (INVALID_SOCKET == recvClientSocket) {
		cout << "服务端接受请求失败!" << endl;
	}
	else {
		cout << "服务端接受请求成功!" << endl;
	}

	char recvBuf[1024] = {};
	int reLen = recv(recvClientSocket, recvBuf, 1024, 0);
	cout << "服务器接受到数据:    " << recvBuf << endl << endl;

	closesocket(recvClientSocket);
	closesocket(serviceSocket);

	WSACleanup();
	cout << "服务器停止" << endl;

	// 执行adb connect命令
	string adb = "adb connect ";
	string ip = transform(recvBuf);
	adb.insert(adb.size(), ip);
	string port = ":8888";
	adb.insert(adb.size(), port);
	const char* target = adb.c_str();
	system(target);
}

int main()
{
	adb_connect();
	getchar();
	return 0;
}