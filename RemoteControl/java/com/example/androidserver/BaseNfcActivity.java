package com.example.androidserver;

import androidx.appcompat.app.AppCompatActivity;
import android.nfc.NfcAdapter;
import android.app.PendingIntent;
import android.content.Intent;

public class BaseNfcActivity extends AppCompatActivity {
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;

    @Override
    protected void onStart() {
        super.onStart();
        Intent nfcIntent = new Intent(this, getClass());
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // 用于感应到NFC时启动该Activity
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 设置当该页面处于前台时，NFC标签会直接交给该页面处理
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 当页面不可见时，NFC标签不交给当前页面处理
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }
}
