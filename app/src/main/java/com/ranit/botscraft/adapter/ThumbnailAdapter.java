package com.ranit.botscraft.adapter;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;

import java.util.List;

public class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private final List<String> imageUrls;
    private final OnItemClickListener listener;
    private int selectedPosition = 0;

    public ThumbnailAdapter(List<String> imageUrls, OnItemClickListener listener) {
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPos);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thumbnail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String data = imageUrls.get(position);
        
        if (data.startsWith("http")) {
            Glide.with(holder.itemView.getContext()).load(data).centerCrop().into(holder.imgThumbnail);
        } else {
            try {
                String cleanBase64 = data.contains(",") ? data.split(",")[1] : data;
                byte[] bytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                Glide.with(holder.itemView.getContext()).load(bytes).centerCrop().into(holder.imgThumbnail);
            } catch (Exception e) {
                holder.imgThumbnail.setImageResource(R.drawable.ic_launcher_background);
            }
        }

        if (position == selectedPosition) {
            holder.selectionBorder.setVisibility(View.VISIBLE);
            holder.selectionOverlay.setVisibility(View.GONE);
            holder.tvIndex.setVisibility(View.VISIBLE);
            holder.tvIndex.setText(String.valueOf(position + 1));
        } else {
            holder.selectionBorder.setVisibility(View.GONE);
            holder.selectionOverlay.setVisibility(View.VISIBLE);
            holder.tvIndex.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        View selectionOverlay, selectionBorder;
        TextView tvIndex;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
            selectionBorder = itemView.findViewById(R.id.selectionBorder);
            tvIndex = itemView.findViewById(R.id.tvIndex);
        }
    }
}
