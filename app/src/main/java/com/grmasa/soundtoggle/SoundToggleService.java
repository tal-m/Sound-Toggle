package com.grmasa.soundtoggle;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.HashSet;
import java.util.Set;

public class SoundToggleService extends TileService {

    private final BroadcastReceiver broadcastReceiver = new Receiver();

    static class ModeState {
        public int mediaVolume;

        public ModeState() {
            this.mediaVolume = -1;
        }
    }

    int lastMode = -1;   // Remember the last mode
    ModeState lastModeState = new ModeState(); // Remember last mode state to detect if it was manually changed

    ModeState[] modeStates = new ModeState[SoundModes.MODES.length];

    public SoundToggleService() {
        for (int i = 0; i < SoundModes.MODES.length; ++i) {
            modeStates[i] = new ModeState();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        loadState();
    }

    class Receiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            SoundToggleService.this.updateTile();
        }
    }

    public AudioManager getAudioManager() {
        return (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    public int getCurrentMode() {
        AudioManager am = getAudioManager();
        for (int i = 0; i < SoundModes.MODES.length; ++i) {
            if (SoundModes.MODES[i].condition.test(am)) {
                return i;
            }
        }
        return -1;
    }

    private boolean wasModeManuallyChanged(int from) {
        AudioManager am = getAudioManager();

        int curVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        return  lastMode != from || lastModeState.mediaVolume != curVolume;
    }

    public void switchMode(int from, int to) {
        AudioManager am = getAudioManager();
        if (from >= 0) {
            SoundModes.Mode fromMode = SoundModes.MODES[from];
            // Restore the volume only if the mode and/or volume weren't manually changed in the meantime.
            if (fromMode.restoreVolume && !wasModeManuallyChanged(from)) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, modeStates[from].mediaVolume, 0);
            }
        }
        if (to >= 0) {
            SoundModes.Mode toMode = SoundModes.MODES[to];
            if (toMode.restoreVolume) {
                modeStates[to].mediaVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
            toMode.activate.accept(this);
        }
        lastMode = to;
        lastModeState.mediaVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        saveState();

    }

    public void updateTile() {
        Tile qsTile = getQsTile();

        int curMode = getCurrentMode();
        if (curMode >= 0) {
            SoundModes.Mode mode = SoundModes.MODES[curMode];
            qsTile.setLabel(mode.name);
            qsTile.setContentDescription(mode.name);
            qsTile.setIcon(Icon.createWithResource(this, mode.icon));
            qsTile.setState(Tile.STATE_ACTIVE);
            qsTile.updateTile();
        } else {
            // Fallback for unknown state
            qsTile.setLabel("Unknown");
            qsTile.setState(Tile.STATE_UNAVAILABLE);
            qsTile.updateTile();

        }
    }

    public void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            // deprecated in API 26
            vibrator.vibrate(500);
        }
    }

    public void onClick() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        int currentModeIndex = getCurrentMode();

        Set<String> excludedModes = loadExcludedModeNames();

        for (int i = 1; i <= SoundModes.MODES.length; i++) {
            int nextIndex = (currentModeIndex + i) % SoundModes.MODES.length;
            SoundModes.Mode nextMode = SoundModes.MODES[nextIndex];
            if (!excludedModes.contains(nextMode.name)) {
                switchMode(currentModeIndex, nextIndex);
                break;
            }
        }
    }

    private Set<String> loadExcludedModeNames() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        return prefs.getStringSet("excluded_modes", new HashSet<>());
    }

    private void loadState() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        for (int i = 0; i < SoundModes.MODES.length; i++) {
            modeStates[i].mediaVolume = prefs.getInt("mediaVolume_" + i, -1);
        }

        lastMode = prefs.getInt("lastMode", -1);
        lastModeState.mediaVolume = prefs.getInt("lastMode_mediaVolume", -1);
    }

    private void saveState() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("lastMode", lastMode);
        editor.putInt("lastMode_mediaVolume", lastModeState.mediaVolume);

        if (lastMode >= 0) {
            editor.putInt("mediaVolume_" + lastMode, modeStates[lastMode].mediaVolume);
        }

        editor.putString("tutorial", "done");
        editor.apply();
    }

    public void onStartListening() {
        updateTile();
        registerReceiver(this.broadcastReceiver, new IntentFilter("android.media.RINGER_MODE_CHANGED"));
    }

    public void onStopListening() {
        unregisterReceiver(this.broadcastReceiver);
    }
}
