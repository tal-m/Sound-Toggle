package com.grmasa.soundtoggle;

import android.media.AudioManager;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SoundModes {
    private SoundModes() {}

    public static final class Mode {
        public final String name;
        public final int icon;
        public final Predicate<AudioManager> condition;
        public final Consumer<SoundToggleService> activate;
        public final boolean restoreVolume; // Restore media volume on exit

        public Mode(String name, int icon, Predicate<AudioManager> condition, Consumer<SoundToggleService> activate, boolean restoreVolume) {
            this.name = name;
            this.icon = icon;
            this.condition = condition;
            this.activate = activate;
            this.restoreVolume = restoreVolume; // Default value
        }
    }

    public static final Mode[] MODES = new Mode[]{
            new Mode(
                    "Normal",
                    R.drawable.ic_audio_vol,
                    (am) -> am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL,
                    (service) -> {
                        service.getAudioManager().setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        service.vibrate();
                    },
                    false
            ),
            new Mode(
                    "Vibrate",
                    R.drawable.ic_audio_ring_notif_vibrate,
                    (am) -> (am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) &&
                            (am.getStreamVolume(AudioManager.STREAM_MUSIC) > 0),
                    (service) -> {
                        AudioManager am = service.getAudioManager();
                        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                        int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, 0);
                        service.vibrate();
                    },
                    false
            ),
            new Mode(
                    "VibrateHush", // Vibrate without media
                    R.drawable.vibratehush, // Changed icon to reflect silent media
                    (am) -> am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE && am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0,
                    (service) -> {
                        AudioManager am = service.getAudioManager();
                        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                        service.vibrate();
                    },
                    true
            ),
            new Mode(
                    "Silent",
                    R.drawable.ic_audio_vol_mute,
                    (am) -> am.getRingerMode() == AudioManager.RINGER_MODE_SILENT,
                    (service) -> service.getAudioManager().setRingerMode(AudioManager.RINGER_MODE_SILENT),
                    false
            )
    };
}
