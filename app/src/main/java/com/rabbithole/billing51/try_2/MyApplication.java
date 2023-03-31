package com.rabbithole.billing51.try_2;

import android.app.Application;

public class MyApplication extends Application {

    public BillingClientLifecycle getBillingClientLifecycle(){
        return BillingClientLifecycle.getInstance(this);
    }

}
