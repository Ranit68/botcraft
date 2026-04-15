package com.ranit.botscraft.util;

import java.util.List;

public class ImageDataHolder {
    private static List<String> images;

    public static void setImages(List<String> imgList) {
        images = imgList;
    }

    public static List<String> getImages() {
        return images;
    }
}
