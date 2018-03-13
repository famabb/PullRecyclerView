package com.famabb.simple;

import android.app.Application;

import com.famabb.pull.PullRecyclerView;

/**
 * Created by ${ChenJC} on 2018/3/12.
 */

public class AppContext extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PullRecyclerView.PullSysConfig config = new PullRecyclerView
                .PullSysConfig.Builder()
                .moreViewClass(SimpleRefreshMoreView.class)
                .refreshViewClass(SimpleRefreshHeadView.class)
                .build();
        PullRecyclerView.setPullSysConfig(config);
    }
}
