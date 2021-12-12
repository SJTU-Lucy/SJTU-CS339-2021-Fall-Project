package com.example.androidserver;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.nfc.NfcAdapter;
import android.content.Intent;
import android.nfc.tech.Ndef;
import android.nfc.Tag;
import android.os.Parcelable;
import android.widget.Toast;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadTagActivity extends BaseNfcActivity {
    public TextView mContent;          // 显示TAG
    public TextView mARP;              // 显示ARP信息
    public TextView mIP;               // 显示扫描IP信息
    public Button mReturnBut;          // 进入下一activity
    public EditText mInput;            // 手动输入ip地址的框

    private String mTagText;            // for mContent
    private String ScanIP = "";         // for mARP
    private String InputIP = "";        // for mIP
    private String MAC = "";            // 记录扫描获得MAC地址

    //消息处理者,创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mIP.setText("Cost Time = " + String.valueOf(msg.what) + "ms");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置对应xml界面
        setContentView(R.layout.nfc_activity);
        // 获取控件
        mContent = findViewById(R.id.content);
        String message = "Please Scan NFC TAG!";
        mContent.setText(message);
        mARP = findViewById(R.id.arp_msg);
        mIP = findViewById(R.id.ip_ret);
        mReturnBut = findViewById(R.id.finish);
        mInput = findViewById(R.id.input_ip);
        // 设置点击事件
        mReturnBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 读取输入框中的内容
                InputIP = mInput.getText().toString();
                // 切换到SocketActivity中
                Intent mIntent = new Intent(ReadTagActivity.this,SocketActivity.class);

                // 若没有扫描获得结果，则传入输入的
                if(ScanIP.equals("")) {
                    // 首先判断输入的合法性
                    boolean flag = true;
                    int PointIndex = -1;
                    int countPoint = 0;
                    for(int i = 0; i < InputIP.length(); i++) {
                        char tmp = InputIP.charAt(i);
                        if(tmp == '.') {
                            countPoint++;
                            // 出现两个相邻的 or 相隔超过3个字符：非法
                            if(i - PointIndex < 2 || i - PointIndex > 4) {
                                flag = false;
                                break;
                            }
                            // 更新最后一次出现.的位置
                            PointIndex = i;
                        }
                        // 非.也非数字格式：非法
                        else if(tmp < '0' || tmp > '9') {
                            flag = false;
                            break;
                        }
                    }
                    if(InputIP.length() - PointIndex < 2 || InputIP.length() - PointIndex > 4) {
                        flag = false;
                    }
                    // 如果上面出现错误 or .的数量不为3个
                    if(countPoint != 3 || !flag) {
                        String tmp = "IP格式不符合要求，请重新输入！";
                        toastMessage(tmp);
                        return;
                    }
                    // 初步判断符合标准，传入InputIP
                    mIntent.putExtra("IP", InputIP);
                }
                // 如果扫描到了结果，无论输入与否都传入扫描的结果
                else {
                    mIntent.putExtra("IP", ScanIP);
                }
                startActivity(mIntent);
            }
        });
    }

    // 扫描到NFC时，自动执行
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 1. 获取Tag对象
        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        // 2. 获取Ndef的实例
        Ndef ndef = Ndef.get(detectedTag);
        mTagText = "type:" + ndef.getType();
        mTagText += "\nmaxsize:" + ndef.getMaxSize() + "bytes";
        // 3. 读NFC标签，获得MAC信息
        readNfcTag(intent);
        mContent.setText(mTagText);
        // 4. 局域网广播
        //sendDataTwoPart();
        sendDataOnePart();
        //sendDataPing();
        // 5. 读取ARP信息
        readArpLoop();
    }

    // 读取NFC标签，获得MAC值
    private void readNfcTag(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msgs[] = null;
            int contentSize = 0;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                    contentSize += msgs[i].toByteArray().length;
                }
            }
            try {
                if (msgs != null) {
                    NdefRecord record = msgs[0].getRecords()[0];
                    String textRecord = parseTextRecord(record);
                    mTagText += "\ncontent:" + textRecord;
                    mTagText += "\ncontentSize:" + contentSize + " bytes";
                    MAC = textRecord;
                }
            } catch (Exception e) {
            }
        }
    }

    public static String parseTextRecord(NdefRecord ndefRecord) {
        //判断TNF
        if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
            return null;
        }
        //判断可变的长度的类型
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return null;
        }
        try {
            // 获得字节数组，然后进行分析
            byte[] payload = ndefRecord.getPayload();
            // 下面开始NDEF文本数据第一个字节，状态字节
            // 判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            // 其他位都是0，所以进行"位与"运算后就会保留最高位
            String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
            // 3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            int languageCodeLength = payload[0] & 0x3f;
            // 下面开始NDEF文本数据第二个字节，语言编码
            // 获得语言编码
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // 下面开始NDEF文本数据后面的字节，解析出文本
            String textRecord = new String(payload, languageCodeLength + 1,
                    payload.length - languageCodeLength - 1, textEncoding);
            return textRecord;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    // 获取本地IP地址
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

    private String intToIp(int i) {
        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    // 进行局域网广播(两段式)
    private void sendDataTwoPart() {
        // 获取本机所在的局域网地址
        String hostIP = getHostIP();
        int lastIndexOf = hostIP.lastIndexOf(".");
        final String substring = hostIP.substring(0, lastIndexOf + 1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                long startTime = System.currentTimeMillis();
                DatagramPacket dp = new DatagramPacket(new byte[0], 0, 0);
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket();
                    int position = 2;
                    while (position < 255) {
                        dp.setAddress(InetAddress.getByName(substring + String.valueOf(position)));
                        socket.send(dp);
                        position++;
                        // 分两段发送，一次性发的话，达到236左右，会耗时3秒左右再往下发
                        if (position == 125) {
                            socket.close();
                            socket = new DatagramSocket();
                        }
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                long endTime = System.currentTimeMillis();
                long endurance = endTime - startTime;
                msg.what = (int)endurance;
                handler.sendMessage(msg);
            }
        }).start();
    }

    // 进行局域网广播(一段式)
    private void sendDataOnePart() {
        // 获取本机所在的局域网地址
        String hostIP = getHostIP();
        int lastIndexOf = hostIP.lastIndexOf(".");
        final String substring = hostIP.substring(0, lastIndexOf + 1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                long startTime = System.currentTimeMillis();
                DatagramPacket dp = new DatagramPacket(new byte[0], 0, 0);
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket();
                    int position = 2;
                    while (position < 255) {
                        dp.setAddress(InetAddress.getByName(substring + String.valueOf(position)));
                        socket.send(dp);
                        position++;
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                long endTime = System.currentTimeMillis();
                long endurance = endTime - startTime;
                msg.what = (int)endurance;
                handler.sendMessage(msg);
            }
        }).start();
    }

    // 进行局域网广播(ping)
    private void sendDataPing() {
        // 获取本机所在的局域网地址
        String hostIP = getHostIP();
        int lastIndexOf = hostIP.lastIndexOf(".");
        final String substring = hostIP.substring(0, lastIndexOf + 1);
        final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
        final int[] last = new int[1];
        Message msg = new Message();
        long startTime = System.currentTimeMillis();
        for(int i = 0; i <= 255; i++) {
            last[0] = i;
            Runnable runnable = new Runnable() {
                private int a = last[0];
                @Override
                public void run() {
                    execPingIpProcess(substring + a);
                }
            };
            fixedThreadPool.execute(runnable);
        }
        fixedThreadPool.shutdown();
        while (true) {
            if (fixedThreadPool.isTerminated()) {
                long endTime = System.currentTimeMillis();
                long endurance = endTime - startTime;
                msg.what = (int) endurance;
                handler.sendMessage(msg);
                break;
            }
        }
    }

    private void execPingIpProcess(final String ipString) {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 1 " + ipString);
            int status = p.waitFor();
        } catch (Exception e) {
        }
    }


    // 读取ARP信息
    private void readArpLoop() {
        readArp();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                readArpLoop();
            }
        }, 5000);
    }

    private void readArp() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line = "";
            String ip = "";
            String flag = "";
            String mac = "";
            mARP.setText("");
            if (br.readLine() == null) {
                Log.e("scanner", "readArp: null");
            }
            while ((line = br.readLine()) != null) {
                line = line.trim();
                Log.e("Scanner ", "receive " + line);
                if (line.length() < 63) continue;
                if (line.toUpperCase(Locale.US).contains("IP")) continue;
                // 提取有效信息
                ip = line.substring(0, 17).trim();
                flag = line.substring(29, 32).trim();
                mac = line.substring(41, 63).trim();
                if (mac.contains("00:00:00:00:00:00")) continue;
                Log.e("scanner", "readArp: mac= " + mac + " ; ip= " + ip + " ;flag= " + flag);
                mARP.append("\nip:" + ip + "\tmac:" + mac);

                // 如果MAC匹配，更新ScanIP内容
                if(mac.equals(MAC)) {
                    ScanIP = ip;
                    mIP.setText(ScanIP);
                }
            }
            br.close();
        } catch (Exception ignored) {
        }
    }

    // 弹窗提示
    public void toastMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}