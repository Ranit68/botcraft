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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class BlockedBotAdapter extends RecyclerView.Adapter<BlockedBotAdapter.ViewHolder> {

    private final List<Bot> blockedBots;
    private final OnUnblockClickListener listener;

    public interface OnUnblockClickListener {
        void onUnblockClick(Bot bot);
    }

    public BlockedBotAdapter(List<Bot> blockedBots, OnUnblockClickListener listener) {
        this.blockedBots = blockedBots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blocked_bot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bot bot = blockedBots.get(position);
        holder.tvName.setText(bot.name);
        
        if (bot.imageUrl != null && !bot.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(bot.imageUrl).circleCrop().into(holder.imgBot);
        } else {
            holder.imgBot.setImageResource(R.drawable.ic_bot_profile);
        }

        holder.btnUnblock.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUnblockClick(bot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return blockedBots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgBot;
        TextView tvName;
        MaterialButton btnUnblock;

        ViewHolder(View itemView) {
            super(itemView);
            imgBot = itemView.findViewById(R.id.imgBlockedBot);
            tvName = itemView.findViewById(R.id.tvBlockedBotName);
            btnUnblock = itemView.findViewById(R.id.btnUnblock);
        }
    }
}
