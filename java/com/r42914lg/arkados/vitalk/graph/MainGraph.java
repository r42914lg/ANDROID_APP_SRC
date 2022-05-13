package com.r42914lg.arkados.vitalk.graph;

import com.r42914lg.arkados.vitalk.ui.MainActivity;
import dagger.Component;

@Component(modules = {ActivityModule.class, VmProviderModule.class})
public interface MainGraph {
    void inject(MainActivity  activity);
}
