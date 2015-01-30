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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.canorum.MusicControllerSingleton;
import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.artwork.AlbumArtLoader;
import org.ciasaboark.canorum.prefs.ShufflePrefs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Formatter;
import java.util.Locale;

public class MusicController extends RelativeLayout {
    private static final String TAG = "MusicController";
    private static final int SHOW_PROGRESS = 1;
    private Context mContext;
    private SimpleMediaPlayerControl mMediaPlayerController;
    private RelativeLayout mLayout;
    private View mMediaControls;
    private SeekBar mSeekBar;
    private ImageView mShuffleButton;
    private ImageView mRepeatButton;
    private ImageView mPrevButton;
    private ImageView mNextButton;
    private ImageView mPlayButton;
    private TextView mProgressText;
    private TextView mDurationText;
    private OnClickListener mPrevListener;
    private OnClickListener mNextListener;
    private boolean mIsEnabled = true;
    private RepeatMode mRepeatMode = RepeatMode.ALL;
    private ShuffleMode mShuffleMode = ShuffleMode.SIMPLE;
    private LocalBroadcastManager mBroadcastManager;
    private BroadcastReceiver mReceiver;
    private Handler mHandler;
    private boolean mIsSeekbarDragging = false;
    private Drawable mSeekbarThumb;
    private PopupMenu mRepeatPopupMenu;
    private PopupMenu mShufflePopupMenu;
    private OnClickListener mPlayListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mMediaPlayerController.play();
            updatePlayPause();
        }
    };
    private OnClickListener mPauseListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mMediaPlayerController.pause();
            updatePlayPause();
        }
    };
    private OnClickListener mShuffleListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mShufflePopupMenu.show();
        }
    };
    private OnClickListener mRepeatListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mRepeatPopupMenu.show();
        }
    };


    public MusicController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public MusicController(Context context) {
        this(context, null);
    }

    public void setMediaPlayerController(SimpleMediaPlayerControl mpc) {
        Log.d(TAG, "setMediaPlayerController()");
        if (mpc == null) {
            throw new IllegalArgumentException("Media Player Control can not be null");
        }
        mMediaPlayerController = mpc;
        updatePlayPause();
        updatePrevNext();
        updateRepeat();
        updateShuffle();
        updateSeekBar();
    }

    private void updatePlayPause() {
        Log.d(TAG, "updatePlayPause()");
        if (mMediaPlayerController == null || mMediaPlayerController.isEmpty()) {
            //no media controller has been specified yet, disable this button
            Drawable d = getResources().getDrawable(R.drawable.controls_play);
            d.mutate().setColorFilter(getResources().getColor(R.color.controls_disabled), PorterDuff.Mode.MULTIPLY);
            mPlayButton.setImageDrawable(d);
            mPlayButton.setEnabled(false);
            mPlayButton.setOnClickListener(null);
        } else if (mMediaPlayerController.isPlaying()) {
            mPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_pause));
            mPlayButton.setOnClickListener(mPauseListener);
            mPlayButton.setEnabled(true);
        } else {
            mPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_play));
            mPlayButton.setOnClickListener(mPlayListener);
            mPlayButton.setEnabled(true);
        }
    }

    private void updateRepeat() {
        Log.d(TAG, "updateRepeat()");
        Drawable d;
        int colorFilter;
        switch (mRepeatMode) {
            case ALL:
                d = getResources().getDrawable(R.drawable.controls_repeat_all);
                colorFilter = getResources().getColor(R.color.controls_repeat_all);
                break;
            case SINGLE:
                d = getResources().getDrawable(R.drawable.controls_repeat_one);
                colorFilter = getResources().getColor(R.color.controls_repeat_single);
                break;
            default:
                //mRepeatMode == NONE
                //TODO need graphic for this
                d = getResources().getDrawable(R.drawable.android_music_player_end);
                colorFilter = getResources().getColor(R.color.controls_repeat_none);
        }

        if (mMediaPlayerController == null) {
            colorFilter = getResources().getColor(R.color.controls_repeat_disabled);
            mRepeatButton.setEnabled(false);
        } else {
            mRepeatButton.setEnabled(true);
        }

        d.mutate().setColorFilter(colorFilter, PorterDuff.Mode.MULTIPLY);
        mRepeatButton.setImageDrawable(d);
    }

    private void updateShuffle() {
        Log.d(TAG, "updateShuffle()");
        if (mMediaPlayerController == null) {
            //TODO desatureate
            mShuffleButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_shuffle));
            mShuffleButton.setEnabled(false);
        } else {
            switch (mShuffleMode) {
                case OFF:
                    //TODO need graphic for this
                    mShuffleButton.setImageDrawable(getResources().getDrawable(R.drawable.android_music_player_rand));
                    mShuffleButton.setEnabled(true);
                    break;
                case SIMPLE:
                    mShuffleButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_shuffle));
                    mShuffleButton.setEnabled(true);
                    break;
            }
        }
    }

    private void updateSeekBar() {
//        Log.d(TAG, "updateSeekbar()");
        String durationText;
        String progressText;
        mSeekBar.setThumb(null);
        int duration;
        int progress;
        boolean showTextViews = true;
        boolean seekbarEnabled = false;

        if (mMediaPlayerController == null) {
            duration = 0;
            progress = 0;
            showTextViews = false;
        } else {
            duration = mMediaPlayerController.getDuration();
            progress = mMediaPlayerController.getCurrentPosition();
            if (duration <= 0) {
                showTextViews = false;
            }
            if (mMediaPlayerController.isPlaying()) {
                seekbarEnabled = true;
            }
        }

        durationText = getFormattedTime(duration);
        progressText = getFormattedTime(progress);
        mDurationText.setText(durationText);
        mProgressText.setText(progressText);
        mSeekBar.setMax(duration);
        mSeekBar.setProgress(progress);
        mSeekBar.setEnabled(seekbarEnabled);

        if (showTextViews) {
            mDurationText.setVisibility(View.VISIBLE);
            mProgressText.setVisibility(View.VISIBLE);
        } else {
            mDurationText.setVisibility(View.INVISIBLE);
            mProgressText.setVisibility(View.INVISIBLE);
        }
    }

    private String getFormattedTime(int durationMs) {
//        Log.d(TAG, "getFormattedTime()");
        int totalSeconds = durationMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        Formatter formatter = new Formatter(sb, Locale.getDefault());
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private void updatePrevNext() {
        Log.d(TAG, "updatePrevNext()");
        updatePrev();
        updateNext();
    }

    private void updatePrev() {
        if (mMediaPlayerController == null || !mMediaPlayerController.hasPrev()) {
            Drawable d = getResources().getDrawable(R.drawable.controls_prev);
            d.mutate().setColorFilter(getResources().getColor(R.color.controls_disabled), PorterDuff.Mode.MULTIPLY);
            mPrevButton.setImageDrawable(d);
            mPrevButton.setEnabled(false);
            mPrevButton.setOnClickListener(null);
        } else {
            mPrevButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_prev));
            mPrevButton.setEnabled(true);
            mPrevButton.setOnClickListener(mPrevListener);
        }
    }

    private void updateNext() {
        if (mMediaPlayerController == null || !mMediaPlayerController.hasNext()) {
            Drawable d = getResources().getDrawable(R.drawable.controls_next);
            d.mutate().setColorFilter(getResources().getColor(R.color.controls_disabled), PorterDuff.Mode.MULTIPLY);
            mNextButton.setImageDrawable(d);
            mNextButton.setEnabled(false);
            mNextButton.setOnClickListener(null);
        } else {
            mNextButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_next));
            mNextButton.setEnabled(true);
            mNextButton.setOnClickListener(mNextListener);
        }
    }

    private void init() {
        Log.d(TAG, "init()");
        mLayout = (RelativeLayout) inflate(getContext(), R.layout.media_controls, this);
        mMediaControls = mLayout.findViewById(R.id.cur_play_media_controls);
        mSeekBar = (SeekBar) mLayout.findViewById(R.id.controls_seekbar);
        /* TODO this is a bit of a hack, the seekbar will use the default thumb drawable, which we
        cache here, then immediately, set to null so that nothing shows up until we detect a touch
        event on the seekbar
         */
        mSeekbarThumb = mSeekBar.getThumb();
        mShuffleButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_rand);
        mRepeatButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_repeat);
        mPrevButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_prev);
        mNextButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_next);
        mPlayButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_play);
        mProgressText = (TextView) mLayout.findViewById(R.id.controls_text_progress);
        mDurationText = (TextView) mLayout.findViewById(R.id.controls_text_duration);
        attachStaticListeners();
        createPopupMenus();
        updateWidgets();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
//                Log.d(TAG, "handleMessage()");
                switch (msg.what) {
                    case SHOW_PROGRESS:
                        updateSeekBar();
                        if (mMediaPlayerController != null && mMediaPlayerController.isPlaying()) {
                            msg = obtainMessage(SHOW_PROGRESS);
                            sendMessageDelayed(msg, 1000);
                        }
                        break;
                    default:
                        Log.d(TAG, "unknown message received, ignoring");
                }
            }
        };
        initBroadcastReceivers();
    }

    private void createPopupMenus() {
        Log.d(TAG, "createPopupMenus()");
        mRepeatPopupMenu = new PopupMenu(mContext, mRepeatButton);
        mRepeatPopupMenu.inflate(R.menu.popup_repeat);
        mRepeatPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                boolean itemHamdled = false;
                switch (item.getItemId()) {
                    case R.id.popup_menu_repeat_off:
                        //TODO store this setting
                        Toast.makeText(mContext, "selected repeat off", Toast.LENGTH_SHORT).show();
                        itemHamdled = true;
                        break;
                    case R.id.popup_menu_repeat_one:
                        //TODO store this setting
                        Toast.makeText(mContext, "selected repeat one", Toast.LENGTH_SHORT).show();
                        itemHamdled = true;
                        break;
                    case R.id.popup_menu_repeat_all:
                        //TODO store this setting
                        Toast.makeText(mContext, "selected repeat all", Toast.LENGTH_SHORT).show();
                        itemHamdled = true;
                        break;
                }

                updateRepeat();
                return itemHamdled;
            }
        });
//        tryEnableMenuIcons(mRepeatPopupMenu);


        mShufflePopupMenu = new PopupMenu(mContext, mShuffleButton);
        mShufflePopupMenu.inflate(R.menu.popup_shuffle);
        mShufflePopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                boolean itemHandled = false;
                ShufflePrefs shufflePrefs = new ShufflePrefs(mContext);
                switch (item.getItemId()) {
                    case R.id.popup_menu_shuffle_weighted:
                        Toast.makeText(mContext, "selected shuffle weighted", Toast.LENGTH_SHORT).show();
                        shufflePrefs.setShuffleMode(ShufflePrefs.Mode.WEIGHTED_RANDOM);
                        itemHandled = true;
                        break;
                    case R.id.popup_menu_shuffle_least_often_played:
                        Toast.makeText(mContext, "selected shuffle least often played", Toast.LENGTH_SHORT).show();
                        shufflePrefs.setShuffleMode(ShufflePrefs.Mode.LEAST_RECENTLY_PLAYED);
                        itemHandled = true;
                        break;
                    case R.id.popup_menu_shuffle_true_random:
                        Toast.makeText(mContext, "selected shuffle true random", Toast.LENGTH_SHORT).show();
                        shufflePrefs.setShuffleMode(ShufflePrefs.Mode.RANDOM);
                        itemHandled = true;
                        break;
                    case R.id.popup_menu_shuffle_off:
                        //TODO store this setting
                        Toast.makeText(mContext, "selected shuffle off", Toast.LENGTH_SHORT).show();
                        shufflePrefs.setShuffleMode(ShufflePrefs.Mode.LINEAR);
                        itemHandled = true;
                        break;
                }
                updateShuffle();
                return itemHandled;
            }
        });
//        tryEnableMenuIcons(mShufflePopupMenu);
    }

    private void tryEnableMenuIcons(PopupMenu popupMenu) {
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper
                            .getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod(
                            "setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initBroadcastReceivers() {
        Log.d(TAG, "initBroadcastReceivers()");
        //Got a notification that music has began playing
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updatePlayPause();
                updateSeekBar();
                updatePrevNext();
                mHandler.sendEmptyMessage(SHOW_PROGRESS);
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_PLAY));

        //Got a notification that the seek progress has been changed
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mHandler.sendEmptyMessage(SHOW_PROGRESS);
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_SEEK));

        //Got a notification that the music has been paused
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updatePlayPause();
                updatePrevNext();
                updateSeekBar();
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_PAUSE));

        //Got a notification that the next track is playing
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updatePlayPause();
                updatePrevNext();
                updateSeekBar();
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_NEXT));

        //Got a notification that the prev track is playing
        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updatePlayPause();
                updatePrevNext();
                updateSeekBar();
            }
        }, new IntentFilter(MusicControllerSingleton.ACTION_PREV));

        LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int newColor = intent.getIntExtra(AlbumArtLoader.BROADCAST_COLOR_CHANGED_PRIMARY, getResources().getColor(R.color.color_primary));

                int oldColor = getResources().getColor(R.color.color_primary);
                Drawable backgroundDrawable = mMediaControls.getBackground();
                if (backgroundDrawable instanceof ColorDrawable) {
                    oldColor = ((ColorDrawable) backgroundDrawable).getColor();
                }

                if (oldColor != newColor) {
                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, newColor);
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            int color = (Integer) animator.getAnimatedValue();
                            mMediaControls.setBackgroundColor(color);
                        }

                    });
                    colorAnimation.start();
                }

                int defaultAccentColor = getResources().getColor(R.color.color_accent);
                int newAccentColor = intent.getIntExtra(AlbumArtLoader.BROADCAST_COLOR_CHANGED_ACCENT, defaultAccentColor);
                int oldAccentColor = defaultAccentColor;
                Drawable seekbarBackgroundDrawable = mSeekBar.getProgressDrawable();
                if (seekbarBackgroundDrawable instanceof ColorDrawable) {
                    oldAccentColor = ((ColorDrawable) seekbarBackgroundDrawable).getColor();
                }

                //TODO this does change the seekbar color, but fills the entire seekbar with it.  Find a way to only change the current progress part (will probably need to crate a drawable to use for progress)
//                if (oldAccentColor != newAccentColor) {
//                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldAccentColor, newAccentColor);
//                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//
//                        @Override
//                        public void onAnimationUpdate(ValueAnimator animator) {
//                            int color = (Integer)animator.getAnimatedValue();
//                            mSeekBar.setProgressDrawable(new ColorDrawable(color));
//                        }
//
//                    });
//                    colorAnimation.start();
//                }

            }
        }, new IntentFilter(AlbumArtLoader.BROADCAST_COLOR_CHANGED));

    }

    public void updateWidgets() {
        Log.d(TAG, "updateWidgets()");
        updatePlayPause();
        updateShuffle();
        updateRepeat();
        updatePlayPause();
        updateSeekBar();
        updatePrevNext(); //TODO
    }

    private void attachStaticListeners() {
        Log.d(TAG, "attachStaticListeners()");
        mShuffleButton.setOnClickListener(mShuffleListener);
        mRepeatButton.setOnClickListener(mRepeatListener);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private Drawable thumb;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (mMediaPlayerController != null) { //TODO ignore dragging until finger lifted
                        mMediaPlayerController.seekTo(progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsSeekbarDragging = true;
                mSeekBar.setThumb(mSeekbarThumb);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsSeekbarDragging = false;
                mSeekBar.setThumb(null);
            }
        });
    }

    /**
     * Set the OnClick listener for the previous and next buttons.  Passing null will unset any
     * previously attached listeners
     *
     * @param prevListener
     * @param nextListener
     */
    public void setPrevNextListeners(OnClickListener prevListener, OnClickListener nextListener) {
        Log.d(TAG, "setPrevNextListeners()");
        mPrevListener = prevListener;
        mNextListener = nextListener;
        mPrevButton.setOnClickListener(prevListener);
        mNextButton.setOnClickListener(nextListener);
    }

    public void setShuffleListener(OnClickListener shuffleListener) {
        Log.d(TAG, "setShuffleListener()");
        mShuffleButton.setOnClickListener(shuffleListener);
    }

    public void setRepeatListener(OnClickListener repeatListener) {
        Log.d(TAG, "setRepeatListener()");
        mRepeatButton.setOnClickListener(repeatListener);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        Log.d(TAG, "setEnabled()");
        mIsEnabled = isEnabled;
    }

    public enum RepeatMode {
        NONE,
        ALL,
        SINGLE
    }

    public enum ShuffleMode {
        OFF,
        SIMPLE
    }

    public interface SimpleMediaPlayerControl {
        public void play();

        public void pause();

        public void stop();

        public boolean hasPrev();

        public void playNext();

        public boolean hasNext();

        public void playPrev();

        public int getDuration();

        public int getCurrentPosition();

        public void seekTo(int pos);

        public boolean isPlaying();

        public RepeatMode getRepeatMode();

        public ShuffleMode getShuffleMode();

        public boolean isReady();

        public boolean isPaused();

        public boolean isEmpty();
    }
}
