package com.rabbithole.billing51.try_1;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.io.IOException;
import java.util.List;

public class PurchaseListener implements PurchasesUpdatedListener {

    BillingClient billingClient;
    String TAG = "PurchaseListener";

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list!= null){
            for(Purchase purchase:list){
                handlePurchase(purchase, billingClient);
            }
        }
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Log.d(TAG, "Subscribed");
        }
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
            Log.d(TAG, "Not supported");
        }
        else{
            Log.e(TAG, billingResult.getDebugMessage());
        }
    }

    public void handlePurchase(Purchase purchase, BillingClient billingClient){

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
               Log.d(TAG, "Thx");
            }
        };

        billingClient.consumeAsync(consumeParams, listener);

        if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
            if(!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())){
                Log.d(TAG, "Thx");
                return;
            }
            if(!purchase.isAcknowledged()){
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknewledgePurchaseResponseListener);
                Log.d(TAG, "Subscribed");
            }
            else{
                Log.d(TAG, "Already subs");
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                Log.d(TAG, "Pending");
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                Log.d(TAG, "Unspecified");
        }
    }

    AcknowledgePurchaseResponseListener acknewledgePurchaseResponseListener = billingResult -> {
        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
            Log.d(TAG, "Subscribed");
        }
    };

    private boolean verifyValidSignature(String signedData, String signature){
        try {
            String base64Key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0phhR1yHtfpzdWgY+5bnym/QrlyL8nrTqY2LbHDr0Xd4UxWo4k+gydZ4sShavhinZrpIipU1L0aWjIcA0i1FqdSQkCUbKg3AvOO9Iv+fhcdN2yW6fjhCvd10KWolp6QYD9snvMnaZ6lktAxbjz9CM0CeYbu/zDETTiiz97T39WVj++iCngyz0olwCFzUSHD2E6pdxfmK6E7ZKJJuet9WLl8KO6JHcHct+SeGbDLiEbO/cOuiS6f9WR/G0bPNIXR/bDGol2enHIrdImPNUVxpWSg2r8wgA1uP3Cmyx+Dgt51Qc+tsatcbFdDYMxkJcXamb8f4UKKH0b6mh5m9KPPSiwIDAQAB";
            return Security.verifyPurchase(base64Key, signedData, signature);
        }
        catch (IOException e){
            return false;
        }
    }

}
