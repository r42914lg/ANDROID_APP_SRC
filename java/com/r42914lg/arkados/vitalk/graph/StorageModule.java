package com.r42914lg.arkados.vitalk.graph;

import com.r42914lg.arkados.vitalk.model.IDataLoaderListener;

import dagger.Provides;
import dagger.Module;

@Module
public class StorageModule {
    private final IDataLoaderListener listener;

    public StorageModule(IDataLoaderListener listener) {
        this.listener = listener;
    }

    @Provides
    public IDataLoaderListener dataLoaderListener() {
        return listener;
    }
}
