package com.atplatform.yuyenchia.smarthome;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;


import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;


import java.io.IOException;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String InfraredSensor_NAME = "BCM23";//人体红外传感器
    private static final String Trig_NAME = "BCM27";//超声波传感器trig
    private static final String Echo_NAME = "BCM22";//超声波传感器echo
    private static final String Smoke_NAME = "BCM4";//烟雾传感器
    private static final String Buzzer_NAME = "BCM17";//蜂鸣器
    private static final String I2C_DEVICE_NAME = "I2C1";//温湿度传感器I2C
    private static final String LED_NAME = "BCM5";//LED
    private static final String UART_DEVICE_NAME = "UART0";//串口
    private static final int BAUD_RATE = 115200;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = 1;
    private static final int CHUNK_SIZE = 16;
    private static final int I2C_ADDRESS = 0x5C;


    private double Distance;//超声波传感器读取距离
    private double Temperature;//温湿度传感器读取温度
    private double Humidity;//温湿度传感器读取湿度
    private boolean humanState = false;//红外传感器转态
    private boolean smokeState = false;//烟雾传感器状态
    private boolean alarm_state = false;//报警器当前状态

    private long TimeTicket = 0;

    private Gpio InfraredSensor;//红外传感器
    private Gpio SmokeSensor;//烟雾传感器
    private Gpio BuzzerGpio;//蜂鸣器
    private Gpio LEDGpio;//LED

    private Handler LoopHandler = new Handler();
    private Handler ReadI2cHandler = new Handler();
    private SensorManager mSensorManager;
    private I2cDevice mI2cDevice;
    private UartDevice mUartDevice;

    private SmartHomeDataUpload Smart_Home_Data_Upload = new SmartHomeDataUpload();
    private SmartHomeLogic Smart_Home_Logic = new SmartHomeLogic();
    private ScanDatabase Smart_Home_Scan_Database = new ScanDatabase("SmatHome", "5de89812dd3c13007fcfcee5");
//    public static Typeface typefaceHeiTi;
    TextView View_temp, View_humidity, view_human, View_smoke, distanceview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeripheralManager Manager = PeripheralManager.getInstance();
        setContentView(R.layout.activity_main);
        view_human = findViewById(R.id.textView2);
        distanceview = findViewById(R.id.textView5);
        View_temp = findViewById(R.id.textView7);
        View_humidity = findViewById(R.id.textView9);
        View_smoke = findViewById(R.id.textView11);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        try {

            BuzzerGpio = Manager.openGpio(Buzzer_NAME);
            BuzzerGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);

            InfraredSensor = Manager.openGpio(InfraredSensor_NAME);
            InfraredSensor.setDirection(Gpio.DIRECTION_IN);
            InfraredSensor.setActiveType(Gpio.ACTIVE_HIGH);
            InfraredSensor.setEdgeTriggerType(Gpio.EDGE_BOTH);
            InfraredSensor.registerGpioCallback(InfraredSensorCallback);

            SmokeSensor = Manager.openGpio(Smoke_NAME);
            SmokeSensor.setDirection(Gpio.DIRECTION_IN);
            SmokeSensor.setActiveType(Gpio.ACTIVE_HIGH);
            SmokeSensor.setEdgeTriggerType(Gpio.EDGE_BOTH);
            SmokeSensor.registerGpioCallback(SmokeSensorSensorCallback);

            LEDGpio = Manager.openGpio(LED_NAME);
            LEDGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            mI2cDevice = Manager.openI2cDevice(I2C_DEVICE_NAME, I2C_ADDRESS);
            //sensor init
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mSensorManager.registerDynamicSensorCallback(new mDynamicSensorCallback());
            Hcsr04UltrasonicDriver hcsr04UltrasonicDriver = new Hcsr04UltrasonicDriver(Trig_NAME, Echo_NAME);
            hcsr04UltrasonicDriver.register();

            mUartDevice = Manager.openUartDevice(UART_DEVICE_NAME);
            mUartDevice.setBaudrate(BAUD_RATE);
            mUartDevice.setDataSize(DATA_BITS);
            mUartDevice.setParity(UartDevice.PARITY_NONE);
            mUartDevice.setStopBits(STOP_BITS);
            mUartDevice.registerUartDeviceCallback(mUartDeviceCallback);

            ReadI2cHandler.post(ReadI2cRunnable);
            LoopHandler.post(looper);

        } catch (IOException e) {
            Log.e(TAG, "Unable to on GPIO", e);
        }

//        typefaceHeiTi = Typeface.createFromAsset(getAssets(), "fonts/msyh.ttf");
//
//        try {
//            Field field = Typeface.class.getDeclaredField("MONOSPACE");
//            field.setAccessible(true);
//            field.set(null, typefaceHeiTi);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        if (smokeState)
            View_smoke.setText("有烟雾");
        else
            View_smoke.setText("没烟雾");

        if (humanState)
            view_human.setText("有人");
        else
            view_human.setText("没人");
        View_humidity.setText(String.valueOf(Humidity));
        View_temp.setText(String.valueOf(Temperature));
        distanceview.setText(String.valueOf(Distance));
    }

    private Runnable looper = new Runnable() {//主任务循环（1ms
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            TimeTicket++;
            if (TimeTicket > 9999) {
                TimeTicket = 0;
            }

            if (TimeTicket % 20 == 0) {

                Smart_Home_Logic.RenewData(smokeState, humanState, Temperature, Distance, Humidity);
                alarm_state = Smart_Home_Logic.Detection_cycle();
            }
            if (TimeTicket % 100 == 0) {
                Smart_Home_Data_Upload.RenewData(smokeState, humanState, Smart_Home_Logic.Alarmstate_LOCAL, Temperature, Distance, Humidity);
                Smart_Home_Data_Upload.up_load_data();
            }
            if (TimeTicket % 100 == 0) {
                Smart_Home_Scan_Database.ReadAlerm();
            }
            if (Smart_Home_Scan_Database.alarm_switch && alarm_state) {
                setgpioValue(BuzzerGpio, false);
                if (TimeTicket % 60 < 30)
                    setgpioValue(LEDGpio, true);
                else
                    setgpioValue(LEDGpio, false);
            } else {
                setgpioValue(BuzzerGpio, true);
                setgpioValue(LEDGpio, false);
            }

            if (smokeState)
                View_smoke.setText("smoke");
            else
                View_smoke.setText("no smoke");

            if (humanState)
                view_human.setText("people");
            else
                view_human.setText("no people");
            View_humidity.setText(String.valueOf(Humidity));
            View_temp.setText(String.valueOf(Temperature));
            distanceview.setText(String.valueOf(Distance));
            LoopHandler.postDelayed(looper, 1);

        }
    };

    private UartDeviceCallback mUartDeviceCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            try {
                byte[] buffer = new byte[CHUNK_SIZE];
                while (mUartDevice.read(buffer, buffer.length) > 0) {
                    Log.w(TAG, "read from DHT12:" + new String(buffer) + "length:" + buffer.length);
                    String temp = new String(buffer);
                    String[] words = temp.trim().split(",");
                    String errorcase = words[0];
                    if (errorcase.equals("OK")) {
                        if (words[1] != null)
                            if (isNumeric(words[1]))
                                Temperature = Double.parseDouble(words[1]);
                        if (words[2] != null)
                            if (isNumeric(words[2]))
                                Humidity = Double.parseDouble(words[2]);
                    } else {
                        Log.w(TAG, "DHT12 Error Event:" + errorcase);
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to transfer data over UART", e);
            }
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };

    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("-?[0-9]+.?[0-9]+");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }

    private SensorEventListener mDistanceListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values[0] > 0)
                Distance = event.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };

    private class mDynamicSensorCallback extends SensorManager.DynamicSensorCallback {
        @Override
        public void onDynamicSensorConnected(Sensor sensor) {
            if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                mSensorManager.registerListener(mDistanceListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

        }

        @Override
        public void onDynamicSensorDisconnected(Sensor sensor) {
            super.onDynamicSensorDisconnected(sensor);
        }
    }

    private final GpioCallback SmokeSensorSensorCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                if (gpio.getValue()) {
                    Log.e("有烟雾", gpio.getValue() + "");
                    smokeState = true;
                } else {
                    Log.e("没有烟雾", gpio.getValue() + "");
                    smokeState = false;
                }
            } catch (IOException e) {
                Log.i(TAG, "SmokeSensor not in used");
            }
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };

    private final GpioCallback InfraredSensorCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                if (gpio.getValue()) {
                    Log.e("有人来了", gpio.getValue() + "");
                    humanState = true;
                } else {
                    Log.e("没有人", gpio.getValue() + "");
                    humanState = false;
                }
            } catch (IOException e) {
                Log.i(TAG, "InfraredSensor not in used");
            }
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };

    private void setgpioValue(Gpio GPIOS, boolean value) {
        try {
            GPIOS.setValue(value);
        } catch (IOException e) {
            Log.e(TAG, "Error updating GPIO value", e);
        }
    }

//    private void readHumidityAndTemperature(){
//        try {
//            byte[] temp = new byte[5];
////            temp[0]=mI2cDevice.readRegByte(0x00);
////            temp[1]=mI2cDevice.readRegByte(0x01);
////            temp[2]=mI2cDevice.readRegByte(0x02);
////            temp[3]=mI2cDevice.readRegByte(0x03);
////            temp[4]=mI2cDevice.readRegByte(0x04);
//
////            temp[0]= (byte) mI2cDevice.readRegWord(0x00);
////            temp[1]= (byte) mI2cDevice.readRegWord(0x01);
////            temp[2]= (byte) mI2cDevice.readRegWord(0x02);
////            temp[3]= (byte) mI2cDevice.readRegWord(0x03);
////            temp[4]= (byte) mI2cDevice.readRegWord(0x04);
//
//            mI2cDevice.readRegBuffer(0x00, temp, 5);
//            //mI2cDevice.read( temp, temp.length);
//
//            if ((temp[0] + temp[1] + temp[2] + temp[3]) % 256 != (int)temp[4]) {
//                Humidity=0;
//                Temperature=0;
//                return;
//            }
//            // 处理湿度数据
//            Humidity = Double.parseDouble(((int)temp[0] + "." + (int)temp[1]));
//
//            // 处理温度数据
//            if (temp[3] < 0) {
//                Temperature = -Double.parseDouble(((int)temp[2] + "." + -(int)temp[3]));
//            } else {
//                Temperature = Double.parseDouble(((int) temp[2] + "." + (int) temp[3]));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private Runnable ReadI2cRunnable = new Runnable() {
        @Override
        public void run() {
            //readHumidityAndTemperature();
            ReadI2cHandler.postDelayed(ReadI2cRunnable, 1000);
        }
    };

    protected void onDestroy() {
        super.onDestroy();
        LoopHandler.removeCallbacks(looper);
        ReadI2cHandler.removeCallbacks(ReadI2cRunnable);
        mSensorManager.unregisterListener(mDistanceListener);
        if (InfraredSensor != null) {
            InfraredSensor.unregisterGpioCallback(InfraredSensorCallback);
            try {
                InfraredSensor.close();
                InfraredSensor = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close InfraredSensor", e);
            }
        }
        if (SmokeSensor != null) {
            SmokeSensor.unregisterGpioCallback(SmokeSensorSensorCallback);
            try {
                SmokeSensor.close();
                SmokeSensor = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close SmokeSensor", e);
            }
        }
        if (mI2cDevice != null) {
            try {
                mI2cDevice.close();
                mI2cDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close I2C device", e);
            }
        }
        if (mUartDevice != null) {
            try {
                mUartDevice.close();
                mUartDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close UART device", e);
            }
        }
    }
}
