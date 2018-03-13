package com.famabb.pull;

public interface PullListener {
    void onRefresh();
    void onLoadMore();
}
