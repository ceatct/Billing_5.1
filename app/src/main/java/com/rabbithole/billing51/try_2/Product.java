package com.rabbithole.billing51.try_2;

import com.android.billingclient.api.ProductDetails;

import java.util.Collections;
import java.util.List;

public class Product {

    private List<Offer> offers;
    private static Product fromProductDetails;

    public static Product fromProductDetails(ProductDetails productDetails) {
        return fromProductDetails;
    }

    public List<Offer> getOffers() {
        return offers;
    }

    /*public void setOffers(List<Offer> offers) {
        this.offers = offers;
    }*/

    public static class Offer{
        private PricingPhase basePhase;
        private String offerId;
        private String basePlanId;
        private List pricingPhases;
        private PricingPhase trialPhase;
        private PricingPhase firstPhase;
        public long costForPeriod;
        private String productId;
        private String offerIdToken;

        public void setBasePhase(PricingPhase basePhase) {
            this.basePhase = basePhase;
        }

        public long getCostForPeriod() {
            return costForPeriod;
        }

        public void setCostForPeriod(long costForPeriod) {
            this.costForPeriod = costForPeriod;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getOfferIdToken() {
            return offerIdToken;
        }

        public void setOfferIdToken(String offerIdToken) {
            this.offerIdToken = offerIdToken;
        }

        private List<String> ids;

        public List<String> getIds() {
            return ids;
        }

        public void setIds(List<String> ids) {
            this.ids = ids;
        }

        public List getPricingPhases() {
            return pricingPhases;
        }

        public void setPricingPhases(String pricingPhases) {
            this.pricingPhases = Collections.singletonList(pricingPhases);
        }

        public String getOfferId() {
            return offerId;
        }

        public void setOfferId(String offerId) {
            this.offerId = offerId;
        }

        public String getBasePlanId() {
            return basePlanId;
        }

        public void setBasePlanId(String basePlanId) {
            this.basePlanId = basePlanId;
        }

        public PricingPhase getBasePhase() {
            return basePhase;
        }

        public long costForPeriod(BillingPeriod largestPeriod) {
            return costForPeriod;
        }

        public void setPricingPhases(List pricingPhases) {
            this.pricingPhases = pricingPhases;
        }

        public PricingPhase getTrialPhase() {
            return trialPhase;
        }

        public void setTrialPhase(PricingPhase trialPhase) {
            this.trialPhase = trialPhase;
        }

        public PricingPhase getFirstPhase() {
            return firstPhase;
        }

        public void setFirstPhase(PricingPhase firstPhase) {
            this.firstPhase = firstPhase;
        }




    }

    public static class PricingPhase{

        private BillingPeriod billingPeriod;
        private String formattedPrice;

        public BillingPeriod getBillingPeriod() {
            return billingPeriod;
        }

        public String getFormattedPrice() {
            return formattedPrice;
        }

        public void setFormattedPrice(String formattedPrice) {
            this.formattedPrice = formattedPrice;
        }
    }

    public static class BillingPeriod{
        private Period period;
        private int count;

        public BillingPeriod(int i, Object day) {
        }

        public Period getPeriod() {
            return period;
        }

        public int getCount() {
            return count;
        }

        private Integer dayQuantity;

        public Integer getDayQuantity() {
            return dayQuantity;
        }

        public static class Period {
            public static final Object DAY = 1;
            public static final Object WEEK = 7;
            public static final Object MONTH = 30;
            public static final Object YEAR = 365;
        }
    }

}
