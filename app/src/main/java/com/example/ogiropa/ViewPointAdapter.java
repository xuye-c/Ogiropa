package com.example.ogiropa;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ViewPointAdapter extends RecyclerView.Adapter<ViewPointAdapter.ViewHolder> {

    private List<ViewPoint> pointList;
    private Context context;
    private OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDelete(ViewPoint point);
    }

    public ViewPointAdapter(Context context, List<ViewPoint> points, OnDeleteClickListener listener) {
        this.context = context;
        this.pointList = points;
        this.deleteClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView noteText, coordText;
        Button deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            noteText = itemView.findViewById(R.id.noteText);
            coordText = itemView.findViewById(R.id.coordText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_view_point, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ViewPoint point = pointList.get(position);
        holder.noteText.setText(point.note);
        holder.coordText.setText("(" + point.lat + ", " + point.lng + ")");

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDelete(point);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pointList.size();
    }

    public void updateData(List<ViewPoint> newData) {
        this.pointList = newData;
        notifyDataSetChanged();
    }
}

