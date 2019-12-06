package com.atplatform.yuyenchia.smarthome;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.GetCallback;
import com.avos.avoscloud.SaveCallback;



public class ScanDatabase {
    public boolean alarm_switch=false;
    private AVObject smart_home_temp;
    private AVObject temp;
    private boolean toaststate=false;
    public ScanDatabase(String DATACLASS, String ID) {
        smart_home_temp = AVObject.createWithoutData(DATACLASS, ID);
        smart_home_temp.put("ALARM_STATE",false);
        smart_home_temp.put("SMOKE_STATE",false);
        smart_home_temp.put("HUMAN_STATE",false);
        smart_home_temp.put("ALARM_SWITCH",true);
        smart_home_temp.put("TEMPERATURE",0);
        smart_home_temp.put("DISTANCE",0);
        smart_home_temp.put("HUMIDITY",0);
        smart_home_temp.saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                Log.d("saved", "success!");
            }
        });//向数据库写入初始化数据
        temp=AVObject.createWithoutData(DATACLASS,ID);
    }



    public void ReadAlerm()  {
        //    private   AVObject temp=AVObject.createWithoutData(dc,id);
            temp.fetchInBackground(new GetCallback<AVObject>() {
                @Override
                public void done(AVObject avObject, AVException e) {
                    if(avObject!=null)
                    {
                        alarm_switch=avObject.getBoolean("ALARM_SWITCH");
                        toaststate=false;
                    }
                    else
                    {
                        if(!toaststate)
                        {
                            toaststate=true;
                        }
                    }
                    if (alarm_switch)
                    {
                        Log.d("get alarm","open");
                    }
                    else {
                        Log.d("get alarm","off");
                    }
                }
            });


    }
}
