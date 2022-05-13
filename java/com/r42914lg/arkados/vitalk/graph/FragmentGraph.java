package com.r42914lg.arkados.vitalk.graph;

import com.r42914lg.arkados.vitalk.ui.FirstFragment;
import com.r42914lg.arkados.vitalk.ui.SecondFragment;
import com.r42914lg.arkados.vitalk.ui.ThirdFragment;
import com.r42914lg.arkados.vitalk.ui.WorkItemAdapter;

import dagger.Component;

@Component(modules = {VmProviderModule.class})
public interface FragmentGraph {
    void inject(FirstFragment fragment);
    void inject(SecondFragment fragment);
    void inject(ThirdFragment fragment);
    void inject(WorkItemAdapter adapter);
}
