package com.ranit.botscraft.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.model.Bot;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyBotsAdapter extends RecyclerView.Adapter<MyBotsAdapter.ViewHolder> {

    private final List<Bot> bots;
    private final OnBotClickListener listener;

    public interface OnBotClickListener {
        void onBotClick(Bot bot);
    }

    public MyBotsAdapter(List<Bot> bots, OnBotClickListener listener) {
        this.bots = bots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_bot_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bot bot = bots.get(position);
        holder.tvName.setText(bot.getDisplayName());
        
        if (bot.imageUrl != null && !bot.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(bot.imageUrl)
                    .placeholder(R.drawable.ic_bot_profile)
                    .into(holder.imgBot);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                bot.sanitizeForIntent(); // 🔥 PREVENT CRASH
                listener.onBotClick(bot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgBot;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBot = itemView.findViewById(R.id.imgMyBot);
            tvName = itemView.findViewById(R.id.tvMyBotName);
        }
    }
}
