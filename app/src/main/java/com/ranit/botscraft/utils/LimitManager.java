package com.ranit.botscraft.utils;

public class LimitManager {

    private static int count = 0;

    public static boolean reachedLimit() {
        return count >= Constants.FREE_MAX_MSG;
    }

    public static void increment() {
        count++;
    }
}

