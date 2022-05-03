package com.r42914lg.arkados.vitalk.ui;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public interface ICoreFrame {
    void startProgressOverlay();
    void stopProgressOverlay();
    void showFavoriteIcon(boolean showIfTrue);
    void renderMenuItems();
    void showFab(boolean flag);
    void showTabOneMenuItems(boolean flag);
    void updateUI(GoogleSignInAccount account);
    void askRatings();
}
