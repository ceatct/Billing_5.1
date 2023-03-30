package com.rabbithole.billing51.try_2;

import com.android.billingclient.api.ProductDetails;

import java.util.Collections;
import java.util.List;

public class Product {

    public static Product fromProductDetails(ProductDetails productDetails) {
        
    }

    public List<Offer> getOffers() {
    }

    public static class Offer{
        private PricingPhase basePhase;
        private String offerId;
        private String basePlanId;
        private List pricingPhases;

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

        private PricingPhase trialPhase;
        private PricingPhase firstPhase;

        public long costForPeriod(BillingPeriod largestPeriod) {
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

        private Product.BillingPeriod dayQuantity;

        public BillingPeriod getDayQuantity() {
            return dayQuantity;
        }

        public class Period {
            public static final Object DAY;
            public static final Object WEEK;
            public static final Object MONTH;
            public static final Object YEAR;
        }
    }

}
