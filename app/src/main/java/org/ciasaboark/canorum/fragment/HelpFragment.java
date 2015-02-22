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

package org.ciasaboark.canorum.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.versioning.Versioning;

public class HelpFragment extends Fragment {
    private static final String TAG = "HelpFragment";
    final RotateAnimation mRotateForeverAnimation;
    private OnFragmentInteractionListener mListener;
    private View mView;
    private boolean mIconClickable = false;
    private Integer mXCoord;
    private Integer mYCoord;
    View.OnTouchListener mOnTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: {
                    final int x = (int) event.getX();
                    final int y = (int) event.getY();
                    mXCoord = x;
                    mYCoord = y;
                    break;
                }
            }
            return false;
        }
    };
    private View mNormalHeader;
    private View mHiddenHeader;
    private int mClickCount = 0;

    public HelpFragment() {
        // Required empty public constructor
        mRotateForeverAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateForeverAnimation.setDuration(5000);
        mRotateForeverAnimation.setInterpolator(new LinearInterpolator());
        mRotateForeverAnimation.setRepeatCount(Animation.INFINITE);
    }

    public static HelpFragment newInstance() {
        HelpFragment fragment = new HelpFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation anim;
        if (enter) {
            anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        } else {
            anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        }

        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                Log.d(TAG, "fragment animation completed");
                showAppIcon();
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });

        return anim;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_help, container, false);

        Toolbar toolbar = (Toolbar) mView.findViewById(R.id.local_toolbar);
        toolbar.setTitle("Help & Feedback");
        toolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_title_text));
        mListener.setToolbar(toolbar);
        mNormalHeader = mView.findViewById(R.id.help_header_about);
        mHiddenHeader = mView.findViewById(R.id.help_header_hidden);

        initTextLinks();
        initAppIcon();
        initHeader();
        return mView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_help, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    private void showAppIcon() {
        final ImageView appIcon = (ImageView) mView.findViewById(R.id.help_header_icon);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(appIcon, "scaleX", 0, 1);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(appIcon, "scaleY", 0, 1);
        AnimatorSet animSetXY = new AnimatorSet();
        animSetXY.playTogether(scaleX, scaleY);
        animSetXY.setInterpolator(new BounceInterpolator());
        animSetXY.setDuration(2000);
        animSetXY.start();

        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(5000);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //nothing to do here
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIconClickable = true;
                appIcon.startAnimation(mRotateForeverAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                //nothing to do here
            }
        });

        appIcon.startAnimation(rotate);
        appIcon.setVisibility(View.VISIBLE);

    }

    private void initHeader() {
        mNormalHeader.setOnTouchListener(mOnTouch);
//        mNormalHeader.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                toggleVisibility(mNormalHeader);
//                return true;
//            }
//        });

        mHiddenHeader.setOnTouchListener(mOnTouch);
        //TODO setup the switch here

        mHiddenHeader.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleVisibility(mNormalHeader);
                return true;
            }
        });

        String versionCode = Versioning.getVersionCode();
        TextView versionText = (TextView) mView.findViewById(R.id.help_header_version);
        versionText.setText("Version " + versionCode);
    }

    private void initAppIcon() {
        final ImageView appIcon = (ImageView) mView.findViewById(R.id.help_header_icon);
        appIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (++mClickCount > 4) {
                    toggleVisibility(mNormalHeader);
                }
            }
        });
    }

    private void initTextLinks() {
        TextView githubText = (TextView) mView.findViewById(R.id.help_body_github_text);
        TextView commentText = (TextView) mView.findViewById(R.id.help_body_feedback_text);
        githubText.setMovementMethod(LinkMovementMethod.getInstance());
        commentText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void toggleVisibility(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            toggleVisibilityCircularReveal(view);
        } else {
            toggleVisibilitySimple(view);
        }
    }

    @TargetApi(21)
    private void toggleVisibilityCircularReveal(final View view) {
        if (view.getVisibility() == View.VISIBLE) {
            //the view is visible, so animate it out then set visibility to GONE
            int cx;
            int cy;
            if (this.mXCoord == null || this.mYCoord == null) {
                // get the center for the clipping circle
                cx = (view.getLeft() + view.getRight()) / 2;
                cy = (view.getTop() + view.getBottom()) / 2;
            } else {
                //a touch event has been recorded, use the last known coordinates
                cx = this.mXCoord;
                cy = this.mYCoord;
            }

            // get the initial radius for the clipping circle
            int initialRadius = view.getWidth();

            // create the animation (the final radius is zero)
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.INVISIBLE);
                }
            });
            anim.setDuration(700);
            // start the animation
            anim.start();
        } else {
            //the view is not visible (either GONE or INVISIBILE), set visibility to VISIBLE, then
            //animate in
            int cx;
            int cy;
            if (this.mXCoord == null || this.mYCoord == null) {
                // get the center for the clipping circle
                cx = (view.getLeft() + view.getRight()) / 2;
                cy = (view.getTop() + view.getBottom()) / 2;
            } else {
                //a touch event has been recorded, use the last known coordinates
                cx = this.mXCoord;
                cy = this.mYCoord;
            }

            // get the final radius for the clipping circle
            int finalRadius = view.getWidth();

            // create and start the animator for this view
            // (the start radius is zero)
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);
            anim.setDuration(500);
            view.setVisibility(View.VISIBLE);
            anim.start();
        }
    }

    private void toggleVisibilitySimple(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.INVISIBLE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }


}
