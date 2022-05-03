package com.r42914lg.arkados.vitalk.model;

public class FavoritesEvent {
    private boolean enableFavorites;
    private boolean favoritesChecked;

    public boolean checkFavoritesChecked() { return favoritesChecked; }
    public boolean needEnableFavorites() { return enableFavorites; }
    public void setFavoritesChecked(boolean favoritesChecked) { this.favoritesChecked = favoritesChecked; }
    public void setEnableFavorites(boolean enableFavorites) { this.enableFavorites = enableFavorites; }
}
