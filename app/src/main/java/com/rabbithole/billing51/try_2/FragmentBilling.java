package com.rabbithole.billing51.try_2;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.rabbithole.billing51.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FragmentBilling extends Fragment {
    private View groupByProgress;
    private View groupByPremium;
    private View groupByError;
    private View groupByOffers;
    private View rootContainer;
    private BillingViewModel viewModel;
    private final Observer<BillingViewModel.State> stateObserver = state -> {
        switch (state) {
            case LOADING:
                onLoadingState();
                break;
            case GOT_INFO:
                onGotBillingInfoState();
                break;
            case SUBSCRIBED:
                onSubscribedState();
                break;
            case ERROR:
                onBillingErrorState();
                break;
        }
    };

    private final Observer<List<Product>> productObserver = this::handleProducts;

    public FragmentBilling() {
        super(R.layout.fragment_paywall_layout);
    }

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BillingViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        initUI(view);
        subscribeToLiveData();
    }

    private void subscribeToLiveData() {
        viewModel.getLiveState().observe(getViewLifecycleOwner(), stateObserver);
        viewModel.getLiveProducts().observe(getViewLifecycleOwner(), productObserver);
    }

    private void initUI(View rootView) {
        groupByProgress = rootView.findViewById(R.id.group_progress);
        groupByPremium = rootView.findViewById(R.id.group_premium);
        groupByError = rootView.findViewById(R.id.group_error);
        groupByOffers = rootView.findViewById(R.id.group_offers);
        rootContainer = rootView.findViewById(R.id.root_container);
        rootView.findViewById(R.id.btn_try_again).setOnClickListener(viewModel.getTryAgainClickListener());
        rootView.findViewById(R.id.btn_try_again2).setOnClickListener(viewModel.getTryAgainClickListener());
        rootView.findViewById(R.id.btn_premium_subscribe)
                .setOnClickListener(v -> {
                    String productId = getSelectedOfferProductId();
                    String token = getSelectedOfferToken();
                    viewModel.onOfferClick(productId, token, requireActivity());
                });
    }

    private String getSelectedOfferToken() {
        LinearLayout offerContainer = rootContainer.findViewById(R.id.ll_offers_container);
        for (int i = 0; i < offerContainer.getChildCount(); i++) {
            View view = offerContainer.getChildAt(i);
            if (view instanceof GoogleOfferViewV2) {
                GoogleOfferViewV2 offerView = (GoogleOfferViewV2) view;
                if (offerView.isEnabled()) {
                    return offerView.getOfferIdToken();
                }
            }
        }
        return "";
    }

    private String getSelectedOfferProductId() {
        LinearLayout offerContainer = rootContainer.findViewById(R.id.ll_offers_container);
        for (int i = 0; i < offerContainer.getChildCount(); i++) {
            View view = offerContainer.getChildAt(i);
            if (view instanceof GoogleOfferViewV2) {
                GoogleOfferViewV2 offerView = (GoogleOfferViewV2) view;
                if (offerView.isEnabled()) {
                    return offerView.getProductId();
                }
            }
        }
        return "";
    }

    private void onSubscribedState() {
        groupByPremium.setVisibility(View.VISIBLE);
        groupByError.setVisibility(View.INVISIBLE);
        groupByProgress.setVisibility(View.INVISIBLE);
        groupByOffers.setVisibility(View.GONE);
    }

    private void onGotBillingInfoState() {
        if (viewModel.isSubscribed()) {
            onSubscribedState();
            return;
        }
        groupByPremium.setVisibility(View.INVISIBLE);
        groupByError.setVisibility(View.INVISIBLE);
        groupByProgress.setVisibility(View.INVISIBLE);
        groupByOffers.setVisibility(View.VISIBLE);
        if (viewModel.getLiveProducts().getValue() == null || viewModel.getLiveProducts().getValue().isEmpty()) {
            rootContainer.findViewById(R.id.ll_no_offers).setVisibility(View.VISIBLE);
        } else {
            rootContainer.findViewById(R.id.ll_no_offers).setVisibility(View.INVISIBLE);
        }
    }

    private void onLoadingState() {
        groupByError.setVisibility(View.INVISIBLE);
        groupByProgress.setVisibility(View.VISIBLE);
        groupByPremium.setVisibility(View.INVISIBLE);
        groupByOffers.setVisibility(View.INVISIBLE);
    }

    private void onBillingErrorState() {
        groupByError.setVisibility(View.VISIBLE);
        groupByProgress.setVisibility(View.INVISIBLE);
        groupByPremium.setVisibility(View.INVISIBLE);
        groupByOffers.setVisibility(View.INVISIBLE);
    }

    private void handleProducts(List<Product> products) {
        if (products.isEmpty()) {
            if (groupByOffers.getVisibility() == View.VISIBLE) {
                rootContainer.findViewById(R.id.ll_no_offers).setVisibility(View.VISIBLE);
            }
            return;
        }
        rootContainer.findViewById(R.id.ll_no_offers).setVisibility(View.INVISIBLE);
        LinearLayout offerContainer = rootContainer.findViewById(R.id.ll_offers_container);
        offerContainer.removeAllViewsInLayout();
        List<Product.Offer> sortedOffers = viewModel.getSortedOffers(products);

        if (sortedOffers.isEmpty()) {
            if (groupByOffers.getVisibility() == View.VISIBLE) {
                rootContainer.findViewById(R.id.ll_no_offers).setVisibility(View.VISIBLE);
            }
            return;
        }

        for (Product.Offer offer : sortedOffers) {
            if (getContext() == null) return;
            GoogleOfferViewV2 offerView = new GoogleOfferViewV2(getContext());
            offerView.setTexts(
                    viewModel.getOfferTitleFromOffer(offer),
                    viewModel.getTrialFromOffer(offer),
                    viewModel.getFirstPhaseStringFromOffer(offer),
                    viewModel.getBasePhaseFromOffer(offer),
                    viewModel.getEconomyBannerStringFromOffer(offer, sortedOffers),
                    viewModel.getBottomBannerStringFromOffer(offer)
            );
            offerView.setIds(offer.getProductId(), offer.getOfferIdToken());
            offerView.setOnClickListener(v -> {
                String offerToken = offerView.getOfferIdToken();
                for (int i = 0; i < offerContainer.getChildCount(); i++) {
                    View view = offerContainer.getChildAt(i);
                    if (view instanceof GoogleOfferViewV2) {
                        GoogleOfferViewV2 offerView1 = (GoogleOfferViewV2) view;
                        offerView1.setEnabled(offerView1.getOfferIdToken().equals(offerToken));
                    }
                }
            });
            offerContainer.addView(offerView);
            offerView.setEnabled(offerContainer.getChildCount() == 1);//Activate the first offer
        }
    }

    @Override
    public void onDestroyView() {
        unsubscribeFromLiveData();
        super.onDestroyView();
    }

    private void unsubscribeFromLiveData() {
        viewModel.getLiveState().removeObserver(stateObserver);
        viewModel.getLiveProducts().removeObserver(productObserver);
    }
}
