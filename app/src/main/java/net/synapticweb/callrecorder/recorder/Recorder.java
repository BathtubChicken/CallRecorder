/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.recorder;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.settings.SettingsFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recorder {
    private File audioFile;
    private Thread recordingThread;
    private long startingTime;
    private final String format;
    private final String mode;
    private SharedPreferences settings;
    public static final String WAV_FORMAT = "wav";
    public static final String AAC_HIGH_FORMAT = "aac_hi";
    public static final String AAC_MEDIUM_FORMAT = "aac_med";
    public static final String AAC_BASIC_FORMAT = "aac_bas";
    static final String MONO = "mono";

     Recorder() {
        settings = PreferenceManager.getDefaultSharedPreferences(CrApp.getInstance());
        format = settings.getString(SettingsFragment.FORMAT, "");
        mode = settings.getString(SettingsFragment.MODE, "");
    }


    long getStartingTime() {
        return startingTime;
    }

    String getAudioFilePath() {
        return audioFile.getAbsolutePath();
    }

    void startRecording(String phoneNumber) throws RecordingException {
        if(phoneNumber == null)
            phoneNumber = "PrivateCall";

        if(isRunning())
            stopRecording();
        String extension = format.equals(WAV_FORMAT) ? ".wav" : ".aac";
        File recordingsDir;

        if(settings.getString(SettingsFragment.STORAGE, "").equals("private"))
            recordingsDir = CrApp.getInstance().getFilesDir();
        else {
            String filePath = settings.getString(SettingsFragment.STORAGE_PATH, null);
            recordingsDir = (filePath == null) ? CrApp.getInstance().getExternalFilesDir(null) : new File(filePath);
            if(recordingsDir == null) //recordingsDir poate fi null în cazul în care getExternalFilesDir(null) returnează null, adică nu e montat (disponibil) un astfel de spațiu.
                recordingsDir = CrApp.getInstance().getFilesDir();
            }

        phoneNumber = phoneNumber.replaceAll("[()/.,* ;+]", "_");
        String fileName = "Recording" + phoneNumber + new SimpleDateFormat("-d-MMM-yyyy-HH-mm-ss", Locale.US).
                format(new Date(System.currentTimeMillis())) + extension;
        audioFile = new File(recordingsDir, fileName);
        CrLog.log(CrLog.DEBUG, String.format("Recording session started. Format: %s. Mode: %s. Save path: %s",
                format, mode, audioFile.getAbsolutePath()));

        if(format.equals(WAV_FORMAT))
            recordingThread = new Thread(new RecordingThreadWav(mode));
        else
            recordingThread = new Thread(new RecordingThreadAac(audioFile, format, mode));

        recordingThread.start();
        startingTime = System.currentTimeMillis();
    }

    void stopRecording() {
        if(recordingThread != null) {
            CrLog.log(CrLog.DEBUG, "Recording session ended.");
                recordingThread.interrupt();
            recordingThread = null;
            if(format.equals(WAV_FORMAT)) {
                //în cazul în care a apărut o eroare în RecordingThreadWav și fișierul temporar nu există, această
                //condiție va fi detectată în bucla try a CopyPcmToWav.run() și va fi raportată o eroare.
                Thread copyPcmToWav = new Thread(new RecordingThreadWav.CopyPcmToWav(audioFile, mode));
                copyPcmToWav.start();
            }
        }
    }

    boolean isRunning() {
        return recordingThread != null && recordingThread.isAlive();
    }

    public String getFormat() {
        return format;
    }

    public String getMode() {
        return mode;
    }

}
