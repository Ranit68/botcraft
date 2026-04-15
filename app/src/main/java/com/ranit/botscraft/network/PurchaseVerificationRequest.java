package com.ranit.botscraft.network;

public class PurchaseVerificationRequest {
    public String purchaseToken;
    public String sku;

    public PurchaseVerificationRequest(String purchaseToken, String sku) {
        this.purchaseToken = purchaseToken;
        this.sku = sku;
    }
}
