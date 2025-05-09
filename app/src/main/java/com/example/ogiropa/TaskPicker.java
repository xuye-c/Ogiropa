package com.example.ogiropa;

import static com.example.ogiropa.MainActivity.ogiropa;
import static com.example.ogiropa.MainActivity.totalpoints;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;


public class TaskPicker implements Runnable{
    private Context context;
    private boolean isPicked = false;
    private volatile boolean isRunning = true;
    private long estimatedTimeMillis = 20*60*1000;
    private Handler handler;
    private boolean onTime;
    private long lastStartTime = System.currentTimeMillis();


    public TaskPicker(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
    }
    private LocationItem item;
    private LocationItem lastitem=null;
    public void run() {
        Log.d("取点", "TaskPicker Runs!");
        while (isRunning) {
                //List<LocationItem> list = MarkerUtilsTool.readMarkersFromRaw(context, R.raw.points);
            if(!isPicked) {
                List<LocationItem> list = MarkerUtilsTool.readMarkersFromInternal(context);
                item = MarkerUtilsTool.getRandomMarker(list);
                if (lastitem != null) {
                    Log.d("到达", "上次目标点: " + lastitem.getNote());
                    long elapsed = System.currentTimeMillis() - lastStartTime;
                    onTime = elapsed <= estimatedTimeMillis;
                    Log.d("到达", "实际耗时: " + elapsed / 1000 + "s，是否按时: " + onTime);
                    if(onTime){
                        ogiropa++;
                        Log.d("欧吉罗巴到达","ogiropa++-->" + ogiropa);
                    }else{
                        Log.d("欧吉罗巴到达","超时了。");
                    }
                    double distance = DistanceUtil.haversineDistance(item.getLat(), item.getLng(), lastitem.getLat(), lastitem.getLng());
                    estimatedTimeMillis = (long) ((distance / 0.9) * 1000) + 60000 - 40000 * ogiropa + 20000 * totalpoints;

                }
                if (item != null && !item.equals(lastitem)) {
                    Log.d("到达", "预计倒计时时间: " + estimatedTimeMillis / 1000 + "s, =" + estimatedTimeMillis / 60000 + "min");
                    Message msg = handler.obtainMessage();
                    msg.what = 1;
                    msg.obj = item;
                    handler.sendMessage(msg);
                    lastitem = item;
                    isPicked = true;
                    Log.d("到达", "选中目标点: " + item.getNote() + " → (" + item.getLat() + ", " + item.getLng() + ")");
                    //开始或重置计时
                    lastStartTime = System.currentTimeMillis();
                }
            }
            try {
                Thread.sleep(100); // 避免空跑
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void stopTaskpicker(){
        isRunning = false;
    }
    public void isPickedsetFalse(){
        isPicked = false;
    }

    public boolean isPicked() {
        return isPicked;
    }

    public LocationItem getCurrentTask(){
        return item;
    }

    public boolean isOnTime() {
        return onTime;
    }

    public long getEstimatedTimeMillis() {
        return estimatedTimeMillis;
    }
}
