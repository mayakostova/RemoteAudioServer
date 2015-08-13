package com.odonataworshop.audio.server;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RemoteAudioServer extends Activity {

    private ServerSocket serverSocket;

    //Handler updateConversationHandler;

    Thread serverThread = null;

    private TextView text;

    public static final int SERVERPORT = 9991;
    private PCMSocket mPcmSocket;
    private PowerManager.WakeLock mWakeLock;
    private TextUpdateHandler updateHandler;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if(!(wifi.isConnected() || mobile.isConnected())){
            Toast.makeText(this, "No network connection!", Toast.LENGTH_LONG).show();
            finish();
            return;
            //System.exit(0);
        }
        setContentView(R.layout.main);

        text = (TextView) findViewById(R.id.text2);
        updateHandler = new TextUpdateHandler(this);
        updateHandler.setText(text);
        Button stop = (Button) findViewById(R.id.button);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPcmSocket != null) {
                    mPcmSocket.stop();
                }
            }
        });

       // updateConversationHandler = new Handler();

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                serverThread.interrupt();
                serverThread = null;
            }
                if (mWakeLock != null && mWakeLock.isHeld()) {
                    try {
                        mWakeLock.release();
                    } catch (Throwable th) {
                        // ignoring this exception, probably wakeLock was already released
                    }
                } else {

                    // should never happen during normal workflow
                }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, RemoteAudioServer.class.getCanonicalName());
            mWakeLock.acquire();
            Socket socket = null;
            try {
                    serverSocket = new ServerSocket(SERVERPORT);
                serverSocket.setReceiveBufferSize(100*1024*100);
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateHandler.post(Utils.getIPAddress(true) + ":" + serverSocket.getLocalPort());
            mPcmSocket = null;
            List<PCMSocket> pool = Collections.synchronizedList(new LinkedList<PCMSocket>());
            for (int i = 0; i < 5; i ++) { pool.add (new PCMSocket ()); }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                    updateHandler.post("Client Connected...");
                    mPcmSocket = pool.remove(0);
                    mPcmSocket.setSocket(socket);
                    mPcmSocket.setUIHandler(updateHandler);
                    mPcmSocket.start();
                    pool.add(new PCMSocket());
                    //in pcmSocket
                    // updateConversationHandler.post(new updateUIThread(read));
                } catch (IOException e) {
                    e.printStackTrace();
                    updateHandler.post("Client Stopped...");
                }
            }

        }
    }
}
