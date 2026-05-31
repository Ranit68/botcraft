package com.ranit.botscraft.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ranit.botscraft.model.Bot;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<List<Bot>> fullBotList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Bot>> featuredBotList = new MutableLiveData<>(new ArrayList<>());
    
    private DocumentSnapshot lastVisible;
    private boolean isLastPage = false;
    private boolean isDataLoaded = false;

    public LiveData<List<Bot>> getFullBotList() { return fullBotList; }
    public LiveData<List<Bot>> getFeaturedBotList() { return featuredBotList; }

    public void setFullBotList(List<Bot> bots) { fullBotList.setValue(bots); }
    public void setFeaturedBotList(List<Bot> bots) { featuredBotList.setValue(bots); }

    public DocumentSnapshot getLastVisible() { return lastVisible; }
    public void setLastVisible(DocumentSnapshot lastVisible) { this.lastVisible = lastVisible; }

    public boolean isLastPage() { return isLastPage; }
    public void setLastPage(boolean lastPage) { isLastPage = lastPage; }

    public boolean isDataLoaded() { return isDataLoaded; }
    public void setDataLoaded(boolean loaded) { isDataLoaded = loaded; }
}
