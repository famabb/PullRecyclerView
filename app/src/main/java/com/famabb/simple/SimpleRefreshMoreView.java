package com.famabb.simple;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.famabb.pull.AbRefreshMoreView;

/**
 * Created by ${ChenJC} on 2018/2/24.
 * 上拉更多
 */

public class SimpleRefreshMoreView extends AbRefreshMoreView {
    private ImageView mIvAnim;
    private TextView mTvTip;
    private ValueAnimator mAnim;

    public SimpleRefreshMoreView(Context context) {
        super(context);
    }

    @Override
    protected void onNormalState() {
        mTvTip.setText(mContext.getString(R.string.up_load_more));
        stopAnimation();
        mTvTip.setVisibility(View.VISIBLE);
        mIvAnim.setVisibility(View.GONE);
    }

    @Override
    protected void onLoadingMore() {
        mTvTip.setText("正在加载");
        mIvAnim.setVisibility(View.VISIBLE);
        startAnimation();
    }

    @Override
    protected void onResultSuccess() {
        stopAnimation();
        mTvTip.setText(mContext.getString(R.string.load_more_s));
        onResult();
    }

    @Override
    protected void onResultFail() {
        stopAnimation();
        mTvTip.setText(mContext.getString(R.string.load_more_f));
        onResult();
    }

    private void onResult() {
        mTvTip.setVisibility(View.VISIBLE);
        mIvAnim.setVisibility(View.GONE);
    }

    private void startAnimation() {
        mAnim = ValueAnimator.ofFloat(mIvAnim.getRotation(), mIvAnim.getRotation() + 359);
        mAnim.setInterpolator(new LinearInterpolator());
        mAnim.setRepeatCount(ValueAnimator.INFINITE);
        mAnim.setDuration(1000);
        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIvAnim.setRotation((Float) animation.getAnimatedValue());
            }
        });
        mAnim.start();
    }

    public void stopAnimation() {
        if (mAnim != null) {
            mAnim.end();
        }
    }

    @Override
    protected View onCreateView(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.view_refresh_more, null);
    }

    @Override
    protected void initView() {
        mIvAnim = (ImageView) findViewFromId(R.id.iv_anim);
        mTvTip = (TextView) findViewFromId(R.id.tv_tip);
    }
}
