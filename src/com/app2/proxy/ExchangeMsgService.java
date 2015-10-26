package com.app2.proxy;

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.app2.proxy.net.Data;
import com.app2.proxy.net.Network;
import com.app2.proxy.net.Network.ChatMessage;
import com.app2.proxy.net.Network.RegisterName;
import com.app2.proxy.net.Network.UpdateNames;
import com.app2.proxy.utils.Configuration;
import com.app2.proxy.utils.IPv4v6Utils;
import com.app2.proxy.utils.LogExt;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ExchangeMsgService extends Service {
    private static final String TAG = "fuyaoExchangeMsgService";

    private String mLocalIp;
    private String mUUID;

    // for connect remote server begin
    public static final int MSG_CONNECT_REMOTE_SERVER = 1;

    private RemoteClientHandler mRemoteClientHandler = null;
    private Looper mRemoteClientLooper = null;

    private Client mRemoteClient = null;
    private String mRemoteServerIp = null;

    public void setRemoteServerIp(String ip) {
        mRemoteServerIp = ip;
    }

    private WriteThread2 mRemoteWriteThread = null;
    private BlockingQueue<Data> mRemoteClientBufferExs = new LinkedBlockingQueue<Data>();
    // for connect remote server end

    // for connect local adb client begin
    private static final int MSG_CONNECT_LOCAL_SERVER = 3;

    private LocalClientHandler mAdbLocalClientHandler = null;
    private Looper mAdbLocalClientLooper = null;

    private Socket mAdbClientSocket = null;

    private BufferedOutputStream mAdbClientOutputStream = null;
    private BufferedInputStream mAdbClientInputStream = null;

    private ReadThread mAdbClientReadThread = null;
    private WriteThread mAdbClientWriteThread = null;

    private BlockingQueue<Data> mLocalClientBufferExs = new LinkedBlockingQueue<Data>();

    // for connect local adb client end

    public ExchangeMsgService() {
        super();
    }

    class ReadThread extends Thread {

        BufferedInputStream inputStream = null;
        BlockingQueue<Data> bufferExs = null;
        boolean stop = false;

        public void setStop() {
            stop = true;
        }

        public ReadThread(BufferedInputStream i, BlockingQueue<Data> bExs, String name) {
            super(name);
            inputStream = i;
            bufferExs = bExs;
        }

        @Override
        public void run() {
            int size = 0;
            byte[] buffer = new byte[1024];
            Data temp = null;
            try {
                while (!stop && ((size = inputStream.read(buffer)) > 0)) {
                    temp = new Data(buffer, 0, size);
                    bufferExs.put(temp);
                    LogExt.d(TAG, "received: " + temp.getString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    class WriteThread extends Thread {

        BufferedOutputStream outputStream = null;
        BlockingQueue<Data> bufferExs = null;
        boolean stop = false;

        public void setStop() {
            stop = true;
        }

        public WriteThread(BufferedOutputStream o, BlockingQueue<Data> bExs, String name) {
            super(name);
            outputStream = o;
            bufferExs = bExs;
        }

        @Override
        public void run() {
            Data buffer = null;
            try {
                while (!stop && (null != (buffer = bufferExs.take()))) {
                    LogExt.d(TAG, "write: " + buffer.getString());
                    outputStream.write(buffer.getBytes());
                    outputStream.flush();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    class WriteThread2 extends Thread {

        BlockingQueue<Data> bufferExs = null;
        Client client = null;
        boolean stop = false;

        public void setStop() {
            stop = true;
        }

        public WriteThread2(Client client, BlockingQueue<Data> bExs, String name) {
            super(name);
            bufferExs = bExs;
            this.client = client;
        }

        @Override
        public void run() {
            Data buffer = null;
            try {
                while (!stop && (null != (buffer = bufferExs.take()))) {
                    LogExt.d(TAG, "write: " + buffer.getString());
                    client.sendTCP(buffer);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private final class RemoteClientHandler extends Handler {
        public RemoteClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            LogExt.d(TAG, "ClientHandler handleMessage msg " + msg);
            switch (msg.what) {
                case MSG_CONNECT_REMOTE_SERVER:
                    mRemoteClient = new Client();
                    mRemoteClient.start();
                    Network.register(mRemoteClient);
                    mRemoteClient.addListener(mRemoteClientListener);
                    try {
                        LogExt.e(TAG, "start connect remote server");
                        mRemoteClient.connect(5000, (String) msg.obj, msg.arg2);
                        LogExt.e(TAG, "start connect remote over 1");
                        // Server communication after connection can go
                        // here, or in
                        // Listener#connected().
                        // mRemoteWriteThread = new WriteThread(mRemoteClient,
                        // mLocalClientBufferExs, "remote write thread");
                        // mRemoteWriteThread.start();
                    } catch (IOException ex) {
                        LogExt.e(TAG, "IOException", ex);
                    }
                    LogExt.e(TAG, "connect remote server over 2");
                    break;

                default:
                    break;
            }
        }
    }

    Listener mRemoteClientListener = new Listener() {

        @Override
        public void connected(Connection connection) {
            RegisterName registerName = new RegisterName();
            registerName.name = Configuration.TYPE_PHONE_CLIENT + "-" + mLocalIp + "-" +mUUID;
            mRemoteClient.sendTCP(registerName);
            LogExt.e(TAG, "connect remote server ok!");
        }

        @Override
        public void disconnected(Connection connection) {
            mRemoteClient.stop();
        }

        @Override
        public void received(Connection connection, Object object) {
            
            LogExt.d(TAG, "received Object " + object);
            
            if (object instanceof UpdateNames) {
                UpdateNames updateNames = (UpdateNames) object;
                return;
            }

            if (object instanceof ChatMessage) {
                ChatMessage chatMessage = (ChatMessage) object;
                // chatFrame.addMessage(chatMessage.text);
                // getCurrentTermSession().write(chatMessage.text);
                LogExt.d(TAG, "receive text: " + chatMessage.text);

                if (!TextUtils.isEmpty(chatMessage.text) && chatMessage.text.startsWith(Configuration.CMD_CONNECT_ADB)) {
                    connectLocalServer(IPv4v6Utils.getLocalIPAddress(), Configuration.ADB_SERVER_PORT);
                    String temp = chatMessage.text.substring(Configuration.CMD_CONNECT_ADB.length()).trim();
                    
                    LogExt.d(TAG, "start connect local adb pc terminal ip is " + temp);
                //}else if(!TextUtils.isEmpty(chatMessage.text) && chatMessage.text.startsWith(Configuration.)){
                    
                }
                return;
            }

            if (object instanceof Data) {
                Data bEx = (Data) object;
                try {
                    mRemoteClientBufferExs.put(bEx);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LogExt.d(TAG, "receive bufferEx: " + bEx.getString());
            }
        }

        @Override
        public void idle(Connection connection) {
            // TODO Auto-generated method stub
            super.idle(connection);
        }
    };

    private void remoteClientClose() {
        if (null != mRemoteWriteThread) {
            mRemoteWriteThread.setStop();
            mRemoteWriteThread = null;
        }
        if (null != mRemoteClient) {
            mRemoteClient.stop();
        }
    }

    private void localClientClose() {
        if (null != mAdbClientReadThread) {
            mAdbClientReadThread.setStop();
            mAdbClientReadThread = null;
        }
        if (null != mAdbClientWriteThread) {
            mAdbClientWriteThread.setStop();
            mAdbClientWriteThread = null;
        }
        if (null != mAdbClientSocket) {
            try {
                mAdbClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mAdbClientSocket = null;
        }
        if (null != mAdbClientOutputStream) {
            try {
                mAdbClientOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mAdbClientOutputStream = null;
        }

        if (null != mAdbClientInputStream) {
            try {
                mAdbClientInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mAdbClientInputStream = null;
        }

    }

    private final class LocalClientHandler extends Handler {
        public LocalClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            LogExt.d(TAG, "ServiceHandler handleMessage msg " + msg);
            switch (msg.what) {
                case MSG_CONNECT_LOCAL_SERVER:
                    if (null == mAdbClientSocket && null != msg.obj && !TextUtils.isEmpty((String) msg.obj)) {
                        mAdbClientSocket = new Socket();
                        try {
                            mAdbClientSocket.connect(new InetSocketAddress((String) msg.obj, Configuration.ADB_SERVER_PORT), 5000);
                            mAdbClientOutputStream = new BufferedOutputStream(mAdbClientSocket.getOutputStream());
                            mAdbClientInputStream = new BufferedInputStream(mAdbClientSocket.getInputStream());
                            mAdbClientReadThread = new ReadThread(mAdbClientInputStream, mLocalClientBufferExs, "client read thread");
                            mAdbClientReadThread.start();
                            mAdbClientWriteThread = new WriteThread(mAdbClientOutputStream, mRemoteClientBufferExs, "client write thread");
                            mAdbClientWriteThread.start();
                            
                            mRemoteWriteThread = new WriteThread2(mRemoteClient, mLocalClientBufferExs, "send to remote server thread");
                            mRemoteWriteThread.start();
                            LogExt.e(TAG, "client connect local adb socket ok");
                        } catch (IOException e) {
                            LogExt.e(TAG, "client connect socket error", e);
                            localClientClose();
                        }
                    } else {

                    }
                    break;

                default:
                    break;
            }
        }
    }

    public void connectLocalServer(String ip, int port) {
        mAdbLocalClientHandler.sendMessage(mAdbLocalClientHandler.obtainMessage(MSG_CONNECT_LOCAL_SERVER, 0, port, ip));
    }

    public void connectRemoteServer(String ip, int port) {
        mRemoteClientHandler.sendMessage(mRemoteClientHandler.obtainMessage(MSG_CONNECT_REMOTE_SERVER, 0, port, ip));
    }

    @Override
    public void onCreate() {
        // TODO: It would be nice to have an option to hold a partial wakelock
        // during processing, and to have a static startService(Context, Intent)
        // method that would launch the service & hand off a wakelock.

        super.onCreate();

        mLocalIp = IPv4v6Utils.getLocalIPAddress();
        mUUID = IPv4v6Utils.getUUID(this);

        HandlerThread thread = new HandlerThread("RemoteClient");
        thread.start();

        mRemoteClientLooper = thread.getLooper();
        mRemoteClientHandler = new RemoteClientHandler(mRemoteClientLooper);

        HandlerThread thread2 = new HandlerThread("LocalClient");
        thread2.start();

        mAdbLocalClientLooper = thread2.getLooper();
        mAdbLocalClientHandler = new LocalClientHandler(mAdbLocalClientLooper);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: the ret value need change
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mRemoteClientLooper.quit();
        remoteClientClose();
        mAdbLocalClientLooper.quit();
        localClientClose();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ExchangeBinder();
    }

    public class ExchangeBinder extends Binder {
        ExchangeMsgService getService() {
            return ExchangeMsgService.this;
        }
    }

}
