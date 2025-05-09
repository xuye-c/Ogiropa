package com.example.ogiropa;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ViewPointsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ViewPointAdapter adapter;
    private List<ViewPoint> pointList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_points);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        pointList = readPointsFromFile();

        adapter = new ViewPointAdapter(this, pointList, point -> {
            pointList.remove(point);
            savePointsToFile(pointList);
            adapter.updateData(pointList);
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);
    }

    private List<ViewPoint> readPointsFromFile() {
        File file = new File(getFilesDir(), "points.json");
        if (!file.exists()) return new ArrayList<>();

        try {
            FileReader reader = new FileReader(file);
            Type type = new TypeToken<List<ViewPoint>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void savePointsToFile(List<ViewPoint> points) {
        File file = new File(getFilesDir(), "points.json");
        try {
            FileWriter writer = new FileWriter(file);
            new Gson().toJson(points, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}