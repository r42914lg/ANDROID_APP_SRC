package com.r42914lg.arkados.vitalk.graph;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import com.r42914lg.arkados.vitalk.model.ViTalkVM;
import dagger.Module;
import dagger.Provides;

@Module
public class VmProviderModule {
    public static final String TAG = "LG> VmProviderModule";

    private final ViewModelStoreOwner viewModelStoreOwner;

    public VmProviderModule(ViewModelStoreOwner viewModelStoreOwner) {
        this.viewModelStoreOwner = viewModelStoreOwner;
    }

    @Provides
    public ViTalkVM provideViewModel() {
        ViTalkVM viTalkVM = new ViewModelProvider(viewModelStoreOwner).get(ViTalkVM.class);

        if (LOG) {
            Log.d(TAG, ".provideViewModel for viewModelStoreOwner --> " + viewModelStoreOwner + " instance created --> " + viTalkVM);
        }

        return viTalkVM;
    }
}
