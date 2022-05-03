package com.r42914lg.arkados.triviacard.model;

public class UI_VM_EVENT {
    public static final int TYPE_FAVORITES_STATE = 1;
    public static final int TYPE_ADS_STATE = 2;
    public static final int TYPE_TOAST = 3;
    public static final int TYPE_GOOGLE_DIALOG = 4;

    private String dialogTitle;
    private String dialogMessage;
    private String toastText;
    private boolean enableFavorites;
    private boolean favoritesChecked;
    private boolean disableAds;
    private boolean hideAds;
    private int eventType;

    public void setToastText(String toastText) { this.toastText = toastText; }
    public String getToastText() {
        String toReturn = toastText;
        toastText = null;
        return toReturn;
    }

    public void setDialogText(String dialogTitle, String dialogMessage) {
        this.dialogTitle = dialogTitle;
        this.dialogMessage = dialogMessage;
    }

    public String getDialogTitle() { return dialogTitle; }
    public String getDialogMessage() { return dialogMessage; }

    public void setType(int eventType) {
        this.eventType = eventType;
    }
    public int getEventType() {
        return eventType;
    }
    public boolean checkFavoritesChecked() {
        return favoritesChecked;
    }
    public boolean needEnableFavorites() {
        return enableFavorites;
    }
    public boolean checkDisableAds() { return disableAds; }
    public boolean checkHideAds() { return hideAds; }
    public void setDisableAds(boolean disableAds) { this.disableAds = disableAds; }
    public void setHideAds(boolean hideAds) { this.hideAds = hideAds; }
    public void setFavoritesChecked(boolean favoritesChecked) { this.favoritesChecked = favoritesChecked; }
    public void setEnableFavorites(boolean enableFavorites) { this.enableFavorites = enableFavorites; }
}
