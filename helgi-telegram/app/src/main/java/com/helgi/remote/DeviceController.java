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

    // ─── Volume ────────────────────────────────────────────────────

    public String volumeUp() {
        try {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            return "✅ Volume: " + getVolume() + "%";
        } catch (Exception e) { return "❌ Errore volume: " + e.getMessage(); }
    }

    public String volumeDown() {
        try {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            return "✅ Volume: " + getVolume() + "%";
        } catch (Exception e) { return "❌ Errore volume: " + e.getMessage(); }
    }

    public String volumeMute() {
        try {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
            return "✅ Audio silenziato";
        } catch (Exception e) { return "❌ Errore muto: " + e.getMessage(); }
    }

    // ─── Luminosità ────────────────────────────────────────────────

    public String brightnessUp() {
        return setBrightness(Math.min(100, getBrightness() + 10));
    }

    public String brightnessDown() {
        return setBrightness(Math.max(0, getBrightness() - 10));
    }

    private String setBrightness(int percent) {
        try {
            if (!Settings.System.canWrite(ctx)) {
                return "❌ Permesso mancante!\nVai in: Impostazioni → App → Servizi di sistema → Modifica impostazioni di sistema → Attiva";
            }
            int value = Math.max(1, (int) (255 * percent / 100.0));
            Settings.System.putInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, value);
            return "✅ Luminosità: " + percent + "%";
        } catch (Exception e) { return "❌ Errore luminosità: " + e.getMessage(); }
    }

    // ─── Schermo ───────────────────────────────────────────────────

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
            return "✅ Schermo acceso";
        } catch (Exception e) { return "❌ Errore: " + e.getMessage(); }
    }

    public String screenOff() {
        try {
            if (!dpm.isAdminActive(admin)) {
                return "❌ Attiva prima Device Admin!\nVai in: Impostazioni → Sicurezza → Amministratori dispositivo → Servizi di sistema → Attiva";
            }
            dpm.lockNow();
            return "✅ Schermo bloccato";
        } catch (Exception e) { return "❌ Errore: " + e.getMessage(); }
    }

    // ─── App / Sistema ─────────────────────────────────────────────

    public String closeApps() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(home);
            return "✅ Tornato alla Home";
        } catch (Exception e) { return "❌ Errore: " + e.getMessage(); }
    }

    public String reboot() {
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
            return "✅ Riavvio in corso...";
        } catch (Exception e) {
            try {
                Runtime.getRuntime().exec("reboot");
                return "✅ Riavvio in corso...";
            } catch (Exception e2) {
                return "❌ Riavvio non supportato (richiede root o firmware Helgi)";
            }
        }
    }

    public String shutdown() {
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot -p"});
            return "✅ Spegnimento in corso...";
        } catch (Exception e) {
            try {
                Runtime.getRuntime().exec("reboot -p");
                return "✅ Spegnimento in corso...";
            } catch (Exception e2) {
                return "❌ Spegnimento non supportato (richiede root o firmware Helgi)";
            }
        }
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
        return max > 0 ? (int) (cur * 100.0 / max) : 0;
    }

    private int getBrightness() {
        try {
            int b = Settings.System.getInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 128);
            return (int) (b * 100.0 / 255);
        } catch (Exception e) { return 50; }
    }
}
