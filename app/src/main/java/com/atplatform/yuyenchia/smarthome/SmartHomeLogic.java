package com.atplatform.yuyenchia.smarthome;

import android.util.Log;

public class SmartHomeLogic {
    private boolean SmokeState;
    private boolean HumanState;
    public boolean Alarmstate_LOCAL;
    private double Temperture;
    private double Distance;
    private double Humidity;
    private int timeticket=0;
    private boolean AlarmFlag=false;


    public void RenewData(boolean SmokeState1,boolean HuamnState1, double Tempture1, double Distance1,double Humidity1)
    {
        SmokeState=SmokeState1;
        Humidity=Humidity1;
        Temperture=Tempture1;
        Distance=Distance1;
        HumanState=HuamnState1;
    }

    public boolean Detection_cycle()
    {
        if (SmokeState||Temperture>70)
        {
            Alarmstate_LOCAL=true;
        }
        else if (HumanState&&Distance>10&&Distance<100)
        {
            Alarmstate_LOCAL=true;
        }
        else
        {
            Alarmstate_LOCAL=false;
        }
        if(Alarmstate_LOCAL&&!AlarmFlag&&timeticket<5)
        {
            timeticket++;
            if(timeticket==5)
            {
                AlarmFlag=true;
                timeticket=0;
            }
        }
        else if(!Alarmstate_LOCAL&&!AlarmFlag&&timeticket<5)
        {
            timeticket=0;
        }
        if(AlarmFlag)
        {
            timeticket++;
            if(timeticket>100)
            {
                timeticket=0;
                AlarmFlag=false;
            }
        }
       // Log.e("Logic","Flag:"+AlarmFlag+"timeticket"+timeticket+"Alarmstate_LOCAL:"+Alarmstate_LOCAL+"");
        return AlarmFlag;
    }

}
