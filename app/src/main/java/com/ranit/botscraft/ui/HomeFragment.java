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

import com.ranit.botscraft.viewmodel.HomeViewModel;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private NestedScrollView nestedScrollView;
    private RecyclerView rvFeatured;
    private RecyclerView rvDiscover;
    private View skeletonFeatured, skeletonDiscover;
    private EditText etSearch;
    private BotAdapter discoverAdapter;
    private FeaturedBotAdapter featuredAdapter;
    private HomeViewModel viewModel;
    private final List<Bot> filteredBotList = new ArrayList<>();
    private TextView currentSelectedChip;
    private ImageView btnFilter;
    private String selectedGender = "All";
    @Nullable private User currentUser;
    @Nullable private ListenerRegistration userListener;
    private boolean isLoading = false;
    private static final int PAGE_SIZE = 10;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        
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
        
        if (viewModel.isDataLoaded()) {
            skeletonDiscover.setVisibility(View.GONE);
            rvDiscover.setVisibility(View.VISIBLE);
            skeletonFeatured.setVisibility(View.GONE);
            applyLocalFilters();
            updateFeaturedUI();
        } else {
            loadInitialBots();
        }
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
        featuredAdapter = new FeaturedBotAdapter(viewModel.getFeaturedBotList().getValue(), new FeaturedBotAdapter.OnBotClickListener() {
            @Override
            public void onChatClick(Bot bot) {
                openChat(bot);
            }

            @Override
            public void onProfileClick(Bot bot) {
                openProfile(bot);
            }
        });
        rvFeatured.setAdapter(featuredAdapter);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        rvDiscover.setLayoutManager(gridLayoutManager);
        discoverAdapter = new BotAdapter(filteredBotList, new BotAdapter.OnBotClickListener() {
            @Override
            public void onChatClick(Bot bot) {
                openChat(bot);
            }

            @Override
            public void onProfileClick(Bot bot) {
                openProfile(bot);
            }
        });
        rvDiscover.setAdapter(discoverAdapter);

        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    if (!isLoading && !viewModel.isLastPage()) {
                        loadMoreBots();
                    }
                }
            });
        }
    }

    private void openProfile(Bot bot) {
        if (getActivity() == null || bot == null) return;
        bot.sanitizeForIntent();
        Intent intent = new Intent(getActivity(), BotProfileActivity.class);
        intent.putExtra("bot", bot);
        startActivity(intent);
    }

    private void openChat(Bot bot) {
        if (getActivity() == null || bot == null) return;
        bot.sanitizeForIntent();
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
        int checkedItem = 0;
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
        viewModel.setLastPage(false);
        viewModel.setLastVisible(null);

        FirebaseFirestore db = FirebaseManager.getFirestore();
        // Use a larger initial pool (30) and removed explicit orderBy to allow more variety
        Query query = db.collection("bots").limit(30);
        query.get().addOnSuccessListener(value -> {
            List<Bot> bots = new ArrayList<>();
            if (value != null && !value.isEmpty()) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Bot bot = doc.toObject(Bot.class);
                    if (bot != null) {
                        bot.botId = doc.getId();
                        bots.add(bot);
                    }
                }
                viewModel.setLastVisible(value.getDocuments().get(value.size() - 1));
                if (value.size() < 30) viewModel.setLastPage(true);
            } else {
                viewModel.setLastPage(true);
            }
            
            // Randomize the sequence for each user/session
            Collections.shuffle(bots);
            
            viewModel.setFullBotList(bots);
            viewModel.setDataLoaded(true);
            isLoading = false;
            if (skeletonDiscover != null) skeletonDiscover.setVisibility(View.GONE);
            if (rvDiscover != null) rvDiscover.setVisibility(View.VISIBLE);
            applyLocalFilters();
            loadFeaturedBots();
        }).addOnFailureListener(e -> isLoading = false);
    }

    private void loadMoreBots() {
        if (isLoading || viewModel.isLastPage()) return;
        isLoading = true;

        FirebaseFirestore db = FirebaseManager.getFirestore();
        // Must match the ordering/lack of ordering of the initial query
        Query query = db.collection("bots").startAfter(viewModel.getLastVisible()).limit(PAGE_SIZE);
        query.get().addOnSuccessListener(value -> {
            if (value != null && !value.isEmpty()) {
                List<Bot> currentList = viewModel.getFullBotList().getValue();
                if (currentList == null) currentList = new ArrayList<>();
                else currentList = new ArrayList<>(currentList);
                
                List<Bot> newBots = new ArrayList<>();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Bot bot = doc.toObject(Bot.class);
                    if (bot != null) {
                        bot.botId = doc.getId();
                        newBots.add(bot);
                    }
                }
                
                // Shuffle newly loaded bots to maintain randomness
                Collections.shuffle(newBots);
                currentList.addAll(newBots);
                
                viewModel.setFullBotList(currentList);
                viewModel.setLastVisible(value.getDocuments().get(value.size() - 1));
                if (value.size() < PAGE_SIZE) viewModel.setLastPage(true);
            } else {
                viewModel.setLastPage(true);
            }
            isLoading = false;
            applyLocalFilters();
        }).addOnFailureListener(e -> isLoading = false);
    }

    private void loadFeaturedBots() {
        FirebaseFirestore db = FirebaseManager.getFirestore();
        db.collection("bots")
                .whereArrayContains("categories", "trending")
                .limit(20)
                .get()
                .addOnSuccessListener(value -> {
                    List<Bot> featured = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Bot bot = doc.toObject(Bot.class);
                            if (bot != null) {
                                bot.botId = doc.getId();
                                if (currentUser == null || currentUser.blockedBots == null || !currentUser.blockedBots.contains(bot.botId)) {
                                    featured.add(bot);
                                }
                            }
                        }
                    }
                    // Randomize featured bots
                    Collections.shuffle(featured);
                    viewModel.setFeaturedBotList(featured);
                    updateFeaturedUI();
                });
    }

    private void updateFeaturedUI() {
        List<Bot> featured = viewModel.getFeaturedBotList().getValue();
        if (featuredAdapter != null && featured != null) {
            featuredAdapter.updateData(featured);
        }
        boolean hasFeatured = featured != null && !featured.isEmpty();
        
        if (skeletonFeatured != null) {
            // Hide skeleton if data is already loaded (even if featured is still loading, 
            // the main bots are already there and we don't want skeletons on return)
            if (viewModel.isDataLoaded()) {
                skeletonFeatured.setVisibility(View.GONE);
            } else {
                skeletonFeatured.setVisibility(hasFeatured ? View.GONE : View.VISIBLE);
            }
        }
        
        if (rvFeatured != null) {
            rvFeatured.setVisibility(hasFeatured ? View.VISIBLE : View.GONE);
        }
    }

    private void resetPaginationAndReload() {
        viewModel.setFullBotList(new ArrayList<>());
        filteredBotList.clear();
        discoverAdapter.notifyDataSetChanged();
        if (skeletonDiscover != null) skeletonDiscover.setVisibility(View.VISIBLE);
        if (rvDiscover != null) rvDiscover.setVisibility(View.GONE);
        viewModel.setDataLoaded(false);
        loadInitialBots();
    }

    private void applyLocalFilters() {
        if (!isAdded() || getView() == null) return;
        String category = currentSelectedChip != null ? currentSelectedChip.getText().toString() : "Popular";
        String query = etSearch != null ? etSearch.getText().toString().toLowerCase() : "";

        filteredBotList.clear();
        List<Bot> fullList = viewModel.getFullBotList().getValue();
        if (fullList != null) {
            for (Bot b : fullList) {
                boolean matchesSearch = b.name != null && b.name.toLowerCase().contains(query);
                boolean matchesGender = selectedGender.equals("All") || (b.gender != null && b.gender.equalsIgnoreCase(selectedGender));
                boolean matchesCategory = category.equals("Popular") || (b.categories != null && b.categories.contains(category.toLowerCase()));

                if (matchesSearch && matchesGender && matchesCategory) {
                    if (currentUser == null || currentUser.blockedBots == null || !currentUser.blockedBots.contains(b.botId)) {
                        filteredBotList.add(b);
                    }
                }
            }
        }
        if (discoverAdapter != null) discoverAdapter.notifyDataSetChanged();
    }
}
