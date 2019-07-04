package net.synapticweb.callrecorder.player;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.TemplateActivity;
import net.synapticweb.callrecorder.contactdetail.ContactDetailPresenter;
import net.synapticweb.callrecorder.data.Recording;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.chibde.visualizer.LineBarVisualizer;
import com.sdsmdg.harjot.crollerTest.Croller;

public class PlayerActivity extends TemplateActivity {
    final static String TAG = "CallRecorder";
    AudioPlayer player;
    Recording recording;
    ImageButton playPause, resetPlaying;
    SeekBar playSeekBar;
    TextView playedTime, totalTime;
    boolean userIsSeeking = false;
    LineBarVisualizer visualizer;
    AudioManager audioManager;
    int phoneVolume;
    Croller gainControl;
    final static int AUDIO_SESSION_ID = 0;
    final static String IS_PLAYING = "is_playing";
    final static String CURRENT_POS = "current_pos";
    final static int DENSITY_PORTRAIT = 70;
    final static int DENSITY_LANDSCAPE = 150;

    public Fragment createFragment() {return null;}

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setContentView(R.layout.player_activity);

        recording = getIntent().getParcelableExtra(ContactDetailPresenter.RECORDING_EXTRA);
        visualizer = findViewById(R.id.visualizer);
        visualizer.setColor(getResources().getColor(R.color.colorAccentLighter));
        visualizer.setDensity(getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT ? DENSITY_PORTRAIT : DENSITY_LANDSCAPE);
        visualizer.setPlayer(AUDIO_SESSION_ID);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        playPause = findViewById(R.id.test_player_play_pause);
        resetPlaying = findViewById(R.id.test_player_reset);
        playSeekBar = findViewById(R.id.play_seekbar);
        playedTime = findViewById(R.id.test_play_time_played);
        totalTime = findViewById(R.id.test_play_total_time);

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player.getPlayerState() == PlayerAdapter.State.PLAYING) {
                    player.pause();
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else if(player.getPlayerState() == PlayerAdapter.State.PAUSED ||
                        player.getPlayerState() == PlayerAdapter.State.INITIALIZED){
                    player.play();
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_pause));
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });

        resetPlaying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player.getPlayerState() == PlayerAdapter.State.PLAYING)
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
                player.reset();
            }
        });

        playSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int userSelectedPosition = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)
                    userSelectedPosition = progress;
                playedTime.setText(CrApp.getDurationHuman(progress, false));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { userIsSeeking = true; }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userIsSeeking = false;
                player.seekTo(userSelectedPosition);
            }
        });

        gainControl = findViewById(R.id.gain_control);
        gainControl.setOnProgressChangedListener(new Croller.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                player.setGain((float) progress);
            }
        });

        Croller volumeControl = findViewById(R.id.volume_control);
        if(audioManager != null) {
            volumeControl.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            phoneVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumeControl.setProgress(phoneVolume);
        }
        volumeControl.setOnProgressChangedListener(new Croller.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
        });

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            visualizer.setDensity(DENSITY_LANDSCAPE);
        else
            visualizer.setDensity(DENSITY_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        if(player.getPlayerState() == PlayerAdapter.State.UNINITIALIZED ||
//                player.getPlayerState() == PlayerAdapter.State.STOPPED) {
        player = new AudioPlayer(new PlaybackListener());
        player.loadMedia(recording.getPath());
        playedTime.setText("00:00");
        totalTime.setText(CrApp.getDurationHuman(player.getTotalDuration(), false));
        player.setGain(gainControl.getProgress());
//        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int currentPosition = pref.getInt(CURRENT_POS, 0);
        boolean isPlaying = pref.getBoolean(IS_PLAYING, true);
        player.setMediaPosition(currentPosition);

        if(isPlaying) {
            playPause.setBackground(getResources().getDrawable(R.drawable.player_pause));
            player.play();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
            player.setPlayerState(PlayerAdapter.State.PAUSED);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(CURRENT_POS, player.getCurrentPosition());
        editor.putBoolean(IS_PLAYING, player.getPlayerState() == PlayerAdapter.State.PLAYING);
        editor.apply();
        player.stopPlayer();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //e necesar?
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(IS_PLAYING);
        editor.remove(CURRENT_POS);
        editor.apply();
        visualizer.release();
        if(audioManager != null)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, phoneVolume, 0);
    }

    class PlaybackListener implements PlaybackInfoListener {
        @Override
        public void onDurationChanged(int duration) {
            playSeekBar.setMax(duration);
        }

        @Override
        public void onPositionChanged(int position) {
            if(!userIsSeeking) {
                if(Build.VERSION.SDK_INT >= 24)
                    playSeekBar.setProgress(position, true);
                else
                    playSeekBar.setProgress(position);
            }
        }

        @Override
        public void onPlaybackCompleted() {
            //a trebuit să folosesc asta pentru că în lolipop crăpa zicînd că nu am voie să updatez UI din thread secundar.
            playPause.post(new Runnable() {
                @Override
                public void run() {
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
                }
            });
            player.reset();
        }

        @Override
        public void onInitializationError() {
            playPause.setEnabled(false);
            resetPlaying.setEnabled(false);
            findViewById(R.id.player_error_message).setVisibility(View.VISIBLE);
        }

        @Override
        public void onReset() {
            player = new AudioPlayer(new PlaybackListener());
            player.loadMedia(recording.getPath());
            player.setGain(gainControl.getProgress());
        }
    }
}
