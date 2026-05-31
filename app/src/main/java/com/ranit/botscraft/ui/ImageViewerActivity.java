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
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.ranit.botscraft.R;
import com.ranit.botscraft.util.ImageDataHolder;

import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends AppCompatActivity {

    private static final String TAG = "ImageViewerActivity";
    private ViewPager2 viewPager;
    private RecyclerView rvThumbs;
    private TextView tvCounter;
    private List<String> imageUrls = new ArrayList<>();
    private int selectedPosition = 0;
    private ThumbAdapter thumbAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        try {
            List<String> heldImages = ImageDataHolder.getImages();
            if (heldImages == null || heldImages.isEmpty()) {
                finish();
                return;
            }

            imageUrls.addAll(heldImages);

            viewPager = findViewById(R.id.viewPager);
            rvThumbs = findViewById(R.id.rvThumbs);
            tvCounter = findViewById(R.id.tvCounter);
            
            View btnBack = findViewById(R.id.btnBack);
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());

            setupViewPager();
            setupRecyclerView();
            
            updateCounter(0);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            finish();
        }
    }

    private void setupViewPager() {
        ImagePagerAdapter adapter = new ImagePagerAdapter(imageUrls);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int old = selectedPosition;
                selectedPosition = position;
                updateCounter(position);
                
                if (thumbAdapter != null) {
                    thumbAdapter.notifyItemChanged(old);
                    thumbAdapter.notifyItemChanged(selectedPosition);
                    rvThumbs.smoothScrollToPosition(position);
                }
            }
        });
    }

    private void setupRecyclerView() {
        thumbAdapter = new ThumbAdapter(imageUrls);
        rvThumbs.setAdapter(thumbAdapter);
        rvThumbs.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
    }

    private void updateCounter(int position) {
        tvCounter.setText((position + 1) + " / " + imageUrls.size());
    }

    private class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.VH> {
        private final List<String> list;

        ImagePagerAdapter(List<String> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            PhotoView photoView = new PhotoView(parent.getContext());
            photoView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new VH(photoView);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String data = list.get(position);
            try {
                if (data.startsWith("http")) Glide.with(holder.itemView).load(data).into(holder.photoView);
                else {
                    String clean = data.contains(",") ? data.split(",")[1] : data;
                    byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
                    Glide.with(holder.itemView).load(bytes).into(holder.photoView);
                }
            } catch (Exception e) {
                holder.photoView.setImageResource(R.drawable.ic_launcher_background);
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            PhotoView photoView;
            VH(View v) { super(v); photoView = (PhotoView) v; }
        }
    }

    private class ThumbAdapter extends RecyclerView.Adapter<ThumbAdapter.VH> {
        private final List<String> list;

        ThumbAdapter(List<String> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thumbnail, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String data = list.get(position);
            try {
                if (data.startsWith("http")) Glide.with(holder.img.getContext()).load(data).centerCrop().into(holder.img);
                else {
                    String clean = data.contains(",") ? data.split(",")[1] : data;
                    byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
                    Glide.with(holder.img.getContext()).load(bytes).centerCrop().into(holder.img);
                }
            } catch (Exception e) { holder.img.setImageResource(R.drawable.ic_launcher_background); }

            holder.border.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);
            holder.overlay.setVisibility(position == selectedPosition ? View.GONE : View.VISIBLE);
            holder.txt.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);
            holder.txt.setText(String.valueOf(position + 1));

            holder.itemView.setOnClickListener(v -> viewPager.setCurrentItem(position, true));
        }

        @Override public int getItemCount() { return list.size(); }

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
        if (isFinishing()) ImageDataHolder.setImages(null);
    }
}
