package com.famabb.simple;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.famabb.pull.PullListener;
import com.famabb.pull.PullRecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity implements PullListener {
    private Context mContext;
    private PullRecyclerView mPull;
    private List<String> mData;
    private TestAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        mData = new ArrayList<>();
        mPull = findViewById(R.id.pull);
        mAdapter = new TestAdapter();
        mPull.setHeadRefreshView(new SimpleRefreshHeadView(mContext))
                .setMoreRefreshView(new SimpleRefreshMoreView(mContext))
                .setUseLoadMore(true)
                .setUseRefresh(true)
                .setPullLayoutManager(new LinearLayoutManager(this))
                .setPullListener(this)
                .setPullItemAnimator(null)
                .build(mAdapter);
        loadData(100L);
    }

    @Override
    public void onRefresh() {
        loadData(1000L);
    }

    @Override
    public void onLoadMore() {
        loadData(1000L);
    }

    private void loadData(final long sleep) {
        Observable.create(new Observable.OnSubscribe<String[]>() {
            @Override
            public void call(Subscriber<? super String[]> subscriber) {
                SystemClock.sleep(sleep);
                subscriber.onNext(new String[new Random().nextInt(2)]);
                subscriber.onCompleted();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<String[]>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String[] arr) {
                        mData.addAll(Arrays.asList(arr));
                        mAdapter.notifyItemChanged(mData.size() - arr.length, arr.length);
                        mPull.onComplete(arr.length > 0);
                    }
                });
    }

    private class TestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TestViewHolder(mContext);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TestViewHolder) {
                ((TestViewHolder) holder).setTip(position);
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        private class TestViewHolder extends RecyclerView.ViewHolder {
            TextView tv;

            public TestViewHolder(Context context) {
                super(LayoutInflater.from(context).inflate(R.layout.item_view, null));
                tv = itemView.findViewById(R.id.tv);
            }

            private void setTip(int index) {
                tv.setText(String.valueOf(index));
            }
        }
    }
}
