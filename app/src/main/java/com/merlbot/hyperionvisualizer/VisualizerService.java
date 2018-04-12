package com.merlbot.hyperionvisualizer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class VisualizerService extends Service {
    static final String CHANNEL_DEFAULT_IMPORTANCE = "HIGH";
    static final int SINGLE_COLOR = 0;
    static final int RAINBOW_SWIRL = 1;
    static String UDP_IP = "";
    static int UDP_PORT = -1;
    static int LED_COUNT = -1;
    static int RATE = 0;
    static int VISUALIZATION_METHOD = 0;
    static float TURNAROUND_RATE = 0;
    private static final int MY_PERMISSIONS_RECORD_AUDIO = 200;
    private static final int ONGOING_NOTIFICATION_ID = 30;
    static float turnaround = 0;
    static Visualizer vis = null;
    private Handler handler = new Handler();

    public VisualizerService() {
    }

    private static final String TAG = "MyService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Stopped visualizing", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        vis.setDataCaptureListener(null, 0, false, false);
        vis.setEnabled(false);
        handler.removeCallbacksAndMessages(null);
        vis = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Log.d(TAG, "onStartCommand");
        UDP_IP = intent.getStringExtra("ip_address");
        UDP_PORT = intent.getIntExtra("port", UDP_PORT);
        LED_COUNT = intent.getIntExtra("led_count", LED_COUNT);
        RATE = intent.getIntExtra("visualizer_rate", RATE);
        TURNAROUND_RATE = intent.getFloatExtra("color_rate", TURNAROUND_RATE);
        VISUALIZATION_METHOD = intent.getIntExtra("visualization_method", VISUALIZATION_METHOD);
        turnaround = intent.getIntExtra("color_offset", (int) turnaround);


        Toast.makeText(this, "Visualizing to IP " + UDP_IP, Toast.LENGTH_LONG).show();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);


        String channel_id = "TEST";
        String channel_name = "TEST";
        NotificationChannel channel = new NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        Notification notification =
                new Notification.Builder(this, "TEST")
                        .setContentTitle(getText(R.string.foreground_service_notification_title))
                        .setContentText(getText(R.string.foreground_service_notification_title))
                        .setSmallIcon(R.drawable.icon)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .setOngoing(true)
                        .build();
        notification.flags|=Notification.FLAG_NO_CLEAR;

        startForeground(ONGOING_NOTIFICATION_ID, notification);


        vis = new Visualizer(0);
        MyVisualizer visListener = new MyVisualizer();
        vis.setDataCaptureListener(visListener, RATE, true, false);
        int res = vis.setEnabled(true);
        return super.onStartCommand(intent, flags, startid);
    }


    class MyVisualizer implements Visualizer.OnDataCaptureListener{
        final String TAG = "MyVisualizer";

        private void sendBytes(byte[] bytes){
            AudioManager manager = (AudioManager)VisualizerService.this.getSystemService(Context.AUDIO_SERVICE);
            int steps = bytes.length / LED_COUNT;
            float[] hues = new float[96];
            for(int j = 0; j < LED_COUNT; j++){
                int tmp_avg = 0;
                for(int k = 0; k < steps; k++){
                    tmp_avg += (int) bytes[steps*j+k] & 0xFF;
                }
                float intres = tmp_avg / (float) steps;
                hues[j] = intres/(float) 255;
            }

            MyVisualizer.MyInnerThread mit = new MyVisualizer.MyInnerThread(hues);
            mit.start();

        }

        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int i) {
            sendBytes(bytes);
        }


        @Override
        public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int i) {
            sendBytes(bytes);
        }

        class MyInnerThread extends Thread{
            float[] hues;
            MyInnerThread(float[] hues){
                this.hues = hues;
            }

            public void run(){
                sendByteArray(getByteArray(hues));
            }

            byte[] getByteArray(float[] vs){
                byte[] bytes = new byte[LED_COUNT*3];
                int[] hues = new int[LED_COUNT];

                if(VISUALIZATION_METHOD == R.id.radiobutton_rainbow){
                    hues = rainBowSwirl();
                }else if(VISUALIZATION_METHOD == R.id.radiobutton_single_colour){
                    hues = single_color();
                }else{
                    for(int i = 0; i < LED_COUNT; i++){
                        hues[i] = 0;
                    }
                }

                for(int i = 0; i < LED_COUNT; i++){
                    float[] hsv = new float[]{(float) hues[i], 1, vs[i]};
                    int color = Color.HSVToColor(hsv);
                    bytes[i*3] = (byte) Color.red(color);
                    bytes[i*3+1] = (byte) Color.green(color);
                    bytes[i*3+2] = (byte) Color.blue(color);
                }
                return bytes;
            }

            int[] rainBowSwirl(){
                int[] res = new int[LED_COUNT];
                for(int i = 0; i < LED_COUNT; i++){
                    res[i] = ((int) (((float) i / LED_COUNT) * 360) + (int) turnaround) % 360;
                }
                turnaround += TURNAROUND_RATE;
                return res;
            }

            int[] single_color(){
                int[] res = new int[LED_COUNT];
                for(int i = 0; i < LED_COUNT; i++){
                    res[i] = (int) turnaround % 360;
                }
                turnaround += TURNAROUND_RATE;
                return res;
            }

            void sendByteArray(byte[] bytes){
                DatagramSocket ds = null;
                try {
                    ds = new DatagramSocket();
                    InetAddress serverAddr = InetAddress.getByName(UDP_IP);
                    DatagramPacket dp;
                    dp = new DatagramPacket(bytes, bytes.length, serverAddr, UDP_PORT);
                    ds.send(dp);
                } catch (SocketException e) {
                    e.printStackTrace();
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (ds != null) {
                        ds.close();
                    }
                }
            }
        }
    }
}
