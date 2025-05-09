package com.example.ogiropa;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DetectStep implements Runnable {
    private volatile boolean running = true;
    private volatile boolean stateChanged = false;
    private final BlockingQueue<SensorEvent> eventQueue = new LinkedBlockingQueue<>();

    //public int stepCount = 0;
    public long walkTime = 0;
    public long runTime = 0;
    public boolean jogging = false;
    private List<AccelerationSample> accelerationSamples = new ArrayList<>();


    public enum MotionState {
        STOPPED, WALKING, RUNNING
    }
    private MotionState currentState = MotionState.STOPPED;

    public MotionState getCurrentState() {
        return currentState;
    }
    private static class AccelerationSample {
        float acceleration; // 减去重力后的净加速度
        long timestamp; // 毫秒
        AccelerationSample(float acceleration, long timestamp) {
            this.acceleration = acceleration;
            this.timestamp = timestamp;
        }
    }


    public void stop() {
        Log.d("传感器线程", "DetectStep-stop()--Stopping thread");
        running = false;
        //eventQueue.offer(null); // 唤醒阻塞的 take()
    }

    public void addEvent(SensorEvent event) {
        eventQueue.offer(event);
    }

    @Override
    public void run() {
        Log.d("传感器线程", "DetectStep-run()--Thread started");
        while (running) {
            try {
                SensorEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
                if (event == null) continue; // 避免 null 出错
                handleSensorEvent(event);
                Log.d("传感器线程", "Handled event: " + event.sensor.getType());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Log.d("传感器线程", "DetectStep-run()--Thread stopped");
    }
    private void handleSensorEvent(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // 减去重力影响
            float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
            acceleration = Math.max(acceleration, 0f);

            long currentTime = System.currentTimeMillis();

            // 存样本
            accelerationSamples.add(new AccelerationSample(acceleration, currentTime));

            // 删除太旧的数据，只保留最近3秒
            long windowSize = 3000; // 3秒窗口
            while (!accelerationSamples.isEmpty() && (currentTime - accelerationSamples.get(0).timestamp > windowSize)) {
                accelerationSamples.remove(0);
            }

            // 积分计算总速度变化量
            float totalVelocityChange = 0f;
            float deltaTime=0;
            for (int i = 1; i < accelerationSamples.size(); i++) {
                AccelerationSample prev = accelerationSamples.get(i - 1);
                AccelerationSample now = accelerationSamples.get(i);

                deltaTime = (now.timestamp - prev.timestamp) / 1000.0f; // 毫秒转秒
                totalVelocityChange += prev.acceleration * deltaTime;
            }

            // 平均速度变化量（除以总时间）
            float totalDuration = (accelerationSamples.get(accelerationSamples.size() - 1).timestamp
                    - accelerationSamples.get(0).timestamp) / 1000.0f;
            float averageVelocity = (totalDuration > 0) ? (totalVelocityChange / totalDuration) : 0f;

            // 根据平均速度判断运动状态
            MotionState oldState = currentState;
            if (averageVelocity > 3.0f) {
                currentState = MotionState.RUNNING;
                jogging = true;
                runTime += deltaTime*1000.0f;
            } else if (averageVelocity > 0.5f) {
                currentState = MotionState.WALKING;
                jogging = false;
                walkTime+=deltaTime*1000.0f;
            } else {
                currentState = MotionState.STOPPED;
                jogging = false;
            }

            if (currentState != oldState) {
                stateChanged = true;
                Log.d("切歌", "运动状态变化: " + oldState + " -> " + currentState);
            }
        }
    }

    public boolean getAndResetStateChanged() {
        //Log.d("切歌","DetectStep线程，getAndResetStateChanged() is called");
        boolean changed = stateChanged;
        //Log.d("切歌","当前状态改变："+changed);
        stateChanged = false;
        return changed;
    }
    public boolean isJogging() {
        return jogging;
    }
    public float get_walkTime(){
        return walkTime;
    }
    public float get_runTime(){
        return runTime;
    }
    public String getReport() {
        return  "\nwalk duration: " + walkTime / 1000 + " s" +
                "\nrun duration: " + runTime / 1000 + " s" ;
    }
}
