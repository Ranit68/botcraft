package com.ranit.botscraft.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";

    public interface OnChatInteractionListener {
        void onVoiceClick(ChatMessage message);
        void onImageClick(String imageUrl);
        void onUnlockImageClick(ChatMessage message, int position);
    }

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;
    private static final int VIEW_TYPE_TYPING = 2;
    private static final int VIEW_TYPE_GENERATING = 3;

    @NonNull
    private final List<ChatMessage> list;
    @Nullable
    private final OnChatInteractionListener listener;
    
    private boolean isTyping = false;
    private boolean isGenerating = false;
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
        notifyDataSetChanged();
    }

    public void setGenerating(boolean generating) {
        if (this.isGenerating == generating) return;
        this.isGenerating = generating;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == list.size()) {
            if (isGenerating) return VIEW_TYPE_GENERATING;
            if (isTyping) return VIEW_TYPE_TYPING;
        }
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
        if (viewType == VIEW_TYPE_GENERATING) {
            return new MessageViewHolder(inflater.inflate(R.layout.item_chat_bot, parent, false));
        }
        int layout = (viewType == VIEW_TYPE_USER) ? R.layout.item_chat_user : R.layout.item_chat_bot;
        return new MessageViewHolder(inflater.inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MessageViewHolder) {
            if (getItemViewType(position) == VIEW_TYPE_GENERATING) {
                bindGenerating((MessageViewHolder) holder);
                return;
            }
            ChatMessage msg = list.get(position);
            bindMessage((MessageViewHolder) holder, msg, position);
        }
    }

    private void bindGenerating(@NonNull MessageViewHolder holder) {
        if (holder.tvTime != null) holder.tvTime.setVisibility(View.GONE);
        if (holder.ivContentImage != null) holder.ivContentImage.setVisibility(View.GONE);
        if (holder.llImageLockOverlay != null) holder.llImageLockOverlay.setVisibility(View.GONE);
        holder.message.setVisibility(View.VISIBLE);
        
        // Randomize the "real person" feel messages
        String[] thoughts = {
            "*Wait, let me take a quick selfie for you...*",
            "*Hold on, finding the perfect angle...*",
            "*Just a second, checking how I look...*",
            "*Capturing this moment for you...*",
            "*Smile! Taking a photo now...*"
        };
        String randomThought = thoughts[(int) (System.currentTimeMillis() % thoughts.length)];
        holder.message.setText(formatBehaviorText(randomThought));
    }

    private void bindMessage(@NonNull MessageViewHolder holder, @NonNull ChatMessage msg, int position) {
        // Set Time
        long timestamp = msg.timestamp > 0 ? msg.timestamp : System.currentTimeMillis();
        if (holder.tvTime != null) {
            holder.tvTime.setText(timeFormat.format(new Date(timestamp)));
        }

        // Image or Text
        if (msg.imageUrl != null && !msg.imageUrl.isEmpty()) {
            handleImageMessage(holder, msg, position);
        } else {
            handleTextMessage(holder, msg, position);
        }
        
        // Voice Speaker Integration
        if (holder.ivSpeak != null) {
            holder.ivSpeak.setVisibility(View.GONE);
        }
    }

    private void handleImageMessage(@NonNull MessageViewHolder holder, @NonNull ChatMessage msg, int position) {
        if (holder.ivContentImage != null) {
            holder.ivContentImage.setVisibility(View.VISIBLE);
            
            if (msg.isLocked) {
                if (holder.llImageLockOverlay != null) holder.llImageLockOverlay.setVisibility(View.VISIBLE);
                // Load blurred image or just use placeholder
                Glide.with(holder.itemView.getContext())
                        .load(msg.imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.bg_avatar_glow)
                        .error(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .override(50, 50) // Small size for blur effect
                        .into(holder.ivContentImage);
                
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onUnlockImageClick(msg, position);
                });
            } else {
                if (holder.llImageLockOverlay != null) holder.llImageLockOverlay.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext())
                        .load(msg.imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.bg_avatar_glow)
                        .into(holder.ivContentImage);
                
                holder.ivContentImage.setOnClickListener(v -> {
                    if (listener != null) listener.onImageClick(msg.imageUrl);
                });
            }
        }
        
        String cleanText = cleanMessageText(msg.text);
        if (cleanText != null && !cleanText.isEmpty()) {
            holder.message.setVisibility(View.VISIBLE);
            holder.message.setText(formatBehaviorText(cleanText));
        } else {
            holder.message.setVisibility(View.GONE);
        }
    }

    private void handleTextMessage(@NonNull MessageViewHolder holder, @NonNull ChatMessage msg, int position) {
        if (holder.ivContentImage != null) holder.ivContentImage.setVisibility(View.GONE);
        holder.message.setVisibility(View.VISIBLE);
        
        String content = cleanMessageText(msg.text != null ? msg.text : "");
        
        if (getItemViewType(position) == VIEW_TYPE_BOT && 
            position == list.size() - 1 && 
            shouldAnimateNext && 
            !animatedPositions.contains(position)) {
            
            shouldAnimateNext = false;
            animatedPositions.add(position);
            // Animation for behavior text is complex with Spans, 
            // so we set it directly if it contains behaviors or keep simple animation if not.
            if (content.contains("*")) {
                holder.message.setText(formatBehaviorText(content));
            } else {
                animateText(holder.message, content);
            }
        } else {
            holder.message.setText(formatBehaviorText(content));
        }
    }

    private CharSequence formatBehaviorText(String text) {
        if (text == null) return "";
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        Pattern pattern = Pattern.compile("\\*([^*]+)\\*");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            // Dark gray color and italic for text between stars
            ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#666666")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ssb;
    }

    /**
     * Powerful cleaner that removes all internal AI tags and roleplay image descriptions.
     * Removes: [GEN_IMAGE: ...], [Image: ...], (Imagine: ...), *Imagine: ...*, [Photo: ...], etc.
     */
    private String cleanMessageText(String text) {
        if (text == null) return null;
        // Aggressively remove AI tags like [GEN_IMAGE: ...], [Image: ...], (Imagine: ...), etc.
        return text.replaceAll("(?i)(?:\\[|\\(|\\*|\\{)?\\s*(?:GEN_IMAGE|Image|Imagine|Photo|Prompt):?\\s*([^\\)\\]\\*\\}]+)(?:\\]|\\)|\\*|\\})?", "").trim();
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
                    handler.postDelayed(this, 10);
                }
            }
        };
        handler.post(runnable);
    }

    @Override
    public int getItemCount() {
        return list.size() + (isTyping || isGenerating ? 1 : 0);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView message;
        @Nullable TextView tvTime;
        @Nullable ImageView ivContentImage;
        @Nullable ImageView ivSpeak;
        @Nullable View llImageLockOverlay;

        MessageViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivContentImage = itemView.findViewById(R.id.ivContentImage);
            ivSpeak = itemView.findViewById(R.id.ivSpeak);
            llImageLockOverlay = itemView.findViewById(R.id.llImageLockOverlay);
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(View itemView) {
            super(itemView);
        }
    }
}
