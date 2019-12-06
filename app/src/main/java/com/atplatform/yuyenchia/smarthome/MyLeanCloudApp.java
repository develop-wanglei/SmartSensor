package com.atplatform.yuyenchia.smarthome;

import android.app.Application;
import android.graphics.Typeface;

import com.avos.avoscloud.AVOSCloud;

import java.lang.reflect.Field;


public class MyLeanCloudApp extends Application {
    public static Typeface typefaceHeiTi;//字体参数变量
    @Override
    public void onCreate() {
        super.onCreate();
        AVOSCloud.initialize(this,"3x1teTj3EBqyr6s0UzG0nroP-gzGzoHsz","evT4PsqqdJzJvjxNXKeYXFYw");
        // 放在 SDK 初始化语句 AVOSCloud.initialize() 后面，只需要调用一次即可
        AVOSCloud.setDebugLogEnabled(true);//app发布后关闭
        /*****************************************************************************************************************/
        //字体初始化
        typefaceHeiTi = Typeface.createFromAsset(getAssets(), "fonts/msyh.ttf");
        try {
            Field field = Typeface.class.getDeclaredField("MONOSPACE");
            field.setAccessible(true);
            field.set(null, typefaceHeiTi);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}