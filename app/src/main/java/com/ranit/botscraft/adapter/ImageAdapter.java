package com.ranit.botscraft.adapter;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final List<String> imageUrls;

    public ImageAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String data = imageUrls.get(position);
        
        if (data.startsWith("http")) {
            Glide.with(holder.itemView.getContext())
                    .load(data)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imageView);
        } else {
            // Assume Base64
            try {
                String cleanBase64 = data;
                if (data.contains(",")) {
                    cleanBase64 = data.split(",")[1];
                }
                byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
                Glide.with(holder.itemView.getContext())
                        .load(decodedString)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(holder.imageView);
            } catch (Exception e) {
                holder.imageView.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
