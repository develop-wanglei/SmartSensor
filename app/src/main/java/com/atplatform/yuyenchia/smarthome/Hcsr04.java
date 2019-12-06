package com.atplatform.yuyenchia.smarthome;


import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Hcsr04 implements AutoCloseable {
    private static final String TAG = Hcsr04.class.getSimpleName();
    private static final int pauseInMicro = 10;
    private Gpio trigGpio, echoGpio;
    private Handler handler = new Handler();
    private long startTime, ellapsedTime;
    private float distanceInCm;
    private boolean flag=false;
    private Runnable startTrigger = new Runnable() {
        @Override
        public void run() {
            try {
                trigGpio.setValue(!trigGpio.getValue());
                busyWaitMicros(pauseInMicro);
                trigGpio.setValue(!trigGpio.getValue());
                long longTime = System.nanoTime();
                while (!echoGpio.getValue()&& !flag)
                {
                    startTime = System.nanoTime();
                    if(startTime- longTime >1000000)
                        flag=true;
                }
                if(!flag)
                {
                    while (echoGpio.getValue())
                        ellapsedTime = System.nanoTime() - startTime;
                    ellapsedTime = TimeUnit.NANOSECONDS.toMicros(ellapsedTime);
                    distanceInCm = ellapsedTime / 58;
                    if(distanceInCm>1500)
                        distanceInCm=0;
                }
                else
                {
                    Log.e(TAG, "timeout");
                    flag=false;
                }
                handler.postDelayed(startTrigger, 100);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public Hcsr04(String trigPin, String echoPin) throws IOException {
        PeripheralManager service = PeripheralManager.getInstance();
        trigGpio = service.openGpio(trigPin);
        echoGpio = service.openGpio(echoPin);
        configureGpio(trigGpio, echoGpio);
    }

    public static void busyWaitMicros(long micros) {
        long waitUntil = System.nanoTime() + (micros * 1_000);
        while (waitUntil > System.nanoTime()) {
        }
    }

    @Override
    public void close() {
        handler.removeCallbacks(startTrigger);
        try {
            trigGpio.close();
            echoGpio.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureGpio(Gpio trigGpio, Gpio echoGpio) {
        try {
            trigGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            echoGpio.setDirection(Gpio.DIRECTION_IN);

            trigGpio.setActiveType(Gpio.ACTIVE_HIGH);
            echoGpio.setActiveType(Gpio.ACTIVE_HIGH);
            echoGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            handler.post(startTrigger);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public float[] getProximityDistance() {
        return new float[]{distanceInCm};
    }
}