package com.wls.zhuanzhuan;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * 架构：WLS on 2016/11/15 10:25
 * 邮箱：vchess@126.com
 */

public class SMItemLayout extends LinearLayout {

    public SMItemLayout(Context context) {
        this(context, null);
    }

    public SMItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SMItemLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(VERTICAL);
    }
}
