package com.rabbithole.billing51;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.billingclient.api.BillingClient;
import com.rabbithole.billing51.try_1.Content;
import com.rabbithole.billing51.try_1.PurchaseListener;
import com.rabbithole.billing51.try_1.Sub;

public class MainFragment extends Fragment {

    BillingClient billingClient;

    public MainFragment() {
        super(R.layout.fragment_main);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI(view);
    }

    private void initUI(View rootView) {
        View buy = rootView.findViewById(R.id.buy);
        View buy2 = rootView.findViewById(R.id.buy2);
        View price = rootView.findViewById(R.id.price);

        billingClient = BillingClient.newBuilder(getContext())
                .setListener(new PurchaseListener())
                .enablePendingPurchases()
                .build();

        String prc = Sub.getPrice(billingClient, "");
        String prc2 = Content.getPrice(billingClient, "");

        buy.setOnClickListener(view -> {Sub.buySub(billingClient, getActivity());
            Toast.makeText(getContext(), prc, Toast.LENGTH_LONG).show();
        });

        buy2.setOnClickListener(view -> {Content.buyItem(billingClient, getActivity());
            Toast.makeText(getContext(), prc2, Toast.LENGTH_LONG).show();
        });
    }

}