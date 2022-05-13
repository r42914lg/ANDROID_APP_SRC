package com.r42914lg.arkados.vitalk.utils;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.r42914lg.arkados.vitalk.model.ViTalkVM;

import java.util.Map;

import javax.inject.Inject;

public class PermissionsHelper {
    public static final String TAG = "LG> PermissionsHelper";

    private final AppCompatActivity appCompatActivity;
    private final ViTalkVM viTalkVM;

    private final String [] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.CHANGE_NETWORK_STATE
    };

    private final ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Inject
    public PermissionsHelper(AppCompatActivity appCompatActivity, ViTalkVM viTalkVM) {
        this.appCompatActivity = appCompatActivity;
        this.viTalkVM = viTalkVM;

        requestPermissionLauncher =  appCompatActivity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> result) {
                        if (result.containsValue(false)) {
                            viTalkVM.onPermissionsCheckFailed();
                            if (LOG) {
                                Log.d(TAG, ".onActivityResult --> NOT ALL permissions granted");
                            }
                        } else {
                            viTalkVM.onPermissionsCheckPassed();
                            if (LOG) {
                                Log.d(TAG, ".onActivityResult --> ALL permissions granted");
                            }
                        }
                    }
                });

        if (LOG) {
            Log.d(TAG, "  instance created");
        }

        checkPermissions();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.CHANGE_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {

            if (LOG) {
                Log.d(TAG, ".checkPermissions PASSED, calling onPermissionsCheckPassed() on VM");
            }

            viTalkVM.onPermissionsCheckPassed();
        } else {
            if (LOG) {
                Log.d(TAG, ".checkPermissions --> requesting permissions via activity");
            }
            requestPermissionLauncher.launch(permissions);
        }
    }
}
