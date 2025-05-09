package com.example.ogiropa;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;


public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationSource, AMapLocationListener {
    //地图
    private LatLng currentLatLng=null;
    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocationClient locationClient;
    private AMapLocationClientOption mLocationoption;
    MapView mapView = null;
    AMap aMap = null;
    private Marker currentTargetMarker = null;
    private PolylineOptions polylineOptions;
    private Polyline polyline;
    private boolean isTracking = false;
    private Button startButton, endButton, btnViewButton, markLocationButton;
            //,saveButton, recordButton, mediaControlButton;

    //sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    //private Sensor stepdetecter;
    private DetectStep detectStep;
    private Thread detectThread;
    //测距
    private LatLng lastPoint = null; // 当前总是更新，无论跳不跳
    private LatLng lastAcceptedPoint = null; // 用于画线（只接受合理距离）
    private LinkedList<Float> recentDistances = new LinkedList<>();
    private LatLng lastTotalLatLng;     // 用于计算总距离的上一个点
    private float totalDistance = 0;   // 总距离（米）
    private float runningDistance = 0; // 单位：米
    private LatLng lastRunningLatLng = null;
    private float walking_v = 0.0f;
    private float runniing_v = 0.0f;
    private boolean TrackFinished = false;
    private String report;
    private TextView resultTextView;
    private boolean isMarked = false;
    //TaskPicker
    private long lastArrivalTime = 0L; // 毫秒时间戳
    private TaskPicker picker;
    private Thread taskPickerthread;
    //private boolean newTaskRecieved=false;
    //新增打卡点：
    private List<LocationItem> bufferList = new ArrayList<>();
    private List<Marker> tempMarkers = new ArrayList<>(); // 用于存储临时标记
    private SavePoints SavePointsThread;
    private boolean PointsSaved = true;
    //欧吉罗巴得分：
    public static int ogiropa = 0;
    public static int totalpoints = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //instance = this;
        // 隐私合规设置（必须写在调用 SDK 之前）
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);

        initJsonIfNotExists();

        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                }, 1);
            }
        }
        //获取地图控件引用z
        mapView = (MapView) findViewById(R.id.mapView);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();
        //Log.d("调试", "getMap 获取");
        aMap.setLocationSource(this);
        aMap.setMyLocationEnabled(true);
        aMap.setOnMapClickListener(latLng -> {
            showAddLocationDialog(latLng);
        });
        //Log.d("调试", "setMyLocationEnabled");
        //传感器注册
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //stepdetecter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (accelerometer == null) {
            Toast.makeText(this, "加速度计不可用", Toast.LENGTH_SHORT).show();
        }
//        if (stepdetecter == null) {
//            Toast.makeText(this, "步数检测器不可用", Toast.LENGTH_SHORT).show();
//        }
        //按钮UI
        startButton = findViewById(R.id.startButton);
        startButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
        endButton = findViewById(R.id.endButton);
        endButton.setBackgroundColor(Color.GRAY);
        btnViewButton = findViewById(R.id.btn_view_points);
        btnViewButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
        markLocationButton = findViewById(R.id.saveButton);
        markLocationButton.setBackgroundColor(Color.parseColor("#FF6200EE"));


        resultTextView = findViewById(R.id.resultTextView);

        // 设置按钮监听器
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isTracking) {
                    if(TrackFinished){
                        Log.d("重置", "start calls clear");
                        clearTrack();
                        TrackFinished = false;
                    }
                    startButton.setBackgroundColor(Color.GRAY);
                    btnViewButton.setBackgroundColor(Color.GRAY);
                    endButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
                    markLocationButton.setBackgroundColor(Color.GRAY);
                    detectStep = new DetectStep();
                    detectThread = new Thread(detectStep);
                    detectThread.start();
                    startTracking();

                    //欧吉罗巴大王线程开始
                    picker = new TaskPicker(getApplicationContext(),taskUpdateHandler);
                    taskPickerthread = new Thread(picker);
                    taskPickerthread.start();

                }
            }
        });
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isTracking) {
                    Log.d("结束键", "End pressed, report = " + detectStep.getReport());
                    endButton.setBackgroundColor(Color.GRAY);
                    markLocationButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
                    startButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
                    btnViewButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
                    walking_v = get_v(totalDistance - runningDistance, detectStep.get_walkTime()/1000f);
                    runniing_v = get_v(runningDistance,detectStep.get_runTime()/1000f);
                    stopTracking();
                    if (detectStep != null) detectStep.stop();
                    if (detectThread != null) {
                        try {
                            detectThread.join(); // 等待线程结束
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(picker != null) picker.stopTaskpicker();
                    if(taskPickerthread !=null){
                        try {
                            taskPickerthread.join(); // 等待线程结束
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    double score = getscore(ogiropa,totalpoints);
                    report = detectStep.getReport();
                    report += String.format("\nwalk distance：%.2f m", totalDistance - runningDistance);
                    report += String.format("\nrun distance：%.2f m", runningDistance);
                    report += String.format("\nwalk speed ：%.2f m/s", walking_v);
                    report += String.format("\nrun speed：%.2f m/s", runniing_v);
                    report += String.format("\nTotal Distance：%.2f m", totalDistance);
                    report += String.format("\nTotal Steps：%d ", (int)(totalDistance/0.7));
                    report += String.format("\nScore：%d ", (int)(score));

                    resultTextView.setText(report);
                    showReportDialog(score,report);
//                    // 写入 reportDataBuffer
//                    synchronized (reportDataBuffer) {
//                        String[] row = new String[]{
//                                getCurrentTimeString(), // 一个生成当前时间的函数
//                                String.valueOf((int)(totalDistance/0.7)),
//                                String.valueOf(detectStep.get_walkTime() / 1000f), // 转成秒
//                                String.valueOf(detectStep.get_runTime() / 1000f),
//                                String.format("%.2f", walking_v),
//                                String.format("%.2f", runniing_v),
//                                String.format("%.2f", totalDistance - runningDistance),
//                                String.format("%.2f", runningDistance),
//                                String.format("%.2f", totalDistance)
//                        };
//                        reportDataBuffer.add(row);
//                    }

                    Log.d("到达", "total="+totalpoints+" ,ogiropa="+ ogiropa + " ,score=" + score);
                    TrackFinished = true;
                }
            }
        });
        btnViewButton.setOnClickListener(v -> {
            if(!isTracking&&PointsSaved){
                Intent intent = new Intent(MainActivity.this, ViewPointsActivity.class);
                startActivity(intent);
            }else{
                Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();
            }
        });
        markLocationButton.setOnClickListener(v -> {
            if(!isTracking){
                PointsSaved = false;
                SavePointsThread = new SavePoints(MainActivity.this, bufferList);
                SavePointsThread.start();
                try{
                    SavePointsThread.join();

                    // 线程执行完后检查 isSaved 的值
                    if (SavePointsThread.isSaved()) {
                        // 保存成功
                        PointsSaved = true;
                        clearSavedData();

                        Toast.makeText(this, "Saved Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        // 保存失败
                        Toast.makeText(this, "Fail to Save", Toast.LENGTH_SHORT).show();
                    }
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }else{
                Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();
            }
        });


    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if(null != locationClient){
            locationClient.onDestroy();
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume();
        if (sensorManager != null) {
            if(accelerometer!=null) {
                sensorManager.registerListener(
                        this,
                        accelerometer,
                        SensorManager.SENSOR_DELAY_UI
                );
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }
    @Override
    public void activate(LocationSource.OnLocationChangedListener listener) {
        mListener = listener;
        //Log.d("调试", "activate 被调用");
        try{
            if(locationClient == null){
                locationClient = new AMapLocationClient(getApplicationContext());
                mLocationoption = new AMapLocationClientOption();
                locationClient.setLocationListener(this);
                mLocationoption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                locationClient.setLocationOption(mLocationoption);
                locationClient.startLocation();//启动定位
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void deactivate() {
        mListener = null;
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
        }
        locationClient = null;
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null && amapLocation.getErrorCode() == 0) {
            mListener.onLocationChanged(amapLocation);
            currentLatLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());


            if (isTracking) {
                updateTrackPolyline(currentLatLng);
                //欧吉罗巴，是否到达Marker点：
                    if(picker!=null&&picker.isPicked()&&isMarked) {
                        long now = System.currentTimeMillis();
                        if (now - lastArrivalTime >= 1000) { // 冷却 1 秒
                            LocationItem target = picker.getCurrentTask();
                            LatLng targetLatLng = new LatLng(target.getLat(), target.getLng());

                            boolean reached = checkIfReachedTarget(currentLatLng, targetLatLng);
                            if (reached) {
                                picker.isPickedsetFalse();
                                lastArrivalTime = now; // 更新打卡时间
                                Log.d("取点", "用户（平滑）已到达目标点！");
                                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                                } else {
                                    vibrator.vibrate(200);
                                }
                                playArrivalSound();
                                recentDistances.clear(); // 清空滑动窗口
//                                if(picker.isOnTime()) {
//                                    ogiropa++;
//                                    Log.d("欧吉罗巴到达","ogiropa++-->" + ogiropa);
//                                }else{
//                                    Log.d("欧吉罗巴到达","超时了。");
//                                }
                            }
                        } else {
                            Log.d("冷却中", "未判定到达，正在冷却中...");
                        }
                    }

                if(detectStep != null){
                    if(lastTotalLatLng != null){
                        float segmentTotal = AMapUtils.calculateLineDistance(lastTotalLatLng,currentLatLng);
                        totalDistance += segmentTotal;
                    }
                    lastTotalLatLng = currentLatLng;
                    if (detectStep.isJogging()) {
                        if (lastRunningLatLng != null) {
                            float segment = AMapUtils.calculateLineDistance(lastRunningLatLng, currentLatLng);
                            runningDistance += segment;
                        }
                        lastRunningLatLng = currentLatLng;
                    } else {
                        lastRunningLatLng = null; // 如果不是跑步中，清除上一个参考点
                    }
                }
            } else if (!isTracking) {
                lastTotalLatLng = null;
                lastRunningLatLng = null;
            }

        }
    }

    //轨迹控制
    private void startTracking() {
        isTracking = true;
        polylineOptions = new PolylineOptions()
                .width(10)
                .color(Color.BLUE)
                .geodesic(true); // 更自然的折线
        if (polyline != null) {
            polyline.remove(); // 清除之前的轨迹
        }
    }

    private void stopTracking() {
        isTracking = false;
    }
    private void clearTrack() {
        Log.d("重置", "clearTrack() is called");
        // 清除地图上的轨迹线
        if (polyline != null) {
            polyline.remove();
            polyline = null;
        }

        // 清空轨迹点列表
        if (polylineOptions != null) {
            polylineOptions = new PolylineOptions()
                    .width(10)
                    .color(Color.BLUE)
                    .geodesic(true); // 重建一个空的 polylineOptions
        }

        // 重置定位点
        lastTotalLatLng = currentLatLng;
        lastRunningLatLng = currentLatLng;

        // 重置距离数据
        totalDistance = 0f;
        runningDistance = 0f;

        //重制欧吉罗巴得分
        ogiropa = 0;
        totalpoints = 0;

        // 清空检测到的步行/跑步速度（如果有）
        walking_v = 0f;
        runniing_v = 0f;
        Log.d("重置", "totalDistance: "+totalDistance);
        Log.d("重置", "runningDistance:"+runningDistance);
        report = "";
        resultTextView.setText(report);

    }
    public double getDistance(double lat1, double lng1  , double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0];
    }
    private void updateTrackPolyline(LatLng currentLatLng){
        // 轨迹点筛选逻辑
        float accuracy = AMapUtils.calculateLineDistance(lastPoint, currentLatLng);
        if (accuracy <= 40f) {
            if (lastAcceptedPoint != null) {
                float distanceToLast = AMapUtils.calculateLineDistance(lastAcceptedPoint, currentLatLng);
                if (distanceToLast <= 100f) {
                    polylineOptions.add(currentLatLng);
                    if (polyline != null) {
                        polyline.setPoints(polylineOptions.getPoints());
                    } else {
                        polyline = aMap.addPolyline(polylineOptions);
                    }
                    lastAcceptedPoint = currentLatLng;
                } else {
                    Log.d("过滤", "跳点过大，忽略此次定位");
                }
            } else {
                polylineOptions.add(currentLatLng);
                lastAcceptedPoint = currentLatLng;
            }
        }
        // 无论是否接受，都更新 lastPoint
        lastPoint = currentLatLng;
    }
    private boolean checkIfReachedTarget(LatLng currentLatLng, LatLng targetLatLng) {
        // 到达判定逻辑
        float distanceToTarget = AMapUtils.calculateLineDistance(currentLatLng, targetLatLng);
        recentDistances.add(distanceToTarget);
        if (recentDistances.size() > 5) {
            recentDistances.removeFirst();
        }
        //中位数策略
        List<Float> sorted = new ArrayList<>(recentDistances);
        Collections.sort(sorted);
        float median;
        int size = sorted.size();
        if (size % 2 == 1) {
            median = sorted.get(size / 2);
        } else {
            median = (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2f;
        }

        Log.d("到达判定", "当前距目标点：" + distanceToTarget + "，中位数距离：" + median);
        //每次判定到达之后，清空中位数窗口
        if (recentDistances.size() == 5 && median < 15f) {
            recentDistances.clear(); // 清空
            isMarked = false;
            return true;
        }
        return false;
        //均值策略
//        float sum = 0f;
//        for (float d : recentDistances) {
//            sum += d;
//        }
//        float avgDistance = sum / recentDistances.size();

//        Log.d("到达判定", "当前距目标点：" + distanceToTarget + "，平均距离：" + avgDistance);

        //return (recentDistances.size() == 5 && median < 20f);
    }

    //    sensor代码区
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isTracking && detectStep != null) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                detectStep.addEvent(event);
                Log.d("传感器", "Event added to queue: " + event.sensor.getType());
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    public float get_v(float distance, float time){
        if(time == 0) return 0;
        return distance/time;
    }
    private String getCurrentTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    //数据初始化：
    private void initJsonIfNotExists() {
        File jsonFile = new File(getFilesDir(), "points.json");

        if (!jsonFile.exists()) {
            List<LocationItem> initialPoints = new ArrayList<>();

            LocationItem p1 = new LocationItem("Sports Centre", 31.41607, 120.902766);
            LocationItem p2 = new LocationItem("CCT", 31.415887, 120.900111);
            LocationItem p3 = new LocationItem("Statue", 31.414276, 120.900608);
            LocationItem p4 = new LocationItem("Garden near WDR & Lib", 31.414424, 120.899677);

            initialPoints.add(p1);
            initialPoints.add(p2);
            initialPoints.add(p3);
            initialPoints.add(p4);

            MarkerUtilsTool.writeMarkersToInternal(this, initialPoints);
            Log.d("初始化", "初始 points.json 已写入！");
        }
    }
    private void showAddLocationDialog(LatLng selectedLatLng) {
        Marker tempMarker = aMap.addMarker(new MarkerOptions()
                .position(selectedLatLng)
                .title("Temp Location")
                .snippet("To be saved after confirmed")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );
        tempMarkers.add(tempMarker);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Notes");

        final EditText input = new EditText(this);
        input.setHint("e.g：CCT,Garden beside the Library");
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String note = input.getText().toString().trim();
            if (note.isEmpty()) note = "Untitled Location";

            LocationItem item = new LocationItem(note,selectedLatLng.latitude, selectedLatLng.longitude);
            bufferList.add(item);

            tempMarker.setTitle("Mark Location");
            tempMarker.setSnippet(note);
            tempMarker.showInfoWindow();

            Toast.makeText(this, "Mark Added:" + note, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            tempMarker.remove();
            dialog.cancel();
        });

        builder.show();
        Log.d("bufferList", bufferList.toString());
    }
    public void clearSavedData() {
        // 清空临时标记
        for (Marker marker : tempMarkers) {
            marker.remove(); // 移除每个临时标记
        }
        tempMarkers.clear(); // 清空临时标记列表

        // 清空 bufferList
        bufferList.clear();
    }

    //欧吉罗巴大王驾到！
    Handler taskUpdateHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.d("取点", "handleMessage runs");
            if (msg.what == 1) {
                Log.d("取点", "msg.what == 1");
                LocationItem picked = (LocationItem) msg.obj;
                LatLng latLng = new LatLng(picked.getLat(), picked.getLng());

                if (currentTargetMarker != null) {
                    currentTargetMarker.remove();
                }

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(picked.getNote())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

                currentTargetMarker = aMap.addMarker(markerOptions);
                isMarked = true;
                totalpoints++;
                //aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));//5.1日测试，经纬度搞反，定位飞到非洲的有趣故事
                Log.d("到达", "Marker added");
            }
        }
    };
    private double getscore(int ogiropa, int totalpoints){
        if(totalpoints==0||ogiropa==0) return 0.0;
        else {
            ogiropa++;
            if(totalpoints<=ogiropa) {
                return 100.0;
            }
            else {
                return (ogiropa * 100.0 / totalpoints);
            }
        }
    };
    private String getAssessment(double score){
        if(score>90){
            return "Unbelievable!";
        }else if(score>60){
            return "Great Job！";
        }else{
            return "Keep pushing!";
        }
    }
    private void showReportDialog(double score, String reportContent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 设置标题为 getAssessment 返回的评语
        String assessment = getAssessment(score);
        builder.setTitle(assessment);

        // 创建一个 TextView 显示内容
        final TextView reportTextView = new TextView(this);
        reportTextView.setText(reportContent);
        reportTextView.setTextSize(16); // 字号稍微大一点
        reportTextView.setPadding(50, 40, 50, 40);
        reportTextView.setMovementMethod(new ScrollingMovementMethod()); // 如果内容太长可以滚动

        builder.setView(reportTextView);

        // 设置按钮
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            Toast.makeText(this, "For Ogiropa!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }
    private void playArrivalSound() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(
                focusChange -> {},  // 可忽略焦点变化
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        );

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.arrival);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e("Sound", "播放失败：" + what + ", " + extra);
                    return false;
                });
                mediaPlayer.start();
            } else {
                Log.e("Sound", "MediaPlayer.create 返回 null");
            }
        } else {
            Log.e("Sound", "Audio focus 请求失败");
        }
//        Log.d("音效","playArrivalSound() is called");
//        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.arrival);
//        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
//        mediaPlayer.start();
    }



}