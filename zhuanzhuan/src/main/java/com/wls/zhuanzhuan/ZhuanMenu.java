package com.wls.zhuanzhuan;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * 架构：WLS on 2016/11/18 11:50
 * 邮箱：vchess@126.com
 */

public class ZhuanMenu extends FrameLayout {

    static final String TAG = "ZhuanMenu";

    static final String TAG_ITEM_CONTAINER = "tag_item_container";

    static final String TAG_ITEM_PAGER = "tag_item_pager";

    static final String TAG_ITEM_HINT = "tag_item_hint";

    static final int MENU_STATE_CLOSE = -2;

    static final int MENU_STATE_CLOSED = -1;

    static final int MENU_STATE_OPEN = 1;

    static final int MENU_STATE_OPENED = 2;

    /**
     * 左右菜单 Item 移动动画的距离
     */
    static final float TRAN_SKNEW_VALUE = 160;

    /**
     * Hint 相对 页面的上外边距
     */
    static final int HINT_TOP_MARGIN = 15;

    /**
     * 可旋转、转动布局
     */
    private ZhuanMenuLayout zhuanMenuLayout;

    /**
     * 菜单打开关闭动画帮助类
     */
    private ZhuanMenuAnimator zhuanMenuAnimator;

    /**
     * 页面适配器
     */
    private PagerAdapter pagerAdapter;

    /**
     * 手势识别器
     */
    private GestureDetectorCompat menuDetector;

    /**
     * 菜单状态改变监听器
     */
    private OnZhuanMenuStateChangeListener onZhuanMenuStateChangeListener;

    /**
     * 缓存 Fragment 的集合，供 {@link #pagerAdapter} 回收使用
     */
    private List pagerObjects;

    /**
     * 菜单项集合
     */
    private List<SMItemLayout> smItemLayoutList;

    /**
     * 页面标题字符集合
     */
    private List<String> hintStrList;

    /**
     * 页面标题字符尺寸
     */
    private int hintTextSize = 14;

    /**
     * 页面标题字符颜色
     */
    private int hintTextColor = Color.parseColor("#666666");

    /**
     * 默认打开菜单时页面缩小的比率
     */
    private float scaleRatio = .36f;

    /**
     * 控件是否初始化的标记变量
     */
    private boolean init = true;

    /**
     * 是否启用手势识别
     */
    private boolean enableGesture;

    /**
     * 当前菜单状态，默认为已关闭
     */
    private int menuState = MENU_STATE_CLOSED;

    /**
     * 滑动与触摸之间的阀值
     */
    private int touchSlop = 8;

    private OnZhuanSelectedListener onZhuanSelectedListener = new OnZhuanSelectedListener() {
        @Override
        public void onZhuanSelected(int position) {
            log("ZhuanMenu position:" + position);
        }
    };

    private OnMenuSelectedListener onMenuSelectedListener = new OnMenuSelectedListener() {
        @Override
        public void onMenuSelected(SMItemLayout smItemLayout) {
            closeMenu(smItemLayout);
        }
    };

    private GestureDetector.SimpleOnGestureListener menuGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceX) < touchSlop && distanceY < -touchSlop * 3) {
                openMenu();
            }
            return true;
        }
    };

    public ZhuanMenu(Context context) {
        this(context, null);
    }

    public ZhuanMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZhuanMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpinMenu);
        scaleRatio = typedArray.getFloat(R.styleable.SpinMenu_scale_ratio, scaleRatio);
        hintTextSize = typedArray.getDimensionPixelSize(R.styleable.SpinMenu_hint_text_size, hintTextSize);
        hintTextSize = px2Sp(hintTextColor);
        hintTextColor = typedArray.getColor(R.styleable.SpinMenu_hint_text_color, hintTextColor);
        typedArray.recycle();

        pagerObjects = new ArrayList();
        smItemLayoutList = new ArrayList<>();
        menuDetector = new GestureDetectorCompat(context, menuGestureListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            ViewConfiguration conf = ViewConfiguration.get(getContext());
            touchSlop = conf.getScaledTouchSlop();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        @IdRes final int smLayoutId = 0x6F060505;
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        zhuanMenuLayout = new ZhuanMenuLayout(getContext());
        zhuanMenuLayout.setId(smLayoutId);
        zhuanMenuLayout.setLayoutParams(layoutParams);
        zhuanMenuLayout.setOnZhuanSelectedListener(onZhuanSelectedListener);
        zhuanMenuLayout.setOnMenuSelectedListener(onMenuSelectedListener);
        addView(zhuanMenuLayout);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (init && smItemLayoutList.size() > 0) {
            // 根据 scaleRatio 去调整菜单中 item 视图的整体大小
            int pagerWidth = (int) (getMeasuredWidth() * scaleRatio);
            int pagerHeight = (int) (getMeasuredHeight() * scaleRatio);
            SMItemLayout.LayoutParams containerLayoutParams = new SMItemLayout.LayoutParams(pagerWidth, pagerHeight);
            SMItemLayout smItemLayout;
            FrameLayout frameContainer;
            TextView tvHint;
            for (int i = 0; i < smItemLayoutList.size(); i++) {
                smItemLayout = smItemLayoutList.get(i);
                frameContainer = (FrameLayout) smItemLayout.findViewWithTag(TAG_ITEM_CONTAINER);
                frameContainer.setLayoutParams(containerLayoutParams);
                if (i == 0) { // 初始菜单的时候，默认显示第一个 Fragment
                    FrameLayout pagerLayout = (FrameLayout) smItemLayout.findViewWithTag(TAG_ITEM_PAGER);
                    // 先移除第一个包含 Fragment 的布局
                    frameContainer.removeView(pagerLayout);

                    // 创建一个用来占位的 FrameLayout
                    FrameLayout holderLayout = new FrameLayout(getContext());
                    LinearLayout.LayoutParams pagerLinLayParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
                    holderLayout.setLayoutParams(pagerLinLayParams);

                    // 将占位的 FrameLayout 添加到布局中的 frameContainer 中
                    frameContainer.addView(holderLayout, 0);

                    // 添加 第一个包含 Fragment 的布局添加到 ZhuanMenu 中
                    FrameLayout.LayoutParams pagerFrameParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
                    pagerLayout.setLayoutParams(pagerFrameParams);
                    addView(pagerLayout);
                }

                // 显示标题
                if (hintStrList != null && !hintStrList.isEmpty() && i < hintStrList.size()) {
                    tvHint = (TextView) smItemLayout.findViewWithTag(TAG_ITEM_HINT);
                    tvHint.setText(hintStrList.get(i));
                    tvHint.setTextSize(hintTextSize);
                    tvHint.setTextColor(hintTextColor);
                }

                // 位于菜单中当前显示 Fragment 两边的 SMItemlayout 左右移动 TRAN_SKNEW_VALUE 个距离
                if (zhuanMenuLayout.getSelectedPosition() + 1 == i
                        || (zhuanMenuLayout.isCyclic()
                        && zhuanMenuLayout.getMenuItemCount() - i == zhuanMenuLayout.getSelectedPosition() + 1)) { // 右侧 ItemMenu
                    smItemLayout.setTranslationX(TRAN_SKNEW_VALUE);
                } else if (zhuanMenuLayout.getSelectedPosition() - 1 == i
                        || (zhuanMenuLayout.isCyclic()
                        && zhuanMenuLayout.getMenuItemCount() - i == 1)) { // 左侧 ItemMenu
                    smItemLayout.setTranslationX(-TRAN_SKNEW_VALUE);
                } else {
                    smItemLayout.setTranslationX(0);
                }
            }
            zhuanMenuAnimator = new ZhuanMenuAnimator(this, zhuanMenuLayout, onZhuanMenuStateChangeListener);
            init = false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (enableGesture) menuDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (enableGesture) {
            menuDetector.onTouchEvent(event);
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位转成为 sp
     *
     * @param pxValue
     * @return
     */
    private int px2Sp(float pxValue) {
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    private void log(String log) {
        Log.d(TAG, log);
    }

    public void setFragmentAdapter(PagerAdapter adapter) {
        if (pagerAdapter != null) {
            pagerAdapter.startUpdate(zhuanMenuLayout);
            for (int i = 0; i < adapter.getCount(); i++) {
                ViewGroup pager = (ViewGroup) zhuanMenuLayout.getChildAt(i).findViewWithTag(TAG_ITEM_PAGER);
                pagerAdapter.destroyItem(pager, i, pagerObjects.get(i));
            }
            pagerAdapter.finishUpdate(zhuanMenuLayout);
        }

        int pagerCount = adapter.getCount();
        if (pagerCount > zhuanMenuLayout.getMaxMenuItemCount())
            throw new RuntimeException(String.format("Fragment number can't be more than %d", zhuanMenuLayout.getMaxMenuItemCount()));

        pagerAdapter = adapter;

        SMItemLayout.LayoutParams itemLinLayParams = new SMItemLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        LinearLayout.LayoutParams containerLinlayParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        FrameLayout.LayoutParams pagerFrameParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        LinearLayout.LayoutParams hintLinLayParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        hintLinLayParams.topMargin = HINT_TOP_MARGIN;
        pagerAdapter.startUpdate(zhuanMenuLayout);
        for (int i = 0; i < pagerCount; i++) {
            // 创建菜单父容器布局
            SMItemLayout smItemLayout = new SMItemLayout(getContext());
            smItemLayout.setId(i + 1);
            smItemLayout.setGravity(Gravity.CENTER);
            smItemLayout.setLayoutParams(itemLinLayParams);

            // 创建包裹FrameLayout
            FrameLayout frameContainer = new FrameLayout(getContext());
            frameContainer.setId(pagerCount + i + 1);
            frameContainer.setTag(TAG_ITEM_CONTAINER);
            frameContainer.setLayoutParams(containerLinlayParams);

            // 创建 Fragment 容器
            FrameLayout framePager = new FrameLayout(getContext());
            framePager.setId(pagerCount * 2 + i + 1);
            framePager.setTag(TAG_ITEM_PAGER);
            framePager.setLayoutParams(pagerFrameParams);
            Object object = pagerAdapter.instantiateItem(framePager, i);

            // 创建菜单标题 TextView
            TextView tvHint = new TextView(getContext());
            tvHint.setId(pagerCount * 3 + i + 1);
            tvHint.setTag(TAG_ITEM_HINT);
            tvHint.setLayoutParams(hintLinLayParams);

            frameContainer.addView(framePager);
            smItemLayout.addView(frameContainer);
            smItemLayout.addView(tvHint);
            zhuanMenuLayout.addView(smItemLayout);

            pagerObjects.add(object);
            smItemLayoutList.add(smItemLayout);
        }
        pagerAdapter.finishUpdate(zhuanMenuLayout);
    }

    public void openMenu() {
        if (menuState == MENU_STATE_CLOSED) {
            zhuanMenuAnimator.openMenuAnimator();
        }
    }

    public void closeMenu(SMItemLayout chooseItemLayout) {
        if (menuState == MENU_STATE_OPENED) {
            zhuanMenuAnimator.closeMenuAnimator(chooseItemLayout);
        }
    }

    public int getMenuState() {
        return menuState;
    }

    public void updateMenuState(int state) {
        menuState = state;
    }

    public void setEnableGesture(boolean enable) {
        enableGesture = enable;
    }

    public void setMenuItemScaleValue(float scaleValue) {
        scaleRatio = scaleValue;
    }

    public void setHintTextSize(int textSize) {
        hintTextSize = textSize;
    }

    public void setHintTextColor(int textColor) {
        hintTextColor = textColor;
    }

    public void setHintTextStrList(List<String> hintTextList) {
        hintStrList = hintTextList;
    }

    public void setOnZhuanMenuStateChangeListener(OnZhuanMenuStateChangeListener listener) {
        onZhuanMenuStateChangeListener = listener;
    }

    public float getScaleRatio() {
        return scaleRatio;
    }
}
