package com.skipo.teledom.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;
import com.skipo.teledom.R;


/**
 * Created by Roberto on 08/04/2016.
 * 06.01.2017 Fix onSizeChanged, don't show thumb correct in Android >= 6.0
 */
public class SlideButton extends SeekBar {
    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_VERTICAL = 1;
    public static final int DIRECTION_LEFT = 0;
    public static final int DIRECTION_RIGHT = 1;
    public static final int DIRECTION_CENTR = 2;

    private Drawable thumb;
    private SlideButtonListener listener;
    private int orientation;
    private int startPosition;

    public SlideButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public SlideButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs,defStyle);
        //TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.slideButton, defStyle, 0);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.slideButton);
        try {
            orientation = ta.getInteger(R.styleable.slideButton_orientation, ORIENTATION_HORIZONTAL);
            startPosition = ta.getInteger(R.styleable.slideButton_start_position, DIRECTION_LEFT);
        } finally {
            ta.recycle();
        }

        presetPosition();

    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (orientation == ORIENTATION_VERTICAL) {
            super.onMeasure(heightMeasureSpec, widthMeasureSpec);
            setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
        }else {
            super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (orientation == ORIENTATION_VERTICAL) {
            c.rotate(90);
            c.translate(0, -getWidth());
        }
        super.onDraw(c);
    }
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (orientation == ORIENTATION_VERTICAL) {
            super.onSizeChanged(h, w, oldh, oldw);
        }else {
            super.onSizeChanged( w,h,oldw, oldh);
        }
    }


    @Override
    public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
        this.thumb = thumb;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        int progress = getProgress();
        if (orientation == ORIENTATION_HORIZONTAL) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (thumb.getBounds().contains((int) event.getX(), (int) event.getY())) {
                    super.onTouchEvent(event);
//                    handleSlide(startPosition);
                } else
                    return false;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if ( ((startPosition == DIRECTION_LEFT || startPosition == DIRECTION_CENTR) && progress > 60) ||
                        ((startPosition == DIRECTION_RIGHT || startPosition == DIRECTION_CENTR) && progress < 40) ) {
                    handleSlide(progress);
                }

                presetPosition();
            } else {
                super.onTouchEvent(event);
//                handleSlide(startPosition);
            }
        }else{
            int i=0;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        int x= (int) event.getX();
                        int y= (int) event.getY();
                        if (!thumb.getBounds().contains((int) event.getY(), (int) event.getX())) {
                            return false;
//                            super.onTouchEvent(event);
//                            break;
                        }
                    }
                case MotionEvent.ACTION_MOVE:
                    i=getMax() - (int) (getMax() * event.getY() / getHeight());
                    setProgress(100 - i);
                    onSizeChanged(getWidth(), getHeight(), 0, 0);
                    break;
                case MotionEvent.ACTION_UP:
                    i=getMax() - (int) (getMax() * event.getY() / getHeight());
                    if ( ((startPosition == DIRECTION_LEFT || startPosition == DIRECTION_CENTR) && i > 60) ||
                            ((startPosition == DIRECTION_RIGHT || startPosition == DIRECTION_CENTR) && i < 40) ) {
                        handleSlide(i);
                    }
                    presetPosition();
                    onSizeChanged(getWidth(), getHeight(), 0, 0);
                    break;

                case MotionEvent.ACTION_CANCEL:
                    break;
            }
        }
        return true;
    }

    public void presetPosition() {
        if ( startPosition == DIRECTION_LEFT )
            setProgress(0);
        else if ( startPosition == DIRECTION_RIGHT )
            setProgress(100);
        else
            setProgress(50);
    }

    private void handleSlide(int progress) {
        listener.handleSlide(progress);
    }

    public void setSlideButtonListener(SlideButtonListener listener) {
        this.listener = listener;
    }
}

