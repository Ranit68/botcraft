package com.ranit.botscraft.adapter;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ranit.botscraft.R;
import com.ranit.botscraft.network.VoiceListResponse;

import java.util.List;

public class VoiceAdapter extends RecyclerView.Adapter<VoiceAdapter.ViewHolder> {

    private final List<VoiceListResponse.VoiceItem> voices;
    private final OnVoiceSelectedListener listener;
    private MediaPlayer mediaPlayer;
    private int selectedPosition = -1;

    public interface OnVoiceSelectedListener {
        void onVoiceSelected(VoiceListResponse.VoiceItem voice);
    }

    public VoiceAdapter(List<VoiceListResponse.VoiceItem> voices, OnVoiceSelectedListener listener) {
        this.voices = voices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voice_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VoiceListResponse.VoiceItem voice = voices.get(position);
        holder.tvName.setText(voice.name);
        
        if (voice.labels != null) {
            String details = (voice.labels.gender != null ? voice.labels.gender : "") + " | " + 
                             (voice.labels.accent != null ? voice.labels.accent : "");
            holder.tvDetails.setText(details);
        }

        holder.itemView.setSelected(selectedPosition == position);

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onVoiceSelected(voice);
        });

        holder.btnPreview.setOnClickListener(v -> playPreview(holder.itemView.getContext(), voice.preview_url));
    }

    private void playPreview(Context context, String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(context, "No preview available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            Toast.makeText(context, "Playing preview...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Preview failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return voices.size();
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails;
        ImageView btnPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvVoiceName);
            tvDetails = itemView.findViewById(R.id.tvVoiceDetails);
            btnPreview = itemView.findViewById(R.id.btnVoicePreview);
        }
    }
}
