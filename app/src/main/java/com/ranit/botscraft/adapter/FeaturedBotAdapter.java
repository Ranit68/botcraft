package com.ranit.botscraft.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.ui.BotProfileActivity;
import com.ranit.botscraft.ui.ChatActivity;

import java.util.List;

public class FeaturedBotAdapter extends RecyclerView.Adapter<FeaturedBotAdapter.ViewHolder> {

    private List<Bot> bots;
    private final OnBotClickListener listener;

    public FeaturedBotAdapter(List<Bot> bots, OnBotClickListener listener) {
        this.bots = bots;
        this.listener = listener;
    }

    public void updateData(List<Bot> newBots) {
        this.bots = newBots;
        notifyDataSetChanged();
    }

    public interface OnBotClickListener {
        void onChatClick(Bot bot);
        void onProfileClick(Bot bot);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_bot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bot bot = bots.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(bot.getDisplayName());
        holder.tvDesc.setText(bot.getDisplayDescription());
        
        if (bot.imageUrl != null && !bot.imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(bot.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgBot);
        }

        // Image click -> Chat
        holder.imgBot.setOnClickListener(v -> {
            if (listener != null) listener.onChatClick(bot);
        });

        // Name click -> Profile
        holder.tvName.setOnClickListener(v -> {
            if (listener != null) listener.onProfileClick(bot);
        });
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChatClick(bot);
        });
    }

    @Override
    public int getItemCount() {
        return bots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBot;
        TextView tvName, tvDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBot = itemView.findViewById(R.id.imgBot);
            tvName = itemView.findViewById(R.id.tvName);
            tvDesc = itemView.findViewById(R.id.tvDesc);
        }
    }
}
