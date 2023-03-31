package com.rabbithole.billing51.try_2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class GoogleOfferViewV2 extends View {

    private String productId;
    private String offerIdToken;
    /*public GoogleOfferViewV2(Context context) {

    }*/

    public GoogleOfferViewV2(Context context) {
        super(context);
    }

    public GoogleOfferViewV2(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GoogleOfferViewV2(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public String getOfferIdToken() {
        return offerIdToken;
    }

    public boolean isEnabled() {
        return true;
    }

    public void setTexts(String offerTitleFromOffer, String trialFromOffer, String firstPhaseStringFromOffer, String basePhaseFromOffer, String economyBannerStringFromOffer, String bottomBannerStringFromOffer) {
    }

    public String getProductId() {
        return productId;
    }

    public void setIds(String productId, String offerIdToken) {
    }
}
