package com.ranit.botscraft.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.ranit.botscraft.R;
import com.ranit.botscraft.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";

    public interface OnChatInteractionListener {
        void onVoiceClick(ChatMessage message);
        void onImageClick(String imageUrl);
    }

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;
    private static final int VIEW_TYPE_TYPING = 2;

    @NonNull
    private final List<ChatMessage> list;
    @Nullable
    private final OnChatInteractionListener listener;
    
    private boolean isTyping = false;
    private boolean shouldAnimateNext = false; 
    private final Set<Integer> animatedPositions = new HashSet<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter(@NonNull List<ChatMessage> sharedList, @Nullable OnChatInteractionListener listener) {
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
        ChatMessage msg = list.get(position);
        return "user".equalsIgnoreCase(msg.role) ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_TYPING) {
            return new TypingViewHolder(inflater.inflate(R.layout.item_chat_typing, parent, false));
        }
        int layout = (viewType == VIEW_TYPE_USER) ? R.layout.item_chat_user : R.layout.item_chat_bot;
        return new MessageViewHolder(inflater.inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MessageViewHolder) {
            ChatMessage msg = list.get(position);
            bindMessage((MessageViewHolder) holder, msg, position);
        }
    }

    private void bindMessage(@NonNull MessageViewHolder holder, @NonNull ChatMessage msg, int position) {
        // Set Time
        long timestamp = msg.timestamp > 0 ? msg.timestamp : System.currentTimeMillis();
        if (holder.tvTime != null) {
            holder.tvTime.setText(timeFormat.format(new Date(timestamp)));
        }

        // Image or Text
        if (msg.imageUrl != null && !msg.imageUrl.isEmpty()) {
            handleImageMessage(holder, msg);
        } else {
            handleTextMessage(holder, msg, position);
        }
    }

    private void handleImageMessage(@NonNull MessageViewHolder holder, @NonNull ChatMessage msg) {
        holder.ivContentImage.setVisibility(View.VISIBLE);
        holder.message.setVisibility(msg.text != null && !msg.text.isEmpty() ? View.VISIBLE : View.GONE);
        holder.message.setText(msg.text != null ? msg.text : "");

        if (msg.imageUrl.startsWith("data:image")) {
            loadBase64Image(holder.ivContentImage, msg.imageUrl);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(msg.imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_launcher_background) // Consider a proper placeholder
                    .error(android.R.drawable.stat_notify_error)
                    .into(holder.ivContentImage);
        }

        holder.ivContentImage.setOnClickListener(v -> {
            if (listener != null) listener.onImageClick(msg.imageUrl);
        });
    }

    private void loadBase64Image(ImageView imageView, String b64String) {
        try {
            String pureBase64 = b64String.substring(b64String.lastIndexOf(",") + 1);
            byte[] decoded = Base64.decode(pureBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                Glide.with(imageView.getContext()).load(decoded).into(imageView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode base64 image", e);
        }
    }

    private void handleTextMessage(@NonNull MessageViewHolder holder, @NonNull ChatMessage msg, int position) {
        if (holder.ivContentImage != null) holder.ivContentImage.setVisibility(View.GONE);
        holder.message.setVisibility(View.VISIBLE);
        
        String content = msg.text != null ? msg.text : "";
        
        // SaaS Polish: Animate bot replies only once
        if (getItemViewType(position) == VIEW_TYPE_BOT && 
            position == list.size() - 1 && 
            shouldAnimateNext && 
            !animatedPositions.contains(position)) {
            
            shouldAnimateNext = false;
            animatedPositions.add(position);
            animateText(holder.message, content);
        } else {
            holder.message.setText(content);
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
                    handler.postDelayed(this, 10); // Faster, smoother animation for SaaS feel
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
        TextView message;
        @Nullable TextView tvTime;
        @Nullable ImageView ivContentImage;

        MessageViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivContentImage = itemView.findViewById(R.id.ivContentImage);
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(View itemView) {
            super(itemView);
        }
    }
}
