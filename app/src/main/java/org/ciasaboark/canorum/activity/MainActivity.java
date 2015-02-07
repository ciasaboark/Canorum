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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.fragment.LibraryFragment;
import org.ciasaboark.canorum.fragment.NowPlayingFragment;
import org.ciasaboark.canorum.fragment.OnFragmentInteractionListener;
import org.ciasaboark.canorum.fragment.SettingsFragment;
import org.ciasaboark.canorum.view.NavDrawer;


public class MainActivity extends ActionBarActivity implements NavDrawer.NavDrawerListener, OnFragmentInteractionListener {
    private static final String TAG = "MainActivity";
    private MusicControllerSingleton musicControllerSingleton;
    private NavDrawer mNavDrawer;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private FrameLayout mFragmentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        setSupportActionBar(mToolbar);

        mToolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        mToolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_title_text));
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.nav_drawer_open, R.string.nav_drawer_closed);
        mDrawerLayout.setDrawerListener(actionBarDrawerToggle);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavDrawer = (NavDrawer) findViewById(R.id.nav_drawer);
        mNavDrawer.setListener(this);

        initBroadcastReceivers();

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        musicControllerSingleton = MusicControllerSingleton.getInstance(this);

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Log.d(TAG, "backstack changed");

            }
        });

        //TODO start with the appropriate fragment
        mFragmentContainer = (FrameLayout) findViewById(R.id.main_fragment);
        if (savedInstanceState != null) {
            return;
        } else if (mFragmentContainer != null) {
            if (musicControllerSingleton.isPlaying()) {
                NowPlayingFragment nowPlayingFragment = new NowPlayingFragment();
                nowPlayingFragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction()
                        .add(R.id.main_fragment, nowPlayingFragment)
                        .addToBackStack(null)
                        .commit();
                mNavDrawer.setSelectedSection(NavDrawer.NAV_DRAWER_ITEM.CUR_PLAYING);
            } else {
                LibraryFragment libraryFragment = new LibraryFragment();
                libraryFragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction()
                        .add(R.id.main_fragment, libraryFragment)
                        .addToBackStack(null)
                        .commit();
                mNavDrawer.setSelectedSection(NavDrawer.NAV_DRAWER_ITEM.LIBRARY);
            }
        }
    }

    private void initBroadcastReceivers() {
//        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                int colorPrimary = getResources().getColor(R.color.color_primary);
//                int newColor = intent.getIntExtra(AlbumArtLoader.BROADCAST_COLOR_CHANGED_PRIMARY, colorPrimary);
//                //toolbar disabled for now
//                Drawable d = mToolbar.getBackground();
//                int oldColor = newColor;
//                if (d instanceof ColorDrawable) {
//                    oldColor = ((ColorDrawable) d).getColor();
//                }
//                final boolean useAlpha = !(newColor == colorPrimary);
//
//                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, newColor);
//                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animator) {
//                        int color = (Integer) animator.getAnimatedValue();
//                        int colorWithAlpha = Color.argb(150, Color.red(color), Color.green(color),
//                                Color.blue(color));
//                        float[] hsv = new float[3];
//                        Color.colorToHSV(color, hsv);
//                        hsv[2] *= 0.8f; // value component
//                        int darkColor = Color.HSVToColor(hsv);
//                        int darkColorWithAlpha = Color.argb(150, Color.red(darkColor), Color.green(darkColor),
//                                Color.blue(darkColor));
//                        //toolbar disabled for now
//                        mToolbar.setBackgroundColor(useAlpha ? colorWithAlpha : color);
//                        if (Build.VERSION.SDK_INT >= 21) {
//                            getWindow().setStatusBarColor(useAlpha ? darkColorWithAlpha : darkColor);
//                        }
//                    }
//
//                });
//                colorAnimation.start();
//
//            }
//        }, new IntentFilter(AlbumArtLoader.BROADCAST_COLOR_CHANGED));
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        int i = getFragmentManager().getBackStackEntryCount();
        if (i == 1) {   //we are at the root fragment
            //if the nav drawer isn't open, then open it before closing the app
            if (!mDrawerLayout.isDrawerOpen(Gravity.START)) {
                mDrawerLayout.openDrawer(Gravity.START);
            } else {
                mDrawerLayout.closeDrawers();
                finish();
            }
        } else {
            getFragmentManager().popBackStackImmediate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    protected void onStart() {
        super.onStart();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(NavDrawer.NAV_DRAWER_ITEM item) {
        Fragment fragment = null;
        String title = "";

        switch (item) {
            case CUR_PLAYING:
                mToolbar.setTitle("Currently Playing");
                fragment = new NowPlayingFragment();
                mNavDrawer.setSelectedSection(NavDrawer.NAV_DRAWER_ITEM.CUR_PLAYING);
                break;
            case SETTINGS:
                fragment = new SettingsFragment();
                title = "Settings";
                mNavDrawer.setSelectedSection(NavDrawer.NAV_DRAWER_ITEM.SETTINGS);
                break;
            case QUEUE:
                //todo
//                mToolbar.setTitle("Play Queue");
//                mNavDrawer.setSelectedSection(NavDrawer.NAV_DRAWER_ITEM.QUEUE);
                break;
            case LIBRARY:
                //todo
                fragment = new LibraryFragment();
                mToolbar.setTitle("Library");
                mNavDrawer.setSelectedSection(NavDrawer.NAV_DRAWER_ITEM.LIBRARY);
                break;
            case HELP:
                fragment = new SettingsFragment();
                title = "Help";
                break;
        }
        mDrawerLayout.closeDrawers();
        if (fragment != null && mFragmentContainer != null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, fragment)
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                    .commit();
            mToolbar.setTitle(title);
            mNavDrawer.setSelectedSection(item);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //TODO
    }

    @Override
    public void setToolbarTitle(String title) {
        if (title != null) {
            mToolbar.setTitle(title);
        }
    }

    @Override
    public void setToolbarColor(int color) {
        int colorPrimary = getResources().getColor(R.color.color_primary);
        int newColor = color;
        //toolbar disabled for now
        Drawable d = mToolbar.getBackground();
        int oldColor = newColor;
        if (d instanceof ColorDrawable) {
            oldColor = ((ColorDrawable) d).getColor();
        }
        final boolean useAlpha = !(newColor == colorPrimary);

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, newColor);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (Integer) animator.getAnimatedValue();
                int colorWithAlpha = Color.argb(150, Color.red(color), Color.green(color),
                        Color.blue(color));
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                hsv[2] *= 0.8f; // value component
                int darkColor = Color.HSVToColor(hsv);
                int darkColorWithAlpha = Color.argb(150, Color.red(darkColor), Color.green(darkColor),
                        Color.blue(darkColor));
                //toolbar disabled for now
                mToolbar.setBackgroundColor(useAlpha ? colorWithAlpha : color);
                if (Build.VERSION.SDK_INT >= 21) {
                    getWindow().setStatusBarColor(useAlpha ? darkColorWithAlpha : darkColor);
                }
            }

        });
        colorAnimation.start();
    }

    @Override
    public void setToolbarTransparent() {
        //TODO
    }
}
