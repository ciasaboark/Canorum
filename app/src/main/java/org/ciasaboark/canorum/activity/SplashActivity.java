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

package org.ciasaboark.canorum.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;

public class SplashActivity extends ActionBarActivity {
    private MusicControllerSingleton musicControllerSingleton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_splash);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            AsyncTask<Void, Void, Void> loader = new AsyncTask<Void, Void, Void>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();

                    final Animation rotateForeverAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF,
                            0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    rotateForeverAnimation.setDuration(5000);
                    rotateForeverAnimation.setInterpolator(new LinearInterpolator());
                    rotateForeverAnimation.setRepeatCount(Animation.INFINITE);

                    final View image = findViewById(R.id.main_rotate);

                    ObjectAnimator scaleX = ObjectAnimator.ofFloat(image, "scaleX", 0, 1);
                    ObjectAnimator scaleY = ObjectAnimator.ofFloat(image, "scaleY", 0, 1);
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
                            image.startAnimation(rotateForeverAnimation);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                            //nothing to do here
                        }
                    });

                    image.startAnimation(rotate);
                    image.setVisibility(View.VISIBLE);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    musicControllerSingleton = MusicControllerSingleton.getInstance(SplashActivity.this);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    startMainActivity();
                }
            };
            loader.execute();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicControllerSingleton != null  && musicControllerSingleton.isServiceBound()) {
            musicControllerSingleton.unbindService();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash, menu);
        return true;
    }

    private void startMainActivity() {
        Intent i = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }
}
