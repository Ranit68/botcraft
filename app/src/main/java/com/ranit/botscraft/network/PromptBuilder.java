package com.ranit.botscraft.network;

import com.ranit.botscraft.model.Bot;

public class PromptBuilder {

    public static String build(Bot bot) {
        return "You are an AI chatbot.\n"
                + "Name: " + bot.name + "\n"
                + "Gender: " + bot.gender + "\n"
                + "Age: " + bot.age + "\n"
                + "Relationship: " + bot.relationship + "\n"
                + "Personality: " + bot.personality + "\n"
                + "Description: " + bot.description + "\n"
                + "Behave exactly like this.";
    }
}

