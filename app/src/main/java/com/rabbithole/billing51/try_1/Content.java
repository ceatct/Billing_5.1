package com.rabbithole.billing51.try_1;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Content {

    private BillingClient billingClient;
    private Context context;
    private boolean isSuccess;

    public Content (Context context){
        this.context = context;
    }

    public static void buyItem(BillingClient billingClient, Activity activity){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {

            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                List<QueryProductDetailsParams.Product> productList = List.of(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("200_coins")
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                );
                QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)
                        .build();
                billingClient.queryProductDetailsAsync(params, (billingResult1, list) -> {
                    for(ProductDetails productDetails:list){
                        assert productDetails.getOneTimePurchaseOfferDetails() != null;
                        String offerToken = productDetails.getOneTimePurchaseOfferDetails()
                                .getPriceCurrencyCode();
                        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = List.of(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .setOfferToken(offerToken)
                                        .build()
                        );
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productDetailsParamsList)
                                .build();
                        billingClient.launchBillingFlow(activity, billingFlowParams);
                    }
                });
            }
        });
    }

    public static String getPrice(BillingClient billingClient, String des){
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {

            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                List<QueryProductDetailsParams.Product> productList = List.of(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("200_coins")
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build());

                QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)
                        .build();

                billingClient.queryProductDetailsAsync(params, (billingResult1, list) -> {
                    for (ProductDetails productDetails:list){
                        assert productDetails.getSubscriptionOfferDetails() != null;

                        String des = productDetails.getName() + " " + productDetails.getDescription() + " " + " Price " + productDetails.getOneTimePurchaseOfferDetails()
                                .getFormattedPrice();
                    }
                });
            }
        }));
        return des;
    }

    private String query_purchase(){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {

            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(() -> {
                        try{
                            billingClient.queryPurchasesAsync(
                                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                                    (billingResult1, purchaseList) -> {
                                        for(Purchase ignored : purchaseList){
                                            isSuccess = true;
                                        }
                                    }
                            );
                        }
                        catch (Exception ignored){

                        }
                        if(isSuccess){
                            ConnectionClass.coins = "200";
                        }
                    });
                }
            }
        });
        return "200";
    }



}
