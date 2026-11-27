package com.helgi.remote;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;

public class DeviceController {
    private final Context ctx;
    private final AudioManager audio;
    private final DevicePolicyManager dpm;
    private final ComponentName admin;

    public DeviceController(Context context) {
        this.ctx = context;
        this.audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.admin = new ComponentName(context, AdminReceiver.class);
    }

    // Esegue un comando con privilegi root (che la LIM supporta)
    private boolean runRoot(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            return p.waitFor() == 0;
        } catch (Exception e) {
            try {
                Process p = Runtime.getRuntime().exec(cmd);
                return p.waitFor() == 0;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    // ─── Volume (scatti del 10% invece di 1%) ──────────────────────

    public String volumeUp() {
        return changeVolumeByPercent(+10);
    }

    public String volumeDown() {
        return changeVolumeByPercent(-10);
    }

    private String changeVolumeByPercent(int deltaPercent) {
        try {
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            int step = Math.max(1, (int) Math.round(max * (Math.abs(deltaPercent) / 100.0)));
            int next = deltaPercent > 0
                    ? Math.min(max, cur + step)
                    : Math.max(0, cur - step);

            audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, AudioManager.FLAG_SHOW_UI);
            // Sincronizza anche STREAM_SYSTEM usato da alcune app Helgi
            int maxSys = audio.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
            if (maxSys > 0) {
                int nextSys = (int) Math.round((next / (double) max) * maxSys);
                audio.setStreamVolume(AudioManager.STREAM_SYSTEM, nextSys, 0);
            }
            return "✅ Volume: " + getVolume() + "%";
        } catch (Exception e) {
            return "❌ Errore volume: " + e.getMessage();
        }
    }

    public String volumeMute() {
        try {
            if (audio.isStreamMute(AudioManager.STREAM_MUSIC)) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI);
                return "🔊 Audio riattivato (" + getVolume() + "%)";
            } else {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
                return "🔇 Audio silenziato";
            }
        } catch (Exception e) {
            return "❌ Errore muto: " + e.getMessage();
        }
    }

    // ─── Luminosità (API + Root Settings + Backlight Hardware) ─────

    public String brightnessUp() {
        return setBrightness(Math.min(100, getBrightness() + 15));
    }

    public String brightnessDown() {
        return setBrightness(Math.max(10, getBrightness() - 15));
    }

    private String setBrightness(int percent) {
        int value = Math.max(10, (int) (255 * percent / 100.0));
        boolean ok = false;

        // 1. Metodo standard Android
        try {
            if (Settings.System.canWrite(ctx)) {
                Settings.System.putInt(ctx.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(ctx.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, value);
                ok = true;
            }
        } catch (Exception ignored) {}

        // 2. Metodo Root Settings (funziona anche senza permesso manuale)
        if (runRoot("settings put system screen_brightness_mode 0 && settings put system screen_brightness " + value)) {
            ok = true;
        }

        // 3. Metodo Hardware Backlight diretto (per pannelli LIM Helgi)
        runRoot("for f in /sys/class/backlight/*/brightness; do [ -w \"$f\" ] && echo " + value + " > \"$f\"; done");

        return ok ? "✅ Luminosità: " + percent + "%" : "❌ Impossibile regolare la luminosità";
    }

    // ─── Schermo (Standby / Risveglio pannello LIM via Root) ───────

    @SuppressWarnings("deprecation")
    public String screenOn() {
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.ON_AFTER_RELEASE,
                    "helgi:screen_on");
            wl.acquire(3000);
        } catch (Exception ignored) {}

        // KEYCODE_WAKEUP (224) risveglia il pannello Helgi dallo standby
        if (runRoot("input keyevent 224")) {
            return "✅ Schermo acceso";
        }
        return "❌ Errore accensione schermo";
    }

    public String screenOff() {
        // KEYCODE_SLEEP (223) mette in standby lo schermo della LIM anche se il lockscreen è disabilitato
        if (runRoot("input keyevent 223")) {
            return "✅ Schermo spento (standby)";
        }
        try {
            if (dpm.isAdminActive(admin)) {
                dpm.lockNow();
                return "✅ Schermo bloccato";
            }
        } catch (Exception ignored) {}
        return "❌ Errore spegnimento schermo";
    }

    // ─── Vai alla Home (bypassa il blocco background Android 11) ───

    public String closeApps() {
        // KEYCODE_HOME (3) + avvio esplicito del launcher via root
        if (runRoot("input keyevent 3") || runRoot("am start -a android.intent.action.MAIN -c android.intent.category.HOME")) {
            return "✅ Tornato alla Home";
        }
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(home);
            return "✅ Tornato alla Home";
        } catch (Exception e) {
            return "❌ Errore Home: " + e.getMessage();
        }
    }

    // ─── Riavvio e Spegnimento ─────────────────────────────────────

    public String reboot() {
        if (runRoot("reboot")) return "✅ Riavvio in corso...";
        return "❌ Riavvio non supportato";
    }

    public String shutdown() {
        if (runRoot("reboot -p")) return "✅ Spegnimento in corso...";
        return "❌ Spegnimento non supportato";
    }

    // ─── Stato ─────────────────────────────────────────────────────

    public String getStatus() {
        int vol = getVolume();
        int bri = getBrightness();
        String androidVer = Build.VERSION.RELEASE;
        String model = Build.MODEL;
        long uptime = SystemClock.elapsedRealtime() / 1000;
        long h = uptime / 3600, m = (uptime % 3600) / 60, s = uptime % 60;
        boolean adminOk = dpm.isAdminActive(admin);
        boolean writeOk = Settings.System.canWrite(ctx);

        return "📊 <b>Stato Lavagna Helgi</b>\n\n"
                + "📱 Modello: " + model + "\n"
                + "🤖 Android: " + androidVer + "\n"
                + "🔊 Volume: " + vol + "%\n"
                + "💡 Luminosità: " + bri + "%\n"
                + "⏱ Uptime: " + h + "h " + m + "m " + s + "s\n"
                + "🔐 Device Admin: " + (adminOk ? "✅" : "❌") + "\n"
                + "✏️ Modifica sistema: " + (writeOk ? "✅" : "❌");
    }

    // ─── Helper ────────────────────────────────────────────────────

    private int getVolume() {
        int cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return max > 0 ? (int) Math.round(cur * 100.0 / max) : 0;
    }

    private int getBrightness() {
        try {
            int b = Settings.System.getInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 128);
            return (int) Math.round(b * 100.0 / 255);
        } catch (Exception e) {
            return 50;
        }
    }
}
