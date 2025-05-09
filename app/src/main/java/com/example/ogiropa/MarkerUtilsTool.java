package com.example.ogiropa;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MarkerUtilsTool {
    public static List<LocationItem> readMarkersFromRaw(Context context, int rawResId) {
        InputStream inputStream = context.getResources().openRawResource(rawResId);
        InputStreamReader reader = new InputStreamReader(inputStream);

        Gson gson = new Gson();
        Type listType = new TypeToken<List<LocationItem>>() {}.getType();
        return gson.fromJson(reader, listType);
    }
    public static List<LocationItem> readMarkersFromInternal(Context context) {
        File file = new File(context.getFilesDir(), "points.json");
        if (!file.exists()) return new ArrayList<>();

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<LocationItem>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    public static void writeMarkersToInternal(Context context, List<LocationItem> markers) {
        File file = new File(context.getFilesDir(), "points.json");
        if (file.exists()) {
            try {
                // 先读取现有的数据
                FileReader fileReader = new FileReader(file);
                Gson gson = new Gson();
                Type listType = new TypeToken<List<LocationItem>>() {}.getType();
                List<LocationItem> existingMarkers = gson.fromJson(fileReader, listType);
                fileReader.close();

                // 将新的数据添加到现有数据中
                existingMarkers.addAll(markers);

                // 将合并后的数据写回文件
                FileWriter writer = new FileWriter(file);
                gson.toJson(existingMarkers, writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // 如果文件不存在，直接写入新的数据
            try (FileWriter writer = new FileWriter(file)) {
                Gson gson = new Gson();
                gson.toJson(markers, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        try (FileWriter writer = new FileWriter(file)) {
//            Gson gson = new Gson();
//            gson.toJson(markers, writer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static LocationItem getRandomMarker(List<LocationItem> list) {
        if (list == null || list.isEmpty()) return null;
        Random random = new Random();
        return list.get(random.nextInt(list.size()));
    }
}
