package com.example.androidserver;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.TextView;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.IOException;

public class SocketActivity extends AppCompatActivity {
    TextView ip_info;               // 显示传入ip信息
    TextView socket_info;           // 显示socket连接信息

    String IP = "";

    Runnable runnable = new Runnable(){
        @Override
        public void run() {
            TCPClient();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.socket_activity);
        // 获取控件
        ip_info=(TextView)findViewById(R.id.recv_ip);
        socket_info=(TextView)findViewById(R.id.socketRet);
        // 捕获传入参数IP
        Intent intent=getIntent();
        IP=intent.getStringExtra("IP");
        ip_info.setText("Connecting to ip : " + IP);

        new Thread(runnable).start();
    }

    private String intToIp(int i) {
        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    // 获取本机IP地址
    private String getHostIP(){
        WifiManager wm=(WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo wi=wm.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAdd=wi.getIpAddress();
        // 把整型地址转换成“*.*.*.*”地址
        String ip=intToIp(ipAdd);
        String print = "ip:" + ip;
        return ip;
    }

    // 将手机端IP地址送到PC端服务器，建立ADB连接
    protected void TCPClient(){
        String localHost = getHostIP();
        try {
            // 创建客户端Socket，指定服务器的IP地址和端口
            Socket socket = new Socket(IP, 8888);
            // 获取输出流，向服务器发送数据
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            pw.write(localHost);
            pw.flush();
            // 关闭输出流
            socket.shutdownOutput();
            pw.close();
            os.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
