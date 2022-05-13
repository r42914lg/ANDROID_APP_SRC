package com.r42914lg.arkados.vitalk.graph;

import androidx.appcompat.app.AppCompatActivity;
import dagger.Module;
import dagger.Provides;

@Module
public class ActivityModule {
    private final AppCompatActivity appCompatActivity;

    public ActivityModule(AppCompatActivity appCompatActivity) {
        this.appCompatActivity = appCompatActivity;
    }

    @Provides
    public AppCompatActivity provideActivity() {
        return appCompatActivity;
    }
}
