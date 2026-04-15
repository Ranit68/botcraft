package com.ranit.botscraft.model;

public class SelectionItem {
    public String id;
    public String label;
    public String description;
    public String imageUrl;
    public int imageRes = -1;
    public String colorHex;

    public SelectionItem(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public SelectionItem(String id, String label, String description) {
        this.id = id;
        this.label = label;
        this.description = description;
    }

    public SelectionItem(String id, String label, int imageRes) {
        this.id = id;
        this.label = label;
        this.imageRes = imageRes;
    }

    public static SelectionItem color(String id, String label, String colorHex) {
        SelectionItem item = new SelectionItem(id, label);
        item.colorHex = colorHex;
        return item;
    }
}
