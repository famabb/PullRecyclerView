package com.famabb.pull;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * http://blog.csdn.net/a807891033
 */
public class PullRecyclerView extends RecyclerView {
    public static final int HEAD_STATE_NORMAL = 0;
    public static final int HEAD_STATE_RELEASE_TO_REFRESH = 1;
    public static final int HEAD_STATE_REFRESHING = 2;
    public static final int HEAD_STATE_DONE = 3;
    public static final int HEAD_STATE_FAIL = 4;

    public static final int MORE_STATE_NORMAL = 0;
    public static final int MORE_STATE_REFRESHING = 1;
    public static PullSysConfig mSysConfig;
    private Context mContext;
    /**
     * 摩擦力
     */
    private static final int DRAG_RATE = 2;
    private final AdapterDataObserver dataObserver = new DataObserver();
    private PullToRefreshRecyclerViewAdapter pullToRefreshRecyclerViewAdapter;
    private PullListener pullListener;
    private AbRefreshHeadView mHeadRefreshView;//下拉刷新view
    private AbRefreshMoreView mMoreRefreshView;//上拉刷新view
    private int mHeadRefreshState = HEAD_STATE_NORMAL;
    private int mMoreRefreshState = MORE_STATE_NORMAL;
    private boolean isCanLoadMore = false;//上拉
    private boolean isCanRefresh = false;//下拉

    public static void setPullSysConfig(PullSysConfig config) {
        mSysConfig = config;
    }

    private boolean isExistRefreshView() {
        return mHeadRefreshView != null;
    }

    private boolean isExistLoadMoreView() {
        return mMoreRefreshView != null;
    }

    public boolean isCanRefresh() {
        return isCanRefresh && isExistRefreshView();
    }

    public boolean isCanLoadMore() {
        return isCanLoadMore && isExistLoadMoreView();
    }

    //是否正在加载更多
    public boolean isLoadMore() {
        return mMoreRefreshState == MORE_STATE_REFRESHING && isExistLoadMoreView();
    }

    //是否正在刷新
    public boolean isRefresh() {
        return mHeadRefreshState == HEAD_STATE_REFRESHING && isExistRefreshView();
    }

    //设置头部刷新释放可用
    public PullRecyclerView setUseRefresh(boolean refresh) {
        isCanRefresh = refresh;
        return this;
    }

    //设置底部刷新是否可用
    public PullRecyclerView setUseLoadMore(boolean loadMore) {
        if (isExistLoadMoreView()) {
            int visib = mMoreRefreshView.getVisibility();
            if (visib != (loadMore ? VISIBLE : GONE)) {
                mMoreRefreshView.setVisibility(loadMore ? VISIBLE : GONE);
            }
        }
        isCanLoadMore = loadMore;
        return this;
    }

    //动画
    public PullRecyclerView setPullItemAnimator(ItemAnimator animator) {
        setItemAnimator(animator);
        return this;
    }

    //头部刷新控件
    public PullRecyclerView setHeadRefreshView(AbRefreshHeadView headView) {
        this.mHeadRefreshView = headView;
        return this;
    }

    //底部刷新控件
    public PullRecyclerView setMoreRefreshView(AbRefreshMoreView moreRefreshView) {
        this.mMoreRefreshView = moreRefreshView;
        return this;
    }

    //上下拉刷新监听
    public PullRecyclerView setPullListener(PullListener pullListener) {
        this.pullListener = pullListener;
        return this;
    }


    public PullRecyclerView setPullLayoutManager(LayoutManager layout) {
        setLayoutManager(layout);
        return this;
    }

    //设置适配器
    public void build(Adapter adapter) {
        setAdapter(adapter);
    }

    public PullRecyclerView(Context context) {
        this(context, null);
    }

    public PullRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        initPullConfig();
        setUseLoadMore(isCanLoadMore());
    }

    private void initPullConfig() {
        if (mSysConfig != null) {
            try {
                Class<? extends AbRefreshHeadView> refreshCls = mSysConfig.getRefreshViewClass();
                if (refreshCls != null) {
                    Constructor cst = refreshCls.getDeclaredConstructor(Context.class);
                    mHeadRefreshView = (AbRefreshHeadView) cst.newInstance(mContext);
                }
                Class<? extends AbRefreshMoreView> moreCls = mSysConfig.getMoreViewClass();
                if (moreCls != null) {
                    Constructor cst = moreCls.getDeclaredConstructor(Context.class);
                    mMoreRefreshView = (AbRefreshMoreView) cst.newInstance(mContext);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        pullToRefreshRecyclerViewAdapter = new PullToRefreshRecyclerViewAdapter(adapter);
        super.setAdapter(pullToRefreshRecyclerViewAdapter);
        adapter.registerAdapterDataObserver(dataObserver);
        dataObserver.onChanged();
    }

    @Override
    public Adapter getAdapter() {
        if (pullToRefreshRecyclerViewAdapter != null) {
            return pullToRefreshRecyclerViewAdapter.getAdapter();
        }
        return null;
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
        if (pullToRefreshRecyclerViewAdapter != null) {
            if (layout instanceof GridLayoutManager) {
                final GridLayoutManager gridManager = ((GridLayoutManager) layout);
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return (pullToRefreshRecyclerViewAdapter.isLoadMoreFooter(position)
                                || pullToRefreshRecyclerViewAdapter.isRefreshHeader(position))
                                ? gridManager.getSpanCount() : 1;
                    }
                });

            }
        }
    }

    private float lastY = -1;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastY = e.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isCanRefresh) {
                    lastY = lastY == -1 ? e.getRawY() : lastY;
                    float moveY = e.getRawY() - lastY;
                    lastY = e.getRawY();
                    if (isCanRefresh() && mHeadRefreshView.getVisibleHeight() == 0 && moveY < 0) {
                        return super.onTouchEvent(e);
                    }
                    if (isOnTop() && !isRefresh()) {
                        onMove((int) (moveY / DRAG_RATE));
                        return false;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                checkRefresh();
                break;
        }
        return super.onTouchEvent(e);
    }

    /**
     * 触摸事件结束后检查是否需要刷新
     */
    private void checkRefresh() {
        if (isCanRefresh()) {
            if (mHeadRefreshView.getVisibleHeight() <= 0) {
                return;
            }
            if (mHeadRefreshState == HEAD_STATE_NORMAL) {
                mHeadRefreshView.smoothScrollTo(0);
                mHeadRefreshState = HEAD_STATE_DONE;
            } else if (mHeadRefreshState == HEAD_STATE_RELEASE_TO_REFRESH) {
                setRefreshState(HEAD_STATE_REFRESHING);
            }
        }
    }

    //判断手势状态
    private void onMove(int move) {
        if (isCanRefresh() && !isRefresh()) {
            int newVisibleHeight = mHeadRefreshView.getVisibleHeight() + move;
            if (newVisibleHeight >= mHeadRefreshView.getRefreshHeight() && mHeadRefreshState != HEAD_STATE_RELEASE_TO_REFRESH) {
                mHeadRefreshState = HEAD_STATE_RELEASE_TO_REFRESH;
                mHeadRefreshView.onReleaseState();
            }
            if (newVisibleHeight < mHeadRefreshView.getRefreshHeight() && mHeadRefreshState != HEAD_STATE_NORMAL) {
                mHeadRefreshState = HEAD_STATE_NORMAL;
                mHeadRefreshView.onPullingDown();
            }
            mHeadRefreshView.setVisibleHeight(mHeadRefreshView.getVisibleHeight() + move);
        }
    }

    //设置要执行状态
    private void setRefreshState(int state) {
        if (isExistRefreshView() && mHeadRefreshState != state) {
            switch (state) {
                case HEAD_STATE_REFRESHING://切换到刷新状态
                    mHeadRefreshView.onRefreshing();
                    mHeadRefreshView.smoothScrollTo(mHeadRefreshView.getRefreshHeight());
                    if (pullListener != null) {
                        pullListener.onRefresh();
                    }
                    break;
                case HEAD_STATE_DONE://切换到刷新完成或者加载成功的状态
                    if (mHeadRefreshState == HEAD_STATE_REFRESHING) {
                        mHeadRefreshView.onResultSuccess();
                        mHeadRefreshView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mHeadRefreshView.smoothScrollTo(0);
                            }
                        }, 500);
                    }
                    break;
                case HEAD_STATE_FAIL://切换到刷新失败或者加载失败的状态
                    if (mHeadRefreshState == HEAD_STATE_REFRESHING) {
                        mHeadRefreshView.onResultFail();
                        mHeadRefreshView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mHeadRefreshView.smoothScrollTo(0);
                            }
                        }, 500);
                    }
                    break;
            }
            mHeadRefreshState = state;
        }

    }

    /**
     * 判断列表是否滑到顶部
     */
    private boolean isOnTop() {
        return isExistRefreshView() && mHeadRefreshView.getParent() != null;
    }

    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state == RecyclerView.SCROLL_STATE_IDLE
                && isCanLoadMore()
                && !isLoadMore()
                && !isRefresh()) {
            LayoutManager layoutManager = getLayoutManager();
            int lastCompletelyVisibleItemPosition;
            if (layoutManager instanceof GridLayoutManager) {
                lastCompletelyVisibleItemPosition = ((GridLayoutManager) layoutManager)
                        .findLastCompletelyVisibleItemPosition();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                ((StaggeredGridLayoutManager) layoutManager).findLastCompletelyVisibleItemPositions(into);
                lastCompletelyVisibleItemPosition = findMax(into);
            } else {
                lastCompletelyVisibleItemPosition = ((LinearLayoutManager) layoutManager)
                        .findLastCompletelyVisibleItemPosition();
            }
            if (layoutManager.getChildCount() > 0 &&
                    lastCompletelyVisibleItemPosition == pullToRefreshRecyclerViewAdapter.getItemCount() - 1) {
                mMoreRefreshView.onLoadingMore();
                if (pullListener != null) {
                    pullListener.onLoadMore();
                }
                mMoreRefreshState = MORE_STATE_REFRESHING;
            }
        }
    }

    /**
     * 上下拉完成
     *
     * @param success 下拉或上滑是否成功
     */
    public void onComplete(boolean success) {
        if (isRefresh()) {
            if (success) {
                onPullComplete();
            } else {
                onPullFail();
            }

        }
        if (isLoadMore()) {
            if (success) {
                onLoadMoreComplete();
            } else {
                onLoadMoreFail();
            }
        }
    }


    //下拉刷新成功
    public void onPullComplete() {
        setRefreshState(HEAD_STATE_DONE);
    }

    //下拉刷新失败
    public void onPullFail() {
        setRefreshState(HEAD_STATE_FAIL);
    }

    //上拉加载成功
    public void onLoadMoreComplete() {
        if (isLoadMore()) {
            mMoreRefreshView.onResultSuccess();
            mMoreRefreshView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMoreRefreshState = MORE_STATE_NORMAL;
                    mMoreRefreshView.onNormalState();
                }
            }, 500);
        }
    }

    //上拉加载失败
    public void onLoadMoreFail() {
        if (isLoadMore()) {
            mMoreRefreshView.onResultFail();
            mMoreRefreshView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMoreRefreshState = MORE_STATE_NORMAL;
                    mMoreRefreshView.onNormalState();
                }
            }, 500);
        }
    }

    //主动触发头部刷新
    public void onRefresh() {
        if (isCanRefresh() && !isLoadMore() && !isRefresh()) {
            mHeadRefreshState = HEAD_STATE_REFRESHING;
            mHeadRefreshView.onRefreshing();
            mHeadRefreshView.smoothScrollTo(mHeadRefreshView.getRefreshHeight());
            if (pullListener != null) {
                pullListener.onRefresh();
            }
        }
    }

    //主动触发底部刷新
    public void onLoadMore() {
        if (isCanLoadMore() && !isLoadMore() && !isRefresh()) {
            mMoreRefreshState = MORE_STATE_REFRESHING;
            mMoreRefreshView.onLoadingMore();
            if (pullListener != null) {
                pullListener.onLoadMore();
            }
        }
    }

    private class PullToRefreshRecyclerViewAdapter extends Adapter<ViewHolder> {
        private static final int TYPE_REFRESH_HEADER = 10000;//头部下拉刷新类型
        private static final int TYPE_LOAD_MORE_FOOTER = 10001;//底部加载更多类型
        private Adapter adapter;

        private PullToRefreshRecyclerViewAdapter(Adapter adapter) {
            this.adapter = adapter;
        }

        public Adapter getAdapter() {
            return adapter;
        }

        private boolean isLoadMoreFooter(int position) {
            return isExistLoadMoreView() && position == getItemCount() - 1;
        }

        private boolean isRefreshHeader(int position) {
            return isExistRefreshView() && position == 0;
        }

        /**
         * 判断是否是PullToRefreshRecyclerView保留的itemViewType
         */
        private boolean isReservedItemViewType(int itemViewType) {
            return itemViewType == TYPE_REFRESH_HEADER || itemViewType == TYPE_LOAD_MORE_FOOTER;
        }

        @Override
        public int getItemCount() {
            int count = 0;

            if (isExistRefreshView()) {
                count++;
            }

            if (isExistLoadMoreView()) {
                count++;
            }

            if (adapter != null) {
                count += adapter.getItemCount();
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            if (isRefreshHeader(position)) {
                return TYPE_REFRESH_HEADER;
            }

            if (isLoadMoreFooter(position)) {
                return TYPE_LOAD_MORE_FOOTER;
            }
            int adjPosition = position - 1;
            int adapterCount;
            if (adapter != null) {
                adapterCount = adapter.getItemCount();
                if (adjPosition < adapterCount) {
                    int type = adapter.getItemViewType(adjPosition);
                    if (isReservedItemViewType(type)) {
                        throw new IllegalStateException("PullToRefreshRecyclerView require itemViewType in adapter should be less than 10000 ");
                    }
                    return type;
                }
            }
            return 0;
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_REFRESH_HEADER) {
                return new SimpleViewHolder(mHeadRefreshView);
            } else if (viewType == TYPE_LOAD_MORE_FOOTER) {
                return new SimpleViewHolder(mMoreRefreshView);
            }
            return adapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (isRefreshHeader(position) || isLoadMoreFooter(position)) {
                return;
            }
            int adjPosition = position - (isExistRefreshView() ? 1 : 0);
            if (adapter != null) {
                adapter.onBindViewHolder(holder, adjPosition);
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
            if (isRefreshHeader(position) || isLoadMoreFooter(position)) {
                return;
            }
            int adjPosition = position - (isExistRefreshView() ? 1 : 0);
            if (adapter != null) {
                if (payloads.isEmpty()) {
                    adapter.onBindViewHolder(holder, adjPosition);
                } else {
                    adapter.onBindViewHolder(holder, adjPosition, payloads);
                }
            }
        }

        @Override
        public long getItemId(int position) {
            if (isRefreshHeader(position) || isLoadMoreFooter(position)) {
                return -1;
            }
            int adjPosition = position - (isExistRefreshView() ? 1 : 0);
            if (adapter != null) {
                return adapter.getItemId(adjPosition);
            }
            return -1;
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            LayoutManager manager = recyclerView.getLayoutManager();
            if (manager instanceof GridLayoutManager) {
                final GridLayoutManager gridManager = ((GridLayoutManager) manager);
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return (isLoadMoreFooter(position) || isRefreshHeader(position))
                                ? gridManager.getSpanCount() : 1;
                    }
                });
            }
            adapter.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            adapter.onDetachedFromRecyclerView(recyclerView);
        }

        @Override
        public void onViewAttachedToWindow(ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams
                    && (isRefreshHeader(holder.getLayoutPosition()) || isLoadMoreFooter(holder.getLayoutPosition()))) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
            adapter.onViewAttachedToWindow(holder);
        }

        @Override
        public void onViewDetachedFromWindow(ViewHolder holder) {
            adapter.onViewDetachedFromWindow(holder);
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            adapter.onViewRecycled(holder);
        }

        @Override
        public boolean onFailedToRecycleView(ViewHolder holder) {
            return adapter.onFailedToRecycleView(holder);
        }

        @Override
        public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
            adapter.unregisterAdapterDataObserver(observer);
        }

        @Override
        public void registerAdapterDataObserver(AdapterDataObserver observer) {
            adapter.registerAdapterDataObserver(observer);
        }

        private class SimpleViewHolder extends ViewHolder {
            private SimpleViewHolder(View itemView) {
                super(itemView);
            }
        }

    }

    private class DataObserver extends AdapterDataObserver {

        @Override
        public void onChanged() {
            if (pullToRefreshRecyclerViewAdapter != null) {
                pullToRefreshRecyclerViewAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            Log.e("bug", "--------onItemRangeInserted");
            pullToRefreshRecyclerViewAdapter.notifyItemRangeInserted(isExistRefreshView() ?
                    positionStart + 1 : positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            pullToRefreshRecyclerViewAdapter.notifyItemRangeChanged(isExistRefreshView() ?
                    positionStart + 1 : positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            pullToRefreshRecyclerViewAdapter.notifyItemRangeChanged(isExistRefreshView() ?
                    positionStart + 1 : positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            pullToRefreshRecyclerViewAdapter.notifyItemRangeRemoved(isExistRefreshView() ?
                    positionStart + 1 : positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            pullToRefreshRecyclerViewAdapter.notifyItemMoved(isExistRefreshView() ?
                    fromPosition + 1 : fromPosition, isExistRefreshView() ? toPosition + 1 : toPosition);
        }
    }

    //系统级配置
    public static class PullSysConfig {
        private Class<? extends AbRefreshHeadView> refreshViewClass;
        private Class<? extends AbRefreshMoreView> moreViewClass;

        private Class<? extends AbRefreshHeadView> getRefreshViewClass() {
            return refreshViewClass;
        }

        private Class<? extends AbRefreshMoreView> getMoreViewClass() {
            return moreViewClass;
        }

        private PullSysConfig(Builder builder) {
            refreshViewClass = builder.refreshViewClass;
            moreViewClass = builder.moreViewClass;
        }

        public static final class Builder {
            private Class<? extends AbRefreshHeadView> refreshViewClass;
            private Class<? extends AbRefreshMoreView> moreViewClass;

            public Builder refreshViewClass(Class<? extends AbRefreshHeadView> val) {
                refreshViewClass = val;
                return this;
            }

            public Builder moreViewClass(Class<? extends AbRefreshMoreView> val) {
                moreViewClass = val;
                return this;
            }

            public PullSysConfig build() {
                return new PullSysConfig(this);
            }
        }
    }

}
