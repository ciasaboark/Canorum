package org.ciasaboark.canorum;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

public class MusicController extends RelativeLayout {
    private Context mContext;
    private SimpleMediaPlayerControl mMediaPlayerController;
    private RelativeLayout mLayout;
    private SeekBar mSeekBar;
    private ImageView mShuffleButton;
    private ImageView mRepeatButton;
    private ImageView mPrevButton;
    private ImageView mNextButton;
    private ImageView mPlayButton;
    private OnClickListener mPrevListener;
    private OnClickListener mNextListener;
    private boolean mIsEnabled = true;
    private RepeatMode mRepeatMode = RepeatMode.ALL;
    private ShuffleMode mShuffleMode = ShuffleMode.SIMPLE;
    private OnClickListener mPlayListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mMediaPlayerController.play();
        }
    };
    private OnClickListener mPauseListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mMediaPlayerController.pause();
        }
    };
    private OnClickListener mShuffleListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO
            Toast.makeText(mContext, "not yet implemented", Toast.LENGTH_SHORT).show();
        }
    };
    private OnClickListener mRepeatListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO
            Toast.makeText(mContext, "not yet implemented", Toast.LENGTH_SHORT).show();
        }
    };


    public MusicController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public MusicController(Context context){
        this(context, null);
    }

    public void setMediaPlayerController(SimpleMediaPlayerControl mpc) {
        if (mpc == null) {
            throw new IllegalArgumentException("Media Player Controll can not be null");
        }
        mMediaPlayerController = mpc;
        updatePlayPause();
        updateRepeat();
        updateShuffle();
        updateSeekBar();
    }

    private void updateSeekBar() {
        if (mMediaPlayerController == null) {
            //TODO make sure looks disabled
            mSeekBar.setProgress(0);
            mSeekBar.setMax(1);
            mSeekBar.setEnabled(false);
        } else if (mMediaPlayerController.isPlaying()) {
            mSeekBar.setMax(mMediaPlayerController.getDuration());
            mSeekBar.setProgress(mMediaPlayerController.getCurrentPosition());
            mSeekBar.setEnabled(true);
        } else {
            //TODO this should show the current position when paused, otherwise nothing if not playing
            mSeekBar.setEnabled(false);
        }
    }

    private void init() {
        mLayout = (RelativeLayout) inflate(getContext(), R.layout.media_controls, this);
        mSeekBar = (SeekBar) mLayout.findViewById(R.id.controls_seekbar);
        mShuffleButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_rand);
        mRepeatButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_repeat);
        mPrevButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_prev);
        mNextButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_next);
        mPlayButton = (ImageView) mLayout.findViewById(R.id.controls_button_media_play);
        attachStaticListeners();
        updateWidgets();
    }

    public void updateWidgets() {
        updatePlayPause();
        updateShuffle();
        updateRepeat();
        updatePlayPause();
        updateSeekBar();
//        updatePrevNext(); //TODO
    }

    private void attachStaticListeners() {
        mShuffleButton.setOnClickListener(mShuffleListener);
        mRepeatButton.setOnClickListener(mRepeatListener);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (mMediaPlayerController != null) {
                        mMediaPlayerController.seekTo(progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void updatePlayPause() {
        if (mMediaPlayerController == null) {
            //no media controller has been specified yet, disable this button
            mPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.controls_play)); //TODO tint this to make it look disabled
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

    private void updateShuffle() {
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

    private void updateRepeat() {
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

    /**
     * Set the OnClick listener for the previous and next buttons.  Passing null will unset any
     * previously attached listeners
     * @param prevListener
     * @param nextListener
     */
    public void setPrevNextListeners(OnClickListener prevListener, OnClickListener nextListener) {
        mPrevListener = prevListener;
        mNextListener = nextListener;
        mPrevButton.setOnClickListener(prevListener);
        mNextButton.setOnClickListener(nextListener);
    }

    public void setShuffleListener(OnClickListener shuffleListener) {
        mShuffleButton.setOnClickListener(shuffleListener);
    }

    public void setRepeatListener(OnClickListener repeatListener) {
        mRepeatButton.setOnClickListener(repeatListener);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        mIsEnabled = isEnabled;
    }

    public interface SimpleMediaPlayerControl {
        public void    play();
        public void    pause();
        public boolean    hasPrev();
        public void    playNext();
        public boolean    hasNext();
        public void    playPrev();
        public int     getDuration();
        public int     getCurrentPosition();
        public void    seekTo(int pos);
        public boolean isPlaying();
        public RepeatMode getRepeatMode();
        public ShuffleMode getShuffleMode();
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
}
