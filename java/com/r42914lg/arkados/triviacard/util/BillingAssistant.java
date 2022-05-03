package com.r42914lg.arkados.triviacard.util;

import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.SUBSCRIPTION_SKU;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.r42914lg.arkados.triviacard.model.TriviaCardVM;

import java.util.ArrayList;
import java.util.List;

public class BillingAssistant {
    public static final String TAG = "LG> BillingAssistant";

    private AppCompatActivity activity;
    private TriviaCardVM triviaCardVM;

    private PurchasesUpdatedListener purchasesUpdatedListener;
    private BillingClient billingClient;
    private List<SkuDetails> skuDetailsList;
    private boolean billingClientConnected;

    public BillingAssistant(AppCompatActivity activity, TriviaCardVM  triviaCardVM) {
        this.activity = activity;
        this.triviaCardVM = triviaCardVM;

        purchasesUpdatedListener = new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (Purchase purchase : purchases) {
                        handlePurchase(purchase,true);
                    }
                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    // Handle an error caused by a user cancelling the purchase flow.
                } else {
                    // Handle any other error codes.
                }
            }
        };

        billingClient = BillingClient.newBuilder(activity)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            private boolean retried = false;

            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.

                    billingClientConnected = true;
                    List<String> skuList = new ArrayList<>();
                    skuList.add(SUBSCRIPTION_SKU);
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);

                    billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                                BillingAssistant.this.skuDetailsList = skuDetailsList;
                            }
                            if (billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).getResponseCode() != BillingClient.BillingResponseCode.OK
                                    || billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE).getResponseCode() != BillingClient.BillingResponseCode.OK) {
                                BillingAssistant.this.skuDetailsList = null;
                                if (LOG) {
                                    Log.d(TAG, ".onSkuDetailsResponse --> Feature NOT supported");
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to

                if (!retried) {
                    retried = true;
                    billingClient.startConnection(this);
                } else {
                    billingClientConnected = false;
                    if (LOG) {
                        Log.d(TAG, ".onBillingServiceDisconnected");
                    }
                }
            }
        });

        triviaCardVM.getNeedAcknowledgeFlagLiveData().observe(activity, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (s.equals("TRUE")) {
                    acknowledgePurchase(triviaCardVM.getCurrentPurchaseToken());
                }
            }
        });
    }

    public SkuDetails getSubscriptionSKU() {
        if (skuDetailsList != null &&  skuDetailsList.size() !=  0) {
            for (int i = 0; i < skuDetailsList.size(); i++) {
                if (skuDetailsList.get(i).getSku().equals(SUBSCRIPTION_SKU)) {
                    return skuDetailsList.get(i);
                }
            }
        }
        return null;
    }

    public void shutDownBillingClient() {
        billingClient.endConnection();
    }

    public void fetchPurchases()  {
        if (billingClient != null) {
            billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchases) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            handlePurchase(purchase, false);
                        }
                    } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                        // Handle an error caused by a user cancelling the purchase flow.
                    } else {
                        // Handle any other error codes.
                    }
                }
            });
        }
    }

    public void launchPurchaseFlow(SkuDetails skuDetails) {
        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();

        int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
        // Handle the result.
    }

    private void handlePurchase(Purchase purchase, boolean calledFromMainThread) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.getSkus().isEmpty()) {
            if (LOG) {
                Log.d(TAG, ".handlePurchase P_ID = " + purchase.getOrderId() + " P_TOKEN = " + purchase.getPurchaseToken());
            }
            triviaCardVM.requestCheckGPDevAPIPurchaseTokenLegitimate(purchase, calledFromMainThread);
        }
    }

    private void acknowledgePurchase(String purchaseToken) {
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();

        billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    triviaCardVM.markPurchaseTokenAcknowledged(true);
                }
            }
        });
    }
}
