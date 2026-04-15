package com.ranit.botscraft.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.ranit.botscraft.R;

public class GamesFragment extends Fragment {

    private ViewPager2 viewPager;
    private TextView tabGuess, tabStory, tabSpin;
    private View tabIndicator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_games, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.viewPagerGames);
        tabGuess = view.findViewById(R.id.tabGuess);
        tabStory = view.findViewById(R.id.tabStory);
        tabSpin = view.findViewById(R.id.tabSpin);
        tabIndicator = view.findViewById(R.id.tabIndicator);

        viewPager.setAdapter(new GamesPagerAdapter(this));
        
        // Disable swiping for custom tab logic
        viewPager.setUserInputEnabled(true);

        tabGuess.setOnClickListener(v -> viewPager.setCurrentItem(0));
        tabStory.setOnClickListener(v -> viewPager.setCurrentItem(1));
        tabSpin.setOnClickListener(v -> viewPager.setCurrentItem(2));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                float translationX = (position + positionOffset) * tabIndicator.getWidth();
                tabIndicator.setTranslationX(translationX);
            }

            @Override
            public void onPageSelected(int position) {
                updateTabs(position);
            }
        });

        // Initialize indicator width
        view.post(() -> {
            ViewGroup.LayoutParams lp = tabIndicator.getLayoutParams();
            lp.width = view.getWidth() / 3;
            tabIndicator.setLayoutParams(lp);
        });
    }

    private void updateTabs(int position) {
        tabGuess.setTextColor(getContext().getColor(position == 0 ? R.color.accent_purple : R.color.text_label));
        tabStory.setTextColor(getContext().getColor(position == 1 ? R.color.accent_purple : R.color.text_label));
        tabSpin.setTextColor(getContext().getColor(position == 2 ? R.color.accent_purple : R.color.text_label));
    }

    private static class GamesPagerAdapter extends FragmentStateAdapter {
        public GamesPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1: return new GameStoryFragment();
                case 2: return new GameSpinFragment();
                default: return new GameGuessFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
