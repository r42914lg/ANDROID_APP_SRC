package com.r42914lg.arkados.vitalk.ui;

import com.r42914lg.arkados.vitalk.model.WorkItemVideo;
import java.util.List;

public interface IViTalkWorkItems {
    void onAddRowsToAdapter(List<WorkItemVideo> workItemVideoList);
    void notifyAdapterIconLoaded(int position);
    void onFavoritesChanged();
}
