package com.odonataworshop.audio.server;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.zip.GZIPInputStream;

/**
 * Created by maya on 11.12.2014 г..
 */
public class PCMSocket {
    private AudioTrack audioTrack;
    private Socket socket;
    private TextUpdateHandler mUIHandler;
    private ReadFromSocket readThread;

    public PCMSocket() {
        readThread = new ReadFromSocket();
    }

    public PCMSocket(Socket aSocket) {
        socket = aSocket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void stop() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {

        readThread.start();
    }

    public void setUIHandler(TextUpdateHandler aUIHandler) {
        mUIHandler = aUIHandler;
    }

    public class ReadFromSocket extends Thread {
        public void run() {
            try {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                final BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());//socket.getInputStream();

                final int minSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, minSize,
                        AudioTrack.MODE_STREAM);
              //  audioTrack.setStereoVolume(1f, 1f);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        byte[] buffer = new byte[minSize];
                        audioTrack.play();
                        mUIHandler.post("Audio started playing...");
                        try {
                            int read = 0;
                            while ((read = inputStream.read(buffer)) != -1) {
                                audioTrack.write(buffer, 0, read);
                                audioTrack.flush();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            mUIHandler.post( "Audio stopped playing..." + e.getMessage());
                        }

                        audioTrack.stop();
                        mUIHandler.post( "Audio stopped playing...");
                        try {
                            inputStream.close();
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            mUIHandler.post( "Audio stopped playing...");

                        }
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
                mUIHandler.post( "Audio stopped playing..." + e.getMessage());

            }
        }
    }

}
