package com.app2.proxy;

import com.app2.proxy.utils.Configuration;
import com.app2.proxy.utils.IPv4v6Utils;
import com.app2.proxy.utils.LogExt;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements ServiceConnection, OnClickListener {

    private static final String TAG = "fuyao-MainActivity";

    private ExchangeMsgService mExchangeMsgService = null;
    private Button mOkButton = null;
    private EditText mSetHost = null;
    private TextView mServerIp = null;

    private ScrollView mScrollView = null;
    private TextView mOutTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSetHost = (EditText) findViewById(R.id.host);
        mOkButton = (Button) findViewById(R.id.ok);
        mOkButton.setOnClickListener(this);
        mServerIp = (TextView) findViewById(R.id.serverIp);
        mServerIp.setText(IPv4v6Utils.getLocalIPAddress());
        mOutTextView = (TextView) findViewById(R.id.outPut);

        boolean ret = bindService(new Intent(getApplicationContext(), ExchangeMsgService.class), this, BIND_AUTO_CREATE);
        LogExt.d(TAG, "bindService ret = " + ret);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        mExchangeMsgService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mExchangeMsgService = ((ExchangeMsgService.ExchangeBinder) service).getService();
        LogExt.d(TAG, "connected server");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (null != mExchangeMsgService) {
            mExchangeMsgService = null;
        }
    }

    @Override
    public void onClick(View v) {
        String ip = mSetHost.getText().toString();
        if (TextUtils.isEmpty(ip)) {
            Toast.makeText(this, "ip cann't be empty!", Toast.LENGTH_LONG).show();
            return;
        } else if (null == mExchangeMsgService) {
            Toast.makeText(this, "not bind server!", Toast.LENGTH_LONG).show();
            return;
        }
        mExchangeMsgService.connectRemoteServer(ip, Configuration.MANAGER_SERVER_PORT);
    }
}
