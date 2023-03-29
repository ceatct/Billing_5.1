package com.rabbithole.billing51.try_2;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.rabbithole.billing51.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingClientLifecycle implements DefaultLifecycleObserver, PurchasesUpdatedListener, BillingClientStateListener,
        ProductDetailsResponseListener, PurchasesResponseListener, AcknowledgePurchaseResponseListener {
    private static final String TAG = "BillingLifecycle";
    public static final String ACTION_PURCHASE_STATUS_UPDATED = "purchase_status_updated";
    private static volatile BillingClientLifecycle INSTANCE;
    private final Application app;
    private BillingClient billingClient;
    private int billingConnectionCounter = 5;
    public static final List<String> LIST_OF_PRODUCTS = Collections.unmodifiableList(
            new ArrayList<String>() {{
              /*  if (BuildConfig.DEBUG) {
                    add(BuildHelper.TEST);
                } else {
                    add(BuildHelper.PREMIUM_SUBS);
                } */
            }});
    public final MutableLiveData<Map<String, ProductDetails>> productsWithProductDetails = new MutableLiveData<>();
    /**
     * The purchase event is observable. Only one observer will be notified.
     */
    public SingleLiveEvent<List<Purchase>> purchaseUpdateEvent = new SingleLiveEvent<>();
    public MutableLiveData<ConnectionState> connectionLiveState = new MutableLiveData<>();

    public enum ConnectionState {
        CONNECTED, ERROR;
        static String msg;

        public static ConnectionState ERROR(String message) {
            msg = message;
            return ERROR;
        }

        public String getMsg() {
            return msg;
        }
    }
    /**
     * Purchases are observable. This list will be updated when the Billing Library
     * detects new or existing purchases. All observers will be notified.
     */
    public MutableLiveData<List<Purchase>> purchases = new MutableLiveData<>();

    private BillingClientLifecycle(Application app) {
        this.app = app;
    }
    public static BillingClientLifecycle getInstance(Application app) {
        if (INSTANCE == null) {
            synchronized (BillingClientLifecycle.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BillingClientLifecycle(app);
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "ON_CREATE");
        // Create a new BillingClient in onCreate().
        // Since the BillingClient can only be used once, we need to create a new instance
        // after ending the previous connection to the Google Play Store in onDestroy().
        startBillingConnection();
    }

    public void startBillingConnection() {
        billingClient = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases() // Not used for subscriptions.
                .build();

        if (isBillingReady()) {
            Log.d(TAG, "BillingClient: END connection...");
            billingClient.endConnection();
        }
        Log.d(TAG, "BillingClient: Start connection...");
        billingClient.startConnection(this);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "ON_DESTROY");
        if (isBillingReady()) {
            Log.d(TAG, "BillingClient can only be used once -- closing connection");
            // BillingClient can only be used once.
            // After calling endConnection(), we must create a new BillingClient.
            billingClient.endConnection();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Log.d(TAG, "onBillingServiceDisconnected");
        new Handler(Looper.myLooper()).postDelayed(() -> {
            if (billingClient != null
                    && billingClient.getConnectionState() != BillingClient.ConnectionState.CONNECTED
                    && billingConnectionCounter > 0) {
                billingClient.startConnection(BillingClientLifecycle.this);
                billingConnectionCounter--;
            }
        }, 2000);
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "onBillingSetupFinished: " + responseCode + " " + debugMessage);
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            // The billing client is ready. You can query purchases here.
            connectionLiveState.postValue(ConnectionState.CONNECTED);
            queryProductDetails();
            queryPurchases();
        } else {
            String msg = app.getString(getBillingMessage(responseCode));
            connectionLiveState.postValue(ConnectionState.ERROR(msg));
        }
    }

    private void queryProductDetails() {
        QueryProductDetailsParams.Builder paramsBuilder = QueryProductDetailsParams.newBuilder();
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        for (String product: LIST_OF_PRODUCTS) {
            productList.add(
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(product)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build());
        }
        Log.i(TAG, "queryProductDetailsAsync");
        billingClient.queryProductDetailsAsync(paramsBuilder.setProductList(productList).build(), this);
    }

    private void queryPurchases() {
        if (!isBillingReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
        }
        Log.d(TAG, "queryPurchases: SUBS");
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
        billingClient.queryPurchasesAsync(params,this);
    }

    public boolean isBillingReady() {
        return billingClient != null && billingClient.isReady();
    }

    /**
    {
        "productId":"ua.com.emulatorband.nes4u_premium",
        "type":"subs",
        "title":"Преміум пакет (NES4You: Nostalgia Emulator)",
        "name":"Преміум пакет",
        "localizedIn":[
            "en-US"
        ],
        "skuDetailsToken":"AEuhp4K-f4tXTDhksS2qAnDCFmG-FqvSulNcyEAQ1F734bhHgC-i5NCP9AvgbkWRSflu",
            "subscriptionOfferDetails":[
        {
            "offerIdToken":"AUj\/YhjXgKmUYzH+0RZPL60k9z\/4iKfDpjrD6cDbROnzfCu+JsSttkjYCGueTm\/yiZWLopG+QzEQQkLFp59PrMJFq54lOF29qZzQR3BNHOop845KqWILRrEaklS2hjbaX4GMQZHRGhU=",
                "basePlanId":"premium-base-1",
                "offerId":"offer3",
                "pricingPhases":[
            {
                "priceAmountMicros":31160000,
                    "priceCurrencyCode":"UAH",
                    "formattedPrice":"31,16 грн",
                    "billingPeriod":"P4W",
                    "recurrenceMode":2,
                    "billingCycleCount":1
            },
            {
                "priceAmountMicros":889990000,
                    "priceCurrencyCode":"UAH",
                    "formattedPrice":"889,99 грн",
                    "billingPeriod":"P1Y",
                    "recurrenceMode":1
            }
         ],
            "offerTags":[
            "offer-3-tag",
                    "premium-base-1-year"
         ]
        },
        {
            "offerIdToken":"AUj\/YhhgDIeZJxXvX1GqYBBOIWIUXxScbD\/dIxZdLLQU2fpEns7M8Whg1oFdjGgvHxk8FzmBffrUOGu5Z6KAJoVf40V5NvGJyshr04ajw2yMWtnKqXpPqb3sA1G1UgK6jbKUtDtwUYw=",
                "basePlanId":"premium-base-1",
                "offerId":"offer1",
                "pricingPhases":[
            {
                "priceAmountMicros":0,
                    "priceCurrencyCode":"UAH",
                    "formattedPrice":"Безкоштовно",
                    "billingPeriod":"P3D",
                    "recurrenceMode":2,
                    "billingCycleCount":1
            },
            {
                "priceAmountMicros":60310000,
                    "priceCurrencyCode":"UAH",
                    "formattedPrice":"60,31 грн",
                    "billingPeriod":"P1M",
                    "recurrenceMode":2,
                    "billingCycleCount":1
            },
            {
                "priceAmountMicros":889990000,
                    "priceCurrencyCode":"UAH",
                    "formattedPrice":"889,99 грн",
                    "billingPeriod":"P1Y",
                    "recurrenceMode":1
            }
         ],
            "offerTags":[
            "offer1",
                    "premium",
                    "premium-base-1-year"
         ]
        },
        {
            "offerIdToken":"AUj\/YhjVMd4Lt2G8OQ6x8COQDFqt9JHQL082\/xd4QqNRbkb\/jXUCiIUqlD6bae5iuAgER8nu680TP6j9eY8bJbLDat5\/DNs\/T64r3r5EObDHSh3nEwiz03qJxuGvnr69uJacgV\/rrq0=",
                "basePlanId":"premium-base-1",
                "offerId":"offer2",
                "pricingPhases":[
            {
                "priceAmountMicros":60310000,
                    "priceCurrencyCode":"UAH",
                    "formattedPrice":"60,31 грн",
                    "billingPeriod":"P1M",
                    "recurrenceMode":2,
                    "billingCycleCount":1
            },
            {
                "priceAmountMicros":889990000,
                    "priceCurrencyCode":"UAH",
                    "formattedPrice":"889,99 грн",
                    "billingPeriod":"P1Y",
                    "recurrenceMode":1
            }
         ],
            "offerTags":[
            "offer2",
                    "premium",
                    "premium-base-1-year"
         ]
        },
        {
            "offerIdToken":"AUj\/Yhj3Dep2frx+JPZZZmgX\/GDUZXyin9DAGP9yPp093wbK4cZjgxoCnIzraje6YZHHRhRSg4btD8sOXxIEpJYBHOsejUd8q1w9dseVjAuKh9nFSubLspn4Gmof2Fc=",
                "basePlanId":"premium-base-1",
                "pricingPhases":[
            {
                "priceAmountMicros":889990000,
                    "priceCurrencyCode":"UAH",
                    "formattedPrice":"889,99 грн",
                    "billingPeriod":"P1Y",
                    "recurrenceMode":1
            }
         ],
            "offerTags":[
            "premium-base-1-year"
         ]
        }
   ]
     }
     */
    @Override
    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:

                Log.i(TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                final int expectedSkuDetailsCount = LIST_OF_PRODUCTS.size();
                if (list.isEmpty()) {
                    productsWithProductDetails.postValue(Collections.emptyMap());
                    Log.e(
                            TAG, "onProductDetailsResponse: " +
                                    "Expected ${expectedProductDetailsCount}, " +
                                    "Found null ProductDetails. " +
                                    "Check to see if the products you requested are correctly published " +
                                    "in the Google Play Console."
                    );
                } else {
                    Map<String, ProductDetails> newProductDetailList = new HashMap<>();
                    for (ProductDetails productDetails : list) {
                        newProductDetailList.put(productDetails.getProductId(), productDetails);
                    }
                    productsWithProductDetails.postValue(newProductDetailList);
                    int productDetailsCount = newProductDetailList.size();
                    if (productDetailsCount == expectedSkuDetailsCount) {
                        Log.i(TAG, "onSkuDetailsResponse: Found " + productDetailsCount + " SkuDetails");
                    } else {
                        Log.e(TAG, "onSkuDetailsResponse: " +
                                "Expected " + expectedSkuDetailsCount + ", " +
                                "Found " + productDetailsCount + " SkuDetails. " +
                                "Check to see if the SKUs you requested are correctly published " +
                                "in the Google Play Console.");
                    }
                }
                break;
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
            case BillingClient.BillingResponseCode.ERROR:
                Log.e(TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                connectionLiveState.postValue(ConnectionState.ERROR(app.getString(getBillingMessage(responseCode))));
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            // These response codes are not expected.
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            default:
                Log.wtf(TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
        }
    }

    @Override
    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
        Log.d(TAG, "onQueryPurchasesResponse.. purchase list size:" + list.size());
        processPurchases(list);
    }

    private void processPurchases(List<Purchase> purchasesList) {
        /**
         * {
         *  "orderId":"GPA.3327-7136-8566-62008",
         *  "packageName":"ua.com.emulatorband.nes4u",
         *  "productId":"ua.com.emulatorband.nes4u_month",
         *  "purchaseTime":1671715469889,"purchaseState":0,
         *  "purchaseToken":"ahigolpcaldnadgjpcocffko.AO-J1OzJghrqV8DT3X3sjKrpuoaMpv8-WpenhAo_Jhw_sExupFopINRSFZc7q575hLbyV5d6WvwBkN62JlT811c3dc9ubVIL6Vn5Ua1HkAgHSQ30fJmNo98",
         *  "autoRenewing":true,"acknowledged":true
         *  }
         */

        if (purchasesList != null) {
            Log.d(TAG, "processPurchases: " + purchasesList.size() + " purchase(s)");
        } else {
            Log.d(TAG, "processPurchases: with no purchases");
        }
        if (isUnchangedPurchaseList(purchasesList)) {
            Log.d(TAG, "processPurchases: Purchase list has not changed");
            return;
        }

        purchaseUpdateEvent.postValue(purchasesList);
        if (purchasesList != null && !purchasesList.isEmpty()) {
            sendPurchaseAction();
            for (Purchase purchase: purchasesList) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    updatePurchaseState(purchase.getPurchaseToken());
                }
            }
        } else {
            resetPurchaseState();
        }
        purchases.postValue(purchasesList);
        if (purchasesList != null) {
            logAcknowledgementStatus(purchasesList);
            for (Purchase purchase: purchasesList) {
                if (!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, this);
                }
            }
        }
    }

    private void updatePurchaseState(String token) {
       // Prefs.getInstance().savePurchaseToken(token);
    }

    private void resetPurchaseState() {
       // Prefs.getInstance().savePurchaseToken(null);
    }

    @Override
    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "acknowledgePurchase: " + responseCode + " " + debugMessage);
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "onAcknowledgePurchaseResponse");
            processPurchases(purchases.getValue());
        }
    }

    private void sendPurchaseAction() {
        app.sendBroadcast(new Intent(ACTION_PURCHASE_STATUS_UPDATED));
    }

    public boolean isSubscribed() {
        ///// ТУТ Я КОМЕНТИЛ
      //  String token = Prefs.getInstance().getPurchaseToken();
     //   return token != null;

        /*if (purchases == null || purchases.getValue() == null || purchases.getValue().isEmpty()) return false;
        for (Purchase purchase: purchases.getValue()) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                return true;
            }
        }
        return false;*/
        return true;
    }

    /**
     * Log the number of purchases that are acknowledge and not acknowledged.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * When the purchase is first received, it will not be acknowledge.
     * This application sends the purchase token to the server for registration. After the
     * purchase token is registered to an account, the Android app acknowledges the purchase token.
     * The next time the purchase list is updated, it will contain acknowledged purchases.
     */
    private void logAcknowledgementStatus(List<Purchase> purchasesList) {
        int ack_yes = 0;
        int ack_no = 0;
        for (Purchase purchase : purchasesList) {
            if (purchase.isAcknowledged()) {
                ack_yes++;
            } else {
                ack_no++;
            }
        }
        Log.d(TAG, "logAcknowledgementStatus: acknowledged=" + ack_yes +
                " unacknowledged=" + ack_no);
    }

    /**
     * Check whether the purchases have changed before posting changes.
     */
    private boolean isUnchangedPurchaseList(List<Purchase> purchasesList) {
        // TODO: Optimize to avoid updates with identical data.
        return false;
    }

    public int launchBillingFlow(Activity activity, BillingFlowParams params) {
        if (!isBillingReady()) {
            Log.e(TAG, "launchBillingFlow: BillingClient is not ready");
        }
        BillingResult billingResult = billingClient.launchBillingFlow(activity, params);
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
        return responseCode;
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, String.format("onPurchasesUpdated: %s %s",responseCode, debugMessage));
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                if (purchases == null) {
                    Log.d(TAG, "onPurchasesUpdated: null purchase list");
                    processPurchases(null);
                } else {
                    processPurchases(purchases);
                }
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(TAG, "onPurchasesUpdated: User canceled the purchase");
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                Log.i(TAG, "onPurchasesUpdated: The user already owns this item");
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                Log.e(TAG, "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just getting started, " +
                        "make sure you have configured the application correctly in the " +
                        "Google Play Console. The SKU product ID must match and the APK you " +
                        "are using must be signed with release keys."
                );
                break;
        }
    }

    /**
     * Acknowledge a purchase.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * Apps should acknowledge the purchase after confirming that the purchase token
     * has been associated with a user. This app only acknowledges purchases after
     * successfully receiving the subscription data back from the server.
     * <p>
     * Developers can choose to acknowledge purchases from a server using the
     * Google Play Developer API. The server has direct access to the user database,
     * so using the Google Play Developer API for acknowledgement might be more reliable.
     * TODO(134506821): Acknowledge purchases on the server.
     * <p>
     * If the purchase token is not acknowledged within 3 days,
     * then Google Play will automatically refund and revoke the purchase.
     * This behavior helps ensure that users are not charged for subscriptions unless the
     * user has successfully received access to the content.
     * This eliminates a category of issues where users complain to developers
     * that they paid for something that the app is not giving to them.
     */
    public void acknowledgePurchase(String purchaseToken) {
        Log.d(TAG, "acknowledgePurchase");
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        billingClient.acknowledgePurchase(params, billingResult -> {
            int responseCode = billingResult.getResponseCode();
            String debugMessage = billingResult.getDebugMessage();
            Log.d(TAG, "acknowledgePurchase: " + responseCode + " " + debugMessage);
        });
    }

    public static int getBillingMessage(int code) {
        switch (code) {
            /*case BillingClient.BillingResponse.OK:
                return R.string.billing_SERVICE_DISCONNECTED;*/
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return R.string.billing_SERVICE_DISCONNECTED;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                return R.string.billing_SERVICE_UNAVAILABLE;
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                return R.string.billing_BILLING_UNAVAILABLE;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return R.string.billing_DEVELOPER_ERROR;
            case BillingClient.BillingResponseCode.ERROR:
                return R.string.billing_ERROR;
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return R.string.billing_ITEM_UNAVAILABLE;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                return R.string.billing_USER_CANCELED;

            default: return R.string.billing_ERROR;
        }
    }
}
