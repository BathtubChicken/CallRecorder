package net.synapticweb.callrecorder.player;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

interface PlayerAdapter {

    @IntDef({State.UNINITIALIZED, State.INITIALIZED, State.PLAYING, State.PAUSED })
    @Retention(RetentionPolicy.SOURCE)
    @interface State {
        int UNINITIALIZED = 0;
        int INITIALIZED = 1;
        int PLAYING = 2;
        int PAUSED = 3;
        int STOPPED = 4;
    }

    void setGain(float gain);

    void setMediaPosition(int position);

    void loadMedia(String mediaPath);

    void stopPlayer();

    void play();

    void reset();

    void pause();

    int getPlayerState();

    void setPlayerState(int state);

    void seekTo(int position);

    int getCurrentPosition();

    long getTotalDuration();
}
