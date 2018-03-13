package com.famabb.pull;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by ${ChenJC} on 2018/2/24.
 */

public abstract class AbRefreshView extends LinearLayout {
    protected Context mContext;
    protected View mMainView;

    public AbRefreshView(Context context) {
        this(context, null);
    }

    public AbRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context context) {
        mContext = context;
        mMainView = onCreateView(context);
        initView();
        addView(mMainView);
    }

    //创建布局
    protected abstract View onCreateView(Context context);

    protected abstract void initView();

    protected final View findViewFromId(int viewId) {
        return mMainView.findViewById(viewId);
    }

    protected int getVisibleHeight() {
        LayoutParams lp = (LayoutParams) mMainView.getLayoutParams();
        return lp.height;
    }

    protected void smoothScrollTo(int destHeight) {
        ValueAnimator animator = ValueAnimator.ofInt(getVisibleHeight(), destHeight);
        animator.setDuration(300).start();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setVisibleHeight((int) animation.getAnimatedValue());
            }
        });
        animator.start();
    }

    protected void setVisibleHeight(int height) {
        if (height < 0) {
            height = 0;
        }
        LayoutParams lp = (LayoutParams) mMainView.getLayoutParams();
        lp.height = height;
        mMainView.setLayoutParams(lp);
    }
}
