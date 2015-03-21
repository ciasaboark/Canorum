/*
 * Copyright (c) 2015, Jonathan Nelson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ciasaboark.canorum.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

/**
 * Created by Jonathan Nelson on 3/14/15.
 */
public class HidingToolbar extends Toolbar {
    private static final String TAG = "HidingToolbar";
    private Context mContext;
    private Drawable mZeroPositionBackground;
    private Drawable mFadeInBackground;
    private ViewTreeObserver mViewTreeObserver;
    private ScrollView mScrollView;
    private int mLastScrollPos;

    private boolean mShowingFadeInBackground = false;
    private int mAlpha = 255;


    private ViewTreeObserver.OnScrollChangedListener mListener = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            if (mScrollView == null) {
                Log.e(TAG, "could not find a ScrollView that scrolled, but OnScrollChangedListener was triggered");
                removeObserver();
                return;
            }

            int viewHeight = getHeight();
            int scrollY = mScrollView.getScrollY();
            boolean scrollDirectionDown = scrollY > mLastScrollPos;
            int viewCurTranslation = (int) getTranslationY();
            Log.d(TAG, "scrolling " + (scrollDirectionDown ? " down " : " up ") + " from: " + mLastScrollPos + " to: " + scrollY);

            if (scrollY == 0) {
                setBackground(mZeroPositionBackground);
                mShowingFadeInBackground = false;
                mAlpha = 0;
                setTranslationY(0);
            } else {
                if (mFadeInBackground != null) {
                    float alpha = (float) scrollY / (float) viewHeight;
                    alpha = (float) Math.min(alpha, 1.0);    //cap the alpha at 1.0
                    alpha = (float) Math.max(alpha, 0.0);    //min alpha at 0.0
                    mAlpha = (int) (255 * alpha);
                    mFadeInBackground.setAlpha(mAlpha);
                }

                int moveDiff = mLastScrollPos - scrollY;
                int newTranslationY = viewCurTranslation + moveDiff;
                if (newTranslationY > 0) newTranslationY = 0;

                if (scrollDirectionDown) {
                    //scrolling in down direction, adjust alpha, and move toolbar up
                    if (mFadeInBackground != null) setBackground(mFadeInBackground);
                    setTranslationY(newTranslationY);
                } else {
                    //scrolling in up direction.  if toolbar is not visibile then immediately
                    //move it to the top of the screen
                    if (viewCurTranslation < -viewHeight) {
                        setTranslationY(-viewHeight);
                        if (mFadeInBackground != null) setBackground(mFadeInBackground);
                    } else {
                        setTranslationY(newTranslationY);
                        if (mFadeInBackground != null) setBackground(mFadeInBackground);
                        ;
                    }
                }
            }

            mLastScrollPos = scrollY;
        }
    };

    public HidingToolbar(Context context) {
        this(context, null);
    }

    public HidingToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (context == null) throw new IllegalArgumentException("context can not be null");
        mContext = context;
        mZeroPositionBackground = getBackground();
        mViewTreeObserver = getViewTreeObserver();
    }

    public HidingToolbar attachScrollView(ScrollView scrollView) {
        if (scrollView == null) throw new IllegalArgumentException("scrollview can not be null");
        //detatch the already attached scroll view if there is one
        if (mScrollView != null) {
            detatchScrollView();
        }
        mScrollView = scrollView;
        mLastScrollPos = scrollView.getScrollY();
        mViewTreeObserver.addOnScrollChangedListener(mListener);
        return this;
    }

    public void detatchScrollView() {
        if (mScrollView != null) {
            removeObserver();
            mScrollView = null;
            mLastScrollPos = 0;
        }
    }

    private void removeObserver() {
        try {
            mViewTreeObserver.removeOnScrollChangedListener(mListener);
        } catch (IllegalStateException e) {
            //this is fine
        }
    }

    public HidingToolbar setFadeInBackground(Drawable background) {
        mFadeInBackground = background;
        if (mShowingFadeInBackground) {
            mFadeInBackground.setAlpha(mAlpha);
            setBackground(mFadeInBackground);
        }
        return this;
    }
}
