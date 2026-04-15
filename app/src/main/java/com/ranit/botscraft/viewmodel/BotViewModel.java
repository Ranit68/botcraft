package com.ranit.botscraft.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ranit.botscraft.model.Bot;

public class BotViewModel extends ViewModel {
    private final MutableLiveData<Bot> botData = new MutableLiveData<>(new Bot());

    public MutableLiveData<Bot> getBotData() {
        return botData;
    }

    public void updateBot(Bot updatedBot) {
        botData.setValue(updatedBot);
    }
}
