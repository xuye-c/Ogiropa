package com.example.ogiropa;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SavePoints extends Thread{
    private boolean isSaved = false;
    private final Context context;
    private final List<LocationItem> bufferList;

    public SavePoints(Context context, List<LocationItem> bufferList) {
        this.context = context;
        this.bufferList = new ArrayList<>(bufferList); // 防止并发修改
    }
    @Override
    public void run(){
        try{
            MarkerUtilsTool.writeMarkersToInternal(context, bufferList);
            isSaved = true;
        }catch (Exception e) {
            Log.e("SavePoints", "保存失败: " + e.getMessage());
        }

    }

    public boolean isSaved() {
        return isSaved;
    }
}
