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
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.fragment.HelpFragment;
import org.ciasaboark.canorum.fragment.LibraryWrapperFragment;
import org.ciasaboark.canorum.fragment.NowPlayingFragment;
import org.ciasaboark.canorum.fragment.OnFragmentInteractionListener;
import org.ciasaboark.canorum.fragment.PlaylistLibraryWrapperFragment;
import org.ciasaboark.canorum.fragment.QueueWrapperFragment;
import org.ciasaboark.canorum.fragment.RecentsWrapperFragment;
import org.ciasaboark.canorum.fragment.SettingsFragment;
import org.ciasaboark.canorum.fragment.TOP_LEVEL_FRAGMENTS;
import org.ciasaboark.canorum.receiver.ServiceStateReceiver;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        initNavDrawer();

        ServiceStateReceiver widgetUpdateListener = new ServiceStateReceiver();
        IntentFilter widgetFilter = new IntentFilter(MusicControllerSingleton.ACTION_PLAY);
        widgetFilter.addAction(MusicControllerSingleton.ACTION_PREV);
        widgetFilter.addAction(MusicControllerSingleton.ACTION_NEXT);
        widgetFilter.addAction(MusicControllerSingleton.ACTION_PAUSE);
        widgetFilter.addAction(MusicControllerSingleton.ACTION_STOP);
        LocalBroadcastManager.getInstance(this).registerReceiver(widgetUpdateListener, widgetFilter);

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mFragmentContainer = (FrameLayout) findViewById(R.id.main_fragment);
        if (mFragmentContainer != null) {
            navigateToTopLevelFragment(TOP_LEVEL_FRAGMENTS.LIBRARY);
        }

        ServiceStateReceiver.updateAllWidgets(this);
    }


    private void initNavDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(android.R.color.transparent));
        mNavDrawer = (NavDrawerView) findViewById(R.id.nav_drawer);
        mNavDrawer.setListener(this);
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
        if (i == 0) {   //we are at the root fragment
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
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //TODO save current fragment?
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onPaletteGenerated(final Palette palette) {
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
                mNavDrawer.setHeaderDrawable(new ColorDrawable(color));
                mNavDrawer.setPalette(palette);
            }

        });
        colorAnimation.start();
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
    public void navigateToTopLevelFragment(TOP_LEVEL_FRAGMENTS fragmentName) {
        Fragment fragment = null;

        switch (fragmentName) {
            case CUR_PLAYING:
                fragment = NowPlayingFragment.newInstance();
                mNavDrawer.setSelectedSection(TOP_LEVEL_FRAGMENTS.CUR_PLAYING);
                break;
            case SETTINGS:
                fragment = SettingsFragment.newInstance();
                mNavDrawer.setSelectedSection(TOP_LEVEL_FRAGMENTS.SETTINGS);
                break;
            case QUEUE:
                fragment = QueueWrapperFragment.newInstance();
                mNavDrawer.setSelectedSection(TOP_LEVEL_FRAGMENTS.QUEUE);
                break;
            case LIBRARY:
                fragment = LibraryWrapperFragment.newInstance();
                mNavDrawer.setSelectedSection(TOP_LEVEL_FRAGMENTS.LIBRARY);
                break;
            case RECENTS:
                fragment = RecentsWrapperFragment.newInstance();
                mNavDrawer.setSelectedSection(TOP_LEVEL_FRAGMENTS.RECENTS);
                break;
            case PLAYLISTS:
                fragment = PlaylistLibraryWrapperFragment.newInstance();
                mNavDrawer.setSelectedSection(TOP_LEVEL_FRAGMENTS.PLAYLISTS);
                break;
            case HELP:
                fragment = HelpFragment.newInstance();
                mNavDrawer.setSelectedSection(TOP_LEVEL_FRAGMENTS.HELP);
                break;
        }
        mDrawerLayout.closeDrawers();
        if (fragment != null && mFragmentContainer != null) {
            FragmentManager fm = getSupportFragmentManager();
            //clear the fragment backstack
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            fm.beginTransaction()
                    .replace(R.id.main_fragment, fragment)
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_up)
                    .commit();
            mNavDrawer.setSelectedSection(fragmentName);
        }
    }

    @Override
    public void onItemSelected(TOP_LEVEL_FRAGMENTS item) {
        navigateToTopLevelFragment(item);
    }
}
