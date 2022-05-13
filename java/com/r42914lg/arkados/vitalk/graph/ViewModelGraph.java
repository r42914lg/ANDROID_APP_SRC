package com.r42914lg.arkados.vitalk.graph;

import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import dagger.Component;

@Component(modules = {StorageModule.class, ConcurrentModule.class})
public interface ViewModelGraph {
    void  inject(ViTalkVM viTalkVM);
}
