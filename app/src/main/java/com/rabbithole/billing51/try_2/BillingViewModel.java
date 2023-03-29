package com.rabbithole.billing51.try_2;

import static android.os.Build.VERSION_CODES.R;

import android.app.Application;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BillingViewModel extends AndroidViewModel {
    MutableLiveData<State> liveState = new MutableLiveData<>(State.LOADING);
    final MutableLiveData<List<Product>> mutableProductList = new MutableLiveData<>(new ArrayList<>());
    final Observer<Map<String, ProductDetails>> productObserver = stringProductDetailsMap -> {
        mutableProductList.postValue(getProducts(stringProductDetailsMap));
        liveState.postValue(State.GOT_INFO);
    };
    final BillingClientLifecycle billing;

    final Observer<List<Purchase>> purchaseObserver = purchases -> {
        if (purchases != null && !purchases.isEmpty()) {
            liveState.postValue(State.SUBSCRIBED);
        }
    };

    final Observer<BillingClientLifecycle.ConnectionState> connectionObserver = connectionState -> {
        if (connectionState == BillingClientLifecycle.ConnectionState.ERROR) {
            liveState.postValue(State.ERROR);
            if (connectionState.getMsg() != null) {
                Log.e("BillingViewModel", connectionState.getMsg());
            }
        }
    };

    @NonNull
    public String getOfferTitleFromOffer(Product.Offer offer) {
        String title = "";
        Product.PricingPhase basePhase = offer.getBasePhase();
        if (basePhase != null) {
            title = getTitleFromPeriod(basePhase.getBillingPeriod());
        }
        return title;
    }

    @NonNull
    private String getTitleFromPeriod(Product.BillingPeriod billingPeriod) {
        return getPeriodString(billingPeriod.getPeriod(), billingPeriod.getCount()).toUpperCase(Locale.ROOT);
    }

    private Product.Offer getMostExpansiveOffer(List<Product.Offer> offers) {
        Product.BillingPeriod largestPeriod = getLargestOfferPeriod(offers);
        if (offers.isEmpty()) return null;
        Product.Offer expansiveOffer = offers.get(0);//Taking first offer for comparing
        for (Product.Offer offer: offers) {
            long cost = offer.costForPeriod(largestPeriod);
            long costEx = expansiveOffer.costForPeriod(largestPeriod);
            if (cost > costEx) {
                expansiveOffer = offer;
            }
        }
        return expansiveOffer;
    }

    private Product.BillingPeriod getLargestOfferPeriod(List<Product.Offer> offers) {
        Product.BillingPeriod billingPeriod = new Product.BillingPeriod(1, Product.BillingPeriod.Period.DAY);
        for (Product.Offer offer: offers) {
            Product.PricingPhase basePhase = offer.getBasePhase();
            if (basePhase != null && basePhase.getBillingPeriod().getDayQuantity() > billingPeriod.getDayQuantity()) {
                billingPeriod = basePhase.getBillingPeriod();
            }
        }
        return billingPeriod;
    }

    private String getPeriodString(Product.BillingPeriod.Period period, int count) {
        String periodString = "none";
        @PluralsRes int pluralId = 0;
        switch (period) {
            case DAY:
                pluralId = R.plurals.day;
                break;
            case WEEK:
                pluralId = R.plurals.week;
                break;
            case MONTH:
                pluralId = R.plurals.month;
                break;
            case YEAR:
                pluralId = R.plurals.year;
                break;
        }
        if (pluralId != 0) {
            periodString = getApplication().getResources().getQuantityString(pluralId, count);
            //periodString = getApplication().getString(res);
        }
        return String.format(periodString, count);
    }

    @Nullable
    public String getTrialFromOffer(Product.Offer offer) {
        Product.PricingPhase phase = offer.getTrialPhase();
        if (phase == null) return null;
        return String.format(
                getApplication().getString(R.string.free_trial_template),
                getPeriodString(
                        phase.getBillingPeriod().getPeriod(),
                        phase.getBillingPeriod().getCount()));
    }

    public String getFirstPhaseStringFromOffer(Product.Offer offer) {
        Product.PricingPhase phase = offer.getFirstPhase();
        if (phase == null) return null;

        return String.format(
                getApplication().getString(R.string.price_for_the_first_time_template),
                phase.getFormattedPrice(),
                getShortPeriodString(phase.getBillingPeriod()).toLowerCase(Locale.ROOT),
                getPeriodString(phase.getBillingPeriod().getPeriod(), phase.getBillingPeriod().getCount()));
    }
    @NonNull
    public String getBasePhaseFromOffer(Product.Offer offer) {
        String formattedPrice = "none";
        Product.BillingPeriod period;
        Product.PricingPhase basePhase = offer.getBasePhase();
        if (basePhase == null) {
            return formattedPrice;
        } else {
            formattedPrice = basePhase.getFormattedPrice();
            period = basePhase.getBillingPeriod();
        }

        if (offer.getPricingPhases().size() > 1) {
            return String.format(getApplication().getString(R.string.after_price_template), formattedPrice, getShortPeriodString(period));
        }
        return formattedPrice;//Then $23.99 per year
    }

    @NonNull
    private String getShortPeriodString(Product.BillingPeriod period) {
        String periodString = getPeriodString(period.getPeriod(), period.getCount()).toLowerCase(Locale.ROOT);
        if (period.getCount() == 1) {
            periodString = periodString.replace("1", "").replace(" ", "");
        }
        return periodString;
    }

    public String getEconomyBannerStringFromOffer(Product.Offer offer, List<Product.Offer> allOffers) {
        int economy = getEconomyPercent(offer, allOffers);
        return economy > 0 ? String.format(getApplication().getString(R.string.save_template), economy + "%") : null;
    }

    public String getBottomBannerStringFromOffer(Product.Offer offer) {
        Product.PricingPhase phase = offer.getTrialPhase();
        if (phase == null) return null;

        return String.format(
                getApplication().getString(R.string.try_first_for_free_template),
                getPeriodString(phase.getBillingPeriod().getPeriod(), phase.getBillingPeriod().getCount())
                );
    }

    private int getEconomyPercent(Product.Offer offer, List<Product.Offer> allOffers) {
        int economy = 0;
        Product.BillingPeriod longestPeriod = getLargestOfferPeriod(allOffers);
        Product.Offer expansiveOffer = getMostExpansiveOffer(allOffers);
        if (expansiveOffer != null) {
            long expansiveCost = expansiveOffer.costForPeriod(longestPeriod);
            long currentCost = offer.costForPeriod(longestPeriod);
            if (currentCost < expansiveCost) {
                economy = 100 - (int) ((currentCost * 100)/expansiveCost);
            }
        }
        return economy;
    }

    public List<Product.Offer> getSortedOffers(List<Product> products) {
        List<Product.Offer> list = new ArrayList<>();
        for (Product product: products) {
            for (Product.Offer offer : product.getOffers()) {
                if (isRightOffer(offer, product.getOffers())) {
                    list.add(offer);
                }
            }
        }
        int offersCount = list.size();
        List<Product.Offer> sortedList = new ArrayList<>();
        for (int i = 0; i < offersCount; i++) {
            final Product.Offer mostExpansiveFromList = getMostExpansiveOffer(list);
            sortedList.add(mostExpansiveFromList);
            list.remove(mostExpansiveFromList);
        }
        Collections.reverse(sortedList);

        return sortedList;
    }

    private boolean isRightOffer(Product.Offer offer, List<Product.Offer> offers) {
        //TODO: Create checker!!!
        return offer.getOfferId() != null || offer.getPricingPhases().size() != 1 || !hasSpecOfferWithBasePlan(offer.getBasePlanId(), offers);
    }

    private boolean hasSpecOfferWithBasePlan(String basePlanId, List<Product.Offer> offers) {
        for (Product.Offer offer: offers) {
            if (offer.getBasePlanId().equals(basePlanId) && offer.getOfferId() != null) {
                return true;
            }
        }
        return false;
    }

    public void onOfferClick(String productId, String offerToken, FragmentActivity activity) {
        ProductDetails productDetails = getProductDetailsById(productId);
        if (productDetails == null) return;
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build());
        BillingFlowParams params = BillingFlowParams
                .newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();
        BillingClientLifecycle.getInstance(getApplication()).launchBillingFlow(activity, params);
    }

    public boolean isSubscribed() {
        return billing.isSubscribed();
    }

    public enum State {
        LOADING, SUBSCRIBED, GOT_INFO, ERROR
    }

    public BillingViewModel(@NonNull Application application) {
        super(application);
        billing = BillingClientLifecycle.getInstance(getApplication());
        billing.productsWithProductDetails.observeForever(productObserver);
        billing.purchaseUpdateEvent.observeForever(purchaseObserver);
        billing.connectionLiveState.observeForever(connectionObserver);
    }

    @Override
    protected void onCleared() {
        billing.productsWithProductDetails.removeObserver(productObserver);
        billing.purchaseUpdateEvent.removeObserver(purchaseObserver);
        billing.connectionLiveState.removeObserver(connectionObserver);
        super.onCleared();
    }

    private List<Product> getProducts(Map<String, ProductDetails> stringProductDetailsMap) {
        List<Product> productList = new ArrayList<>();
        for (String productId: LIST_OF_PRODUCTS) {
            ProductDetails productDetails = stringProductDetailsMap.get(productId);
            if (productDetails != null) {
                productList.add(Product.fromProductDetails(productDetails));
            }
        }
        return productList;
    }

    private ProductDetails getProductDetailsById(String productId) {
        Map<String, ProductDetails> productDetailsMap = BillingClientLifecycle.getInstance(getApplication()).productsWithProductDetails.getValue();
        if (productDetailsMap != null && !productDetailsMap.isEmpty()) {
            return productDetailsMap.get(productId);
        } else return null;
    }

    public View.OnClickListener getTryAgainClickListener() {
        return v -> {
            liveState.postValue(State.LOADING);
            BillingClientLifecycle.getInstance(getApplication()).startBillingConnection();
        };
    }

    public LiveData<List<Product>> getLiveProducts() {
        return this.mutableProductList;
    }

    public LiveData<State> getLiveState() {
        return this.liveState;
    }

    public void checkBillingState() {
        if (!BillingClientLifecycle.getInstance(getApplication()).isBillingReady()) {
            liveState.postValue(State.ERROR);
        }
    }
}
