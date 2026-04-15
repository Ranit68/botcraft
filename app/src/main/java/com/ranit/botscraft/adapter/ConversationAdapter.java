package com.ranit.botscraft.adapter;

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
import com.ranit.botscraft.model.Conversation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onChatClick(Bot bot);
        void onProfileClick(Bot bot);
    }

    private final List<Conversation> conversations;
    private final OnConversationClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conv = conversations.get(position);
        Bot bot = conv.bot;

        holder.tvName.setText(bot.getDisplayName());
        holder.tvLastMessage.setText(conv.lastMessage);
        
        // Format time
        long now = System.currentTimeMillis();
        long diff = now - conv.lastTimestamp;
        if (diff < 24 * 60 * 60 * 1000) {
            holder.tvTime.setText(timeFormat.format(new Date(conv.lastTimestamp)));
        } else {
            holder.tvTime.setText(dateFormat.format(new Date(conv.lastTimestamp)));
        }

        if (bot.imageUrl != null && !bot.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(bot.imageUrl)
                    .circleCrop()
                    .into(holder.imgBot);
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(bot));
        holder.imgBot.setOnClickListener(v -> listener.onProfileClick(bot));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBot;
        TextView tvName, tvLastMessage, tvTime;

        ViewHolder(View v) {
            super(v);
            imgBot = v.findViewById(R.id.imgBot);
            tvName = v.findViewById(R.id.tvName);
            tvLastMessage = v.findViewById(R.id.tvLastMessage);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}
