package com.ranit.botscraft.ui;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ranit.botscraft.R;
import com.ranit.botscraft.util.ImageDataHolder;

import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends AppCompatActivity {

    private static final String TAG = "ImageViewerActivity";
    private ImageView imgMain;
    private RecyclerView rvThumbs;
    private TextView tvCounter;
    private List<String> imageUrls = new ArrayList<>();
    private int selectedPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        try {
            // Use DataHolder to get images
            List<String> heldImages = ImageDataHolder.getImages();
            
            if (heldImages == null || heldImages.isEmpty()) {
                Log.e(TAG, "No images found in DataHolder");
                finish();
                return;
            }

            // Copy to local list to survive small lifecycle events without immediate clearing
            imageUrls.addAll(heldImages);

            imgMain = findViewById(R.id.imgMain);
            rvThumbs = findViewById(R.id.rvThumbs);
            tvCounter = findViewById(R.id.tvCounter);
            
            View btnBack = findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            setupMainImage(0);
            setupRecyclerView();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            finish();
        }
    }

    private void setupMainImage(int position) {
        if (imageUrls == null || imageUrls.isEmpty() || position >= imageUrls.size()) return;
        
        selectedPosition = position;
        tvCounter.setText((position + 1) + " / " + imageUrls.size());
        String data = imageUrls.get(position);

        try {
            if (data.startsWith("http") || data.startsWith("https")) {
                Glide.with(this).load(data).into(imgMain);
            } else {
                // Handle Base64 (data:image/png;base64,...)
                String cleanBase64 = data;
                if (data.contains(",")) {
                    cleanBase64 = data.split(",")[1];
                }
                byte[] bytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                Glide.with(this).load(bytes).into(imgMain);
            }
        } catch (Exception e) {
            Log.e(TAG, "Glide error loading main image", e);
            imgMain.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    private void setupRecyclerView() {
        ThumbAdapter adapter = new ThumbAdapter(imageUrls);
        rvThumbs.setAdapter(adapter);
        rvThumbs.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
    }

    private class ThumbAdapter extends RecyclerView.Adapter<ThumbAdapter.VH> {
        private final List<String> list;

        ThumbAdapter(List<String> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thumbnail, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String data = list.get(position);
            
            try {
                if (data.startsWith("http") || data.startsWith("https")) {
                    Glide.with(holder.img.getContext()).load(data).centerCrop().into(holder.img);
                } else {
                    String cleanBase64 = data;
                    if (data.contains(",")) {
                        cleanBase64 = data.split(",")[1];
                    }
                    byte[] bytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                    Glide.with(holder.img.getContext()).load(bytes).centerCrop().into(holder.img);
                }
            } catch (Exception e) {
                holder.img.setImageResource(R.drawable.ic_launcher_background);
            }

            holder.border.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);
            holder.overlay.setVisibility(position == selectedPosition ? View.GONE : View.VISIBLE);
            holder.txt.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);
            holder.txt.setText(String.valueOf(position + 1));

            holder.itemView.setOnClickListener(v -> {
                int old = selectedPosition;
                setupMainImage(position);
                notifyItemChanged(old);
                notifyItemChanged(selectedPosition);
                rvThumbs.smoothScrollToPosition(position);
            });
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView img;
            View border, overlay;
            TextView txt;

            VH(View v) {
                super(v);
                img = v.findViewById(R.id.imgThumbnail);
                border = v.findViewById(R.id.selectionBorder);
                overlay = v.findViewById(R.id.selectionOverlay);
                txt = v.findViewById(R.id.tvIndex);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clear data holder only when finishing to prevent data loss on rotation
        if (isFinishing()) {
            ImageDataHolder.setImages(null);
        }
    }
}
