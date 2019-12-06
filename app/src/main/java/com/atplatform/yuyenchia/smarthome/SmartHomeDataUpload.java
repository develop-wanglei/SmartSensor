package com.atplatform.yuyenchia.smarthome;

import android.util.Log;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.SaveCallback;

public class SmartHomeDataUpload {
    private boolean SmokeState;
    private boolean HumanState;
    private boolean AlarmState;
    private double Temperture;
    private double Distance;
    private double Humidity;
    AVObject UP_DATA=AVObject.createWithoutData("SmatHome","5de89812dd3c13007fcfcee5");



    public void RenewData(boolean SmokeState1,boolean HuamnState1,boolean AlarmState1, double Tempture1, double Distance1,double Humidity1)
    {
        SmokeState=SmokeState1;
        AlarmState=AlarmState1;
        Humidity=Humidity1;
        Temperture=Tempture1;
        Distance=Distance1;
        HumanState=HuamnState1;
    }


    public void up_load_data()
    {
        UP_DATA.put("SMOKE_STATE",SmokeState);
        UP_DATA.put("HUMAN_STATE",HumanState);
        UP_DATA.put("TEMPERATURE",Temperture);
        UP_DATA.put("DISTANCE",Distance);
        UP_DATA.put("HUMIDITY",Humidity);
        UP_DATA.put("ALARM_STATE",AlarmState);
        UP_DATA.saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                Log.d("smart_home_data","saved");
            }
        });
    }
}
