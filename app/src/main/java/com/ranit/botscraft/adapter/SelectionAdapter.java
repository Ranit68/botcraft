package com.ranit.botscraft.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ranit.botscraft.R;
import com.ranit.botscraft.model.SelectionItem;

import java.util.List;

public class SelectionAdapter extends RecyclerView.Adapter<SelectionAdapter.ViewHolder> {

    private final List<SelectionItem> items;
    private final int layoutId;
    private final OnItemClickListener listener;
    private int selectedPosition = 0;

    public interface OnItemClickListener {
        void onItemClick(SelectionItem item);
    }

    public SelectionAdapter(List<SelectionItem> items, int layoutId, OnItemClickListener listener) {
        this.items = items;
        this.layoutId = layoutId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelectionItem item = items.get(position);
        holder.bind(item, position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && currentPos != selectedPosition) {
                int oldPos = selectedPosition;
                selectedPosition = currentPos;
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView label;
        private final View colorView;
        private final View selectedDot;
        private final ImageView imgIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            TextView tv = itemView.findViewById(R.id.tvColorName);
            if (tv == null) tv = itemView.findViewById(R.id.tvStyleName);
            if (tv == null) tv = itemView.findViewById(R.id.tvModelName);
            if (tv == null) tv = itemView.findViewById(R.id.tvTypeName);
            if (tv == null) tv = itemView.findViewById(R.id.tvLabel);
            label = tv;

            View cv = itemView.findViewById(R.id.viewColor);
            if (cv == null) cv = itemView.findViewById(R.id.viewSkinTone);
            colorView = cv;

            View sd = itemView.findViewById(R.id.imgSelected);
            if (sd == null) sd = itemView.findViewById(R.id.viewSelected);
            selectedDot = sd;

            imgIcon = itemView.findViewById(R.id.imgIcon);
        }

        void bind(SelectionItem item, boolean isSelected) {
            if (label != null) {
                label.setText(item.label);
            }

            if (colorView != null && item.colorHex != null) {
                colorView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(item.colorHex)));
            }

            if (imgIcon != null && item.imageRes != -1) {
                imgIcon.setImageResource(item.imageRes);
            }

            if (layoutId == R.layout.item_hair_color) {
                itemView.setBackgroundResource(isSelected ? R.drawable.bg_selection_border : 0);
            } else if (layoutId == R.layout.item_hair_style || layoutId == R.layout.item_model_type || layoutId == R.layout.item_body_type) {
                itemView.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
                if (label != null) {
                    label.setTextColor(isSelected ? Color.WHITE : ContextCompat.getColor(itemView.getContext(), R.color.text_label));
                }
            } else if (layoutId == R.layout.item_skin_tone) {
                if (selectedDot != null) {
                    selectedDot.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                }
                itemView.setBackgroundResource(isSelected ? R.drawable.bg_selection_border : 0);
            } else if (layoutId == R.layout.item_personality) {
                itemView.setBackgroundResource(isSelected ? R.drawable.bg_gender_selected : R.drawable.bg_gender_unselected);
                if (selectedDot != null) selectedDot.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                if (imgIcon != null) {
                    imgIcon.setImageTintList(ColorStateList.valueOf(isSelected ? 
                        ContextCompat.getColor(itemView.getContext(), R.color.accent_purple) : 
                        ContextCompat.getColor(itemView.getContext(), R.color.text_hint)));
                }
            }
        }
    }
}
