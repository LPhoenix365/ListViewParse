package com.pawanjia.listviewbar;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import java.util.List;

/**
 * Description  带指示侧滑组合控件
 *
 * @author liupeng502
 * @data 2017/9/8
 */

public class IndicatorListGroup extends FrameLayout {
    
    private boolean mIsClose;
    private RelativeLayout rl;
    private ViewGroup mLayout;
    private ListView lv;
    private IndicatorList indicator;
    private FrameLayout frame;
    private ImageView iv;
    private float downX;
    private float downY;
    private float totalDx;
    private ObjectAnimator animatorIn;
    private ObjectAnimator animatorOut;
    private int textAreaWidth;
    private Scroller mScroller;
    private ObjectAnimator animatorLeft;
    private ObjectAnimator animatorRight;

    public IndicatorListGroup(Context context) {
        this(context, null);
    }

    public IndicatorListGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicatorListGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        mLayout = (ViewGroup) layoutInflater.inflate(R.layout.indicator_list_group, this, true);
        rl = (RelativeLayout) mLayout.findViewById(R.id.indicator_rl);
        lv = (ListView) mLayout.findViewById(R.id.indicator_lv);
        indicator = (IndicatorList) mLayout.findViewById(R.id.indicator_list);
        frame = (FrameLayout) mLayout.findViewById(R.id.indicator_frame);
        iv = (ImageView) mLayout.findViewById(R.id.indicator_iv);
        mScroller = new Scroller(getContext());
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // 设置第一个可见行会导致指示器 最后几个下不去已解决
                if (firstVisibleItem + visibleItemCount < totalItemCount) {
                    indicator.setSelectedPosition(firstVisibleItem);
                }
            }
        });
        indicator.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                indicator.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                textAreaWidth = indicator.getTextAreaWidth();
                loadAnim(textAreaWidth);
            }
        });
        indicator.setOnTouchListner(new IndicatorList.OnTouchListner() {
            @Override
            public void onTouch(int position) {
                lv.setSelection(position);
            }
        });
        mIsClose = false;
        frame.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsClose) {
                    animatorIn.start();
                    animatorRight.start();
                    mIsClose = false;
                } else {
                    animatorOut.start();
                    animatorLeft.start();
                    mIsClose = true;
                }
            }
        });

    }

    /**
     * 设置关闭导航栏
     */
    public void closeIndector(){
        animatorOut.start();
        animatorLeft.start();
        mIsClose = true;
    }


    public void loadAnim(int textAreaWidth) {
        //from、to位置是指targat的本身
        animatorOut = ObjectAnimator.ofFloat(rl, "translationX", 0, textAreaWidth);
        animatorOut.setDuration(500);

        //to位置是指target 起始和终点位置就是上面位置相反的
        animatorIn = ObjectAnimator.ofFloat(rl, "translationX", textAreaWidth, 0);
        animatorIn.setDuration(500);

        animatorLeft = ObjectAnimator.ofFloat(iv, "rotation", 0, 180);
        animatorLeft.setDuration(500);

        animatorRight = ObjectAnimator.ofFloat(iv, "rotation", 180, 0);
        animatorRight.setDuration(500);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = ev.getX();
                float moveY = ev.getY();
                if (Math.abs(moveY - downY) < Math.abs(moveX - downX)) {
                    return true;
                }
                break;
            default:
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX();
                float moveY = event.getY();
                float dx = moveX - downX;
                totalDx += dx;
                //左边界
                if (totalDx <= 0) {
                    totalDx = 0;
                }
                //右边界
                if (totalDx >= textAreaWidth) {
                    totalDx = textAreaWidth;
                }
                float percent = totalDx * 1.f / textAreaWidth;
                rl.setTranslationX(evaluate(percent, 0, textAreaWidth));
                downX = moveX;
                //旋转箭头
                iv.setRotation(evaluate(percent, 0, 180));
                break;
            case MotionEvent.ACTION_UP:
                if (totalDx <= textAreaWidth / 2) {
                    //展开 dx大于0往右
                    mScroller.startScroll(Math.round(totalDx), 0, Math.round(-totalDx), 0, 300);
                } else {
                    //收缩
                    mScroller.startScroll(Math.round(totalDx), 0, Math.round(textAreaWidth - totalDx), 0, 300);
                }
                invalidate();
                break;
            default:
        }
        return super.onTouchEvent(event);
    }


    /**
     * 计算移动view
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            int currX = mScroller.getCurrX();
            rl.setTranslationX(currX);
            float percent = currX * 1.f / textAreaWidth;
            iv.setRotation(evaluate(percent, 0, 180));
            postInvalidate();
        }

    }

    //避免每次创建
    private Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }


    public void setIndicatorText(List<String> textArray) {
        if (textArray == null || textArray.size()<=0) {
            indicator.setVisibility(GONE);
            iv.setVisibility(GONE);
        }else{
            indicator.setVisibility(VISIBLE);
            iv.setVisibility(VISIBLE);
        }
        indicator.setIndicatorText(textArray);
    }


    public void setAdapter(BaseAdapter adapter) {
        BaseAdapter lvAdapter = (BaseAdapter) lv.getAdapter();
        if (lvAdapter != null) {
            lvAdapter.notifyDataSetChanged();
        }else{
            if (adapter != null) {
                lv.setAdapter(adapter);
            }
        }
    }


    public void notifyDataSetChanged() {
        BaseAdapter lvAdapter = (BaseAdapter) lv.getAdapter();
        if (lvAdapter != null) {
            lvAdapter.notifyDataSetChanged();
        }
    }
    
}
