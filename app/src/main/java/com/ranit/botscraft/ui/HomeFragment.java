package com.ranit.botscraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ranit.botscraft.R;
import com.ranit.botscraft.adapter.BotAdapter;
import com.ranit.botscraft.adapter.FeaturedBotAdapter;
import com.ranit.botscraft.firebase.FirebaseManager;
import com.ranit.botscraft.model.Bot;
import com.ranit.botscraft.model.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private NestedScrollView nestedScrollView;
    private RecyclerView rvFeatured;
    private RecyclerView rvDiscover;
    private View skeletonFeatured, skeletonDiscover;
    private EditText etSearch;
    private BotAdapter discoverAdapter;
    private FeaturedBotAdapter featuredAdapter;
    private final List<Bot> fullBotList = new ArrayList<>();
    private final List<Bot> filteredBotList = new ArrayList<>();
    private final List<Bot> featuredBotList = new ArrayList<>();
    private TextView currentSelectedChip;
    private ImageView btnFilter;
    private String selectedGender = "All";
    @Nullable private User currentUser;
    @Nullable private ListenerRegistration userListener;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private DocumentSnapshot lastVisible;
    private static final int PAGE_SIZE = 10;
    public HomeFragment() {}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nestedScrollView = view.findViewById(R.id.nestedScrollViewHome);
        rvFeatured = view.findViewById(R.id.rvFeatured);
        rvDiscover = view.findViewById(R.id.rvDiscover);
        skeletonFeatured = view.findViewById(R.id.skeletonFeatured);
        skeletonDiscover = view.findViewById(R.id.skeletonDiscover);
        etSearch = view.findViewById(R.id.etSearch);
        btnFilter = view.findViewById(R.id.btnFilter);
        setupRecyclerViews();
        setupChips(view);
        setupSearch();
        setupFilter();
        listenToUserUpdates();
        loadInitialBots();
    }
    private void listenToUserUpdates() {
        String uid = FirebaseManager.getUserId();
        if (uid == null) return;
        userListener = FirebaseManager.getFirestore().collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    currentUser = snapshot.toObject(User.class);
                    applyLocalFilters();
                    updateFeaturedUI();
                });
    }
    private void setupRecyclerViews() {
        if (getContext() == null) return;
        rvFeatured.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        featuredAdapter = new FeaturedBotAdapter(featuredBotList, this::openChat);
        rvFeatured.setAdapter(featuredAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        rvDiscover.setLayoutManager(gridLayoutManager);
        discoverAdapter = new BotAdapter(filteredBotList, this::openChat);
        rvDiscover.setAdapter(discoverAdapter);
        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    if (!isLoading && !isLastPage) {
                        loadMoreBots();
                    }
                }
            });
        }
    }
    private void openChat(Bot bot) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("bot", bot);
        startActivity(intent);
    }
    private void setupChips(View view) {
        currentSelectedChip = view.findViewById(R.id.chipPopular);
        View.OnClickListener chipListener = v -> {
            if (getContext() == null) return;
            if (currentSelectedChip != null) {
                currentSelectedChip.setBackgroundResource(R.drawable.bg_chip_unselected);
                currentSelectedChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
            }
            currentSelectedChip = (TextView) v;
            currentSelectedChip.setBackgroundResource(R.drawable.bg_chip_selected);
            currentSelectedChip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            if (etSearch != null) etSearch.setText("");
            resetPaginationAndReload();
        };
        int[] chipIds = {
            R.id.chipPopular, R.id.chipBold, R.id.chipNormal, R.id.chipProfessional,
            R.id.chipFriendly, R.id.chipRomantic, R.id.chipFlirty, R.id.chipAngry, 
            R.id.chipIndian, R.id.chipArab, R.id.chipEuropean, R.id.chipJapanese,
            R.id.chipShy, R.id.chipMature
        };
        for (int id : chipIds) {
            View chip = view.findViewById(id);
            if (chip != null) chip.setOnClickListener(chipListener);
        }
    }
    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyLocalFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilter() {
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showGenderFilterDialog());
        }
    }
    private void showGenderFilterDialog() {
        if (getContext() == null) return;
        
        String[] options = {"All", "Male", "Female", "Other"};
        int checkedItem = 0; // Default to "All"
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(selectedGender)) {
                checkedItem = i;
                break;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filter by Gender")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    selectedGender = options[which];
                    resetPaginationAndReload();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void loadInitialBots() {
        if (isLoading) return;
        isLoading = true;
        isLastPage = false;
        lastVisible = null;

        FirebaseFirestore db = FirebaseManager.getFirestore();
        Query query = db.collection("bots")
                .orderBy("name") 
                .limit(PAGE_SIZE);
        query.get().addOnSuccessListener(value -> {
            fullBotList.clear();
            if (value != null && !value.isEmpty()) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Bot bot = doc.toObject(Bot.class);
                    if (bot != null) {
                        bot.botId = doc.getId();
                        fullBotList.add(bot);
                    }
                }
                lastVisible = value.getDocuments().get(value.size() - 1);
                if (value.size() < PAGE_SIZE) isLastPage = true;
            } else {
                isLastPage = true;
            }
            isLoading = false;
            if (skeletonDiscover != null) skeletonDiscover.setVisibility(View.GONE);
            if (rvDiscover != null) rvDiscover.setVisibility(View.VISIBLE);
            
            applyLocalFilters();
            loadFeaturedBots();
        }).addOnFailureListener(e -> {
            isLoading = false;
        });
    }
    private void loadMoreBots() {
        if (isLoading || isLastPage) return;
        isLoading = true;

        FirebaseFirestore db = FirebaseManager.getFirestore();
        Query query = db.collection("bots")
                .orderBy("name")
                .startAfter(lastVisible)
                .limit(PAGE_SIZE);

        query.get().addOnSuccessListener(value -> {
            if (value != null && !value.isEmpty()) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Bot bot = doc.toObject(Bot.class);
                    if (bot != null) {
                        bot.botId = doc.getId();
                        fullBotList.add(bot);
                    }
                }
                lastVisible = value.getDocuments().get(value.size() - 1);
                if (value.size() < PAGE_SIZE) isLastPage = true;
            } else {
                isLastPage = true;
            }
            isLoading = false;
            applyLocalFilters();
        }).addOnFailureListener(e -> {
            isLoading = false;
        });
    }

    private void loadFeaturedBots() {
        FirebaseFirestore db = FirebaseManager.getFirestore();
        db.collection("bots")
                .whereArrayContains("categories", "trending")
                .limit(10) // Limit trending bots
                .get()
                .addOnSuccessListener(value -> {
                    featuredBotList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Bot bot = doc.toObject(Bot.class);
                            if (bot != null) {
                                bot.botId = doc.getId();
                                // Only add if not blocked
                                if (currentUser == null || !currentUser.blockedBots.contains(bot.botId)) {
                                    featuredBotList.add(bot);
                                }
                            }
                        }
                    }
                    updateFeaturedUI();
                });
    }

    private void resetPaginationAndReload() {
        fullBotList.clear();
        filteredBotList.clear();
        discoverAdapter.notifyDataSetChanged();
        if (skeletonDiscover != null) skeletonDiscover.setVisibility(View.VISIBLE);
        if (rvDiscover != null) rvDiscover.setVisibility(View.GONE);
        loadInitialBots();
    }

    private void applyLocalFilters() {
        if (!isAdded() || getView() == null) return;

        String category = currentSelectedChip != null ? currentSelectedChip.getText().toString() : "Popular";
        String query = etSearch != null ? etSearch.getText().toString().toLowerCase() : "";

        filteredBotList.clear();
        for (Bot bot : fullBotList) {
            if (currentUser != null && currentUser.blockedBots.contains(bot.botId)) continue;
            boolean matchesGender = selectedGender.equals("All") || (bot.gender != null && bot.gender.equalsIgnoreCase(selectedGender));
            boolean matchesCategory = category.equalsIgnoreCase("Popular") || isBotInCategory(bot, category);
            boolean matchesSearch = query.isEmpty() || isMatch(bot.name, query) || isMatch(bot.personality, query) || isMatch(bot.description, query);
            if (matchesGender && matchesCategory && matchesSearch) {
                filteredBotList.add(bot);
            }
        }
        discoverAdapter.notifyDataSetChanged();
    }

    private void updateFeaturedUI() {
        if (!isAdded() || getView() == null) return;
        
        // Re-filter featuredBotList based on current blocking state
        List<Bot> tempFeatured = new ArrayList<>();
        for (Bot b : featuredBotList) {
            if (currentUser == null || !currentUser.blockedBots.contains(b.botId)) {
                tempFeatured.add(b);
            }
        }
        
        if (featuredAdapter != null) {
            featuredAdapter.notifyDataSetChanged();
        }
        if (skeletonFeatured != null) skeletonFeatured.setVisibility(View.GONE);

        boolean hasTrending = !tempFeatured.isEmpty();
        boolean showFeatured = (etSearch == null || etSearch.getText().toString().isEmpty()) && selectedGender.equals("All");
        
        if (rvFeatured != null) rvFeatured.setVisibility(showFeatured && hasTrending ? View.VISIBLE : View.GONE);
        View featuredTitle = getView().findViewById(R.id.tvFeaturedTitle);
        if (featuredTitle != null) featuredTitle.setVisibility(showFeatured && hasTrending ? View.VISIBLE : View.GONE);
    }

    private boolean isBotInCategory(Bot bot, String category) {
        if (bot.categories != null) {
            for (String cat : bot.categories) {
                if (cat.equalsIgnoreCase(category)) return true;
            }
        }
        return isMatch(bot.modelType, category) || isMatch(bot.relationship, category) || isMatch(bot.moodStyle, category);
    }

    private boolean isMatch(String field, String filter) {
        return field != null && field.toLowerCase().contains(filter.toLowerCase());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
    }
}
