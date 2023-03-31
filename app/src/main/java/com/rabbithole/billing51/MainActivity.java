package com.rabbithole.billing51;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.rabbithole.billing51.try_2.BillingClientLifecycle;
import com.rabbithole.billing51.try_2.MyApplication;

public class MainActivity extends AppCompatActivity {

    private final BillingClientLifecycle billingClientLifecycle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        billingClientLifecycle.onCreate(this);


    }

}