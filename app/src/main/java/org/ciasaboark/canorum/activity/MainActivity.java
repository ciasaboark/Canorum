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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.fragment.HelpFragment;
import org.ciasaboark.canorum.fragment.LibraryWrapperFragment;
import org.ciasaboark.canorum.fragment.NowPlayingFragment;
import org.ciasaboark.canorum.fragment.OnFragmentInteractionListener;
import org.ciasaboark.canorum.fragment.QueueWrapperFragment;
import org.ciasaboark.canorum.fragment.SettingsFragment;
import org.ciasaboark.canorum.view.NavDrawerView;


public class MainActivity extends ActionBarActivity implements NavDrawerView.NavDrawerListener, OnFragmentInteractionListener {
    private static final String TAG = "MainActivity";
    private MusicControllerSingleton musicControllerSingleton;
    private NavDrawerView mNavDrawer;
    private DrawerLayout mDrawerLayout;

    private FrameLayout mFragmentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        FragmentManager fm = getSupportFragmentManager();
        int i = fm.getBackStackEntryCount();
        if (i == 1) {   //we are at the root fragment
            //if the nav drawer isn't open, then open it before closing the app
            if (!mDrawerLayout.isDrawerOpen(Gravity.START)) {
                mDrawerLayout.openDrawer(Gravity.START);
            } else {
                finish();
            }
        } else {
            fm.popBackStackImmediate();
        }
    }

    private void initToolbar() {

    }

    @Override
    public void setToolbar(Toolbar toolbar) {
        Menu menu = toolbar.getMenu();
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);


        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_closed);
        mDrawerLayout.setDrawerListener(actionBarDrawerToggle);
    }

    @Override
    public void onPaletteGenerated(Palette palette) {
        int headerColor = palette.getVibrantColor(
                palette.getDarkVibrantColor(
                        palette.getMutedColor(
                                palette.getDarkMutedColor(getResources().getColor(R.color.color_primary))
                        )
                )
        );
        int oldHeaderColor = getResources().getColor(R.color.color_primary);
        Drawable d = mNavDrawer.getHeaderDrawable();
        if (d instanceof ColorDrawable) {
            oldHeaderColor = ((ColorDrawable) d).getColor();
        }
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldHeaderColor, headerColor);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (Integer) animator.getAnimatedValue();
//                        float[] hsv = new float[3];
//                        Color.colorToHSV(color, hsv);
//                        hsv[2] *= 0.8f; // value component
//                        int darkColor = Color.HSVToColor(hsv);
//                        int darkColorWithAlpha = Color.argb(150, Color.red(darkColor), Color.green(darkColor),
//                                Color.blue(darkColor));
                mNavDrawer.setHeaderDrawable(new ColorDrawable(color));
            }

        });
        colorAnimation.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void onStart() {
        super.onStart();

        initNavDrawer();

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        musicControllerSingleton = MusicControllerSingleton.getInstance(this);

        //TODO start with the appropriate fragment
        mFragmentContainer = (FrameLayout) findViewById(R.id.main_fragment);
        if (mFragmentContainer != null) {
            if (musicControllerSingleton.isPlaying()) {
                NowPlayingFragment nowPlayingFragment = new NowPlayingFragment();
                nowPlayingFragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.main_fragment, nowPlayingFragment)
                        .commit();
                mNavDrawer.setSelectedSection(NavDrawerView.NAV_DRAWER_ITEM.CUR_PLAYING);
            } else {
                LibraryWrapperFragment libraryFragment = new LibraryWrapperFragment();
                libraryFragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .add(R.id.main_fragment, libraryFragment)
                        .commit();
                mNavDrawer.setSelectedSection(NavDrawerView.NAV_DRAWER_ITEM.LIBRARY);
            }
        }
    }

    private void initNavDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavDrawer = (NavDrawerView) findViewById(R.id.nav_drawer);
        mNavDrawer.setListener(this);
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
    public void onItemSelected(NavDrawerView.NAV_DRAWER_ITEM item) {
        Fragment fragment = null;

        switch (item) {
            case CUR_PLAYING:
                fragment = NowPlayingFragment.newInstance();
                mNavDrawer.setSelectedSection(NavDrawerView.NAV_DRAWER_ITEM.CUR_PLAYING);
                break;
            case SETTINGS:
                fragment = SettingsFragment.newInstance();
                mNavDrawer.setSelectedSection(NavDrawerView.NAV_DRAWER_ITEM.SETTINGS);
                break;
            case QUEUE:
                fragment = QueueWrapperFragment.newInstance();
                mNavDrawer.setSelectedSection(NavDrawerView.NAV_DRAWER_ITEM.QUEUE);
                break;
            case LIBRARY:
                fragment = LibraryWrapperFragment.newInstance();
                mNavDrawer.setSelectedSection(NavDrawerView.NAV_DRAWER_ITEM.LIBRARY);
                break;
            case HELP:
                fragment = HelpFragment.newInstance();
                break;
        }
        mDrawerLayout.closeDrawers();
        if (fragment != null && mFragmentContainer != null) {
            FragmentManager fm = getSupportFragmentManager();

            fm.beginTransaction()
                    .replace(R.id.main_fragment, fragment)
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_up)
                    .commit();
            mNavDrawer.setSelectedSection(item);
        }
    }
}
