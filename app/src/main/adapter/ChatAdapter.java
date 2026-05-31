package com.ranit.botscraft.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.model.ChatMessage;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnChatInteractionListener {
        void onVoiceClick(ChatMessage message);
        void onImageClick(String imageUrl);
    }

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;
    private static final int VIEW_TYPE_TYPING = 2;

    private final List<ChatMessage> list;
    private final OnChatInteractionListener listener;
    private boolean isTyping = false;
    private boolean shouldAnimateNext = false; 
    private final Set<Integer> animatedPositions = new HashSet<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter(List<ChatMessage> sharedList, OnChatInteractionListener listener) {
        this.list = sharedList;
        this.listener = listener;
    }

    public void setTyping(boolean typing) {
        if (this.isTyping == typing) return;
        this.isTyping = typing;
        if (typing) {
            shouldAnimateNext = true; 
            notifyItemInserted(list.size());
        } else {
            notifyItemRemoved(list.size());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isTyping && position == list.size()) return VIEW_TYPE_TYPING;
        return "user".equals(list.get(position).role) ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_TYPING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_typing, parent, false);
            return new TypingViewHolder(view);
        }
        int layout = (viewType == VIEW_TYPE_USER) ? R.layout.item_chat_user : R.layout.item_chat_bot;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MessageViewHolder) {
            ChatMessage msg = list.get(position);
            MessageViewHolder msgHolder = (MessageViewHolder) holder;
            
            String timeStr = timeFormat.format(new Date(msg.timestamp > 0 ? msg.timestamp : System.currentTimeMillis()));
            if (msgHolder.tvTime != null) msgHolder.tvTime.setText(timeStr);

            if (getItemViewType(position) == VIEW_TYPE_BOT) {
                if (msgHolder.btnVoice != null) {
                    msgHolder.btnVoice.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onVoiceClick(msg);
                        }
                    });
                }

                if (msg.imageUrl != null && !msg.imageUrl.isEmpty()) {
                    msgHolder.ivContentImage.setVisibility(View.VISIBLE);
                    msgHolder.message.setVisibility(msg.text != null && !msg.text.isEmpty() ? View.VISIBLE : View.GONE);
                    Glide.with(msgHolder.itemView.getContext()).load(msg.imageUrl).into(msgHolder.ivContentImage);
                    msgHolder.ivContentImage.setOnClickListener(v -> {
                        if (listener != null) listener.onImageClick(msg.imageUrl);
                    });
                    msgHolder.message.setText(msg.text != null ? msg.text : "");
                } else {
                    msgHolder.ivContentImage.setVisibility(View.GONE);
                    msgHolder.message.setVisibility(View.VISIBLE);
                    if (position == list.size() - 1 && shouldAnimateNext && !animatedPositions.contains(position)) {
                        shouldAnimateNext = false;
                        animatedPositions.add(position);
                        animateText(msgHolder.message, msg.text != null ? msg.text : "");
                    } else {
                        msgHolder.message.setText(msg.text != null ? msg.text : "");
                    }
                }
            } else {
                msgHolder.message.setText(msg.text != null ? msg.text : "");
            }
        }
    }

    private void animateText(TextView textView, String text) {
        final int[] index = {0};
        Handler handler = new Handler(Looper.getMainLooper());
        textView.setText("");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < text.length()) {
                    textView.append(String.valueOf(text.charAt(index[0]++)));
                    handler.postDelayed(this, 15);
                }
            }
        };
        handler.post(runnable);
    }

    @Override
    public int getItemCount() {
        return list.size() + (isTyping ? 1 : 0);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView message, tvTime;
        ImageView btnVoice, ivContentImage;
        MessageViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnVoice = itemView.findViewById(R.id.btnVoice);
            ivContentImage = itemView.findViewById(R.id.ivContentImage);
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(View itemView) {
            super(itemView);
        }
    }
}
