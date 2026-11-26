package com.helgi.remote;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;

public class RemoteService extends Service {
    private static final String TAG = "RemoteService";
    private static final String CHANNEL_ID = "sys_service_channel";
    private static final int NOTIF_ID = 1;

    private TelegramBot bot;
    private DeviceController device;
    private Thread pollingThread;
    private volatile boolean running = false;

    @Override
    public void onCreate() {
        super.onCreate();
        bot = new TelegramBot();
        device = new DeviceController(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        if (!running) {
            running = true;
            pollingThread = new Thread(this::pollLoop);
            pollingThread.setDaemon(true);
            pollingThread.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (pollingThread != null) pollingThread.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ─── Loop principale Telegram ───────────────────────────────────

    private void pollLoop() {
        Log.i(TAG, "Bot polling avviato");
        while (running) {
            try {
                JSONArray updates = bot.getUpdates();
                for (int i = 0; i < updates.length(); i++) {
                    processUpdate(updates.getJSONObject(i));
                }
            } catch (Exception e) {
                Log.e(TAG, "pollLoop error: " + e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void processUpdate(JSONObject update) throws Exception {
        // Messaggio testuale (es. /start)
        if (update.has("message")) {
            JSONObject msg = update.getJSONObject("message");
            String chatId = String.valueOf(msg.getJSONObject("chat").getLong("id"));
            if (!Config.ALLOWED_CHAT_ID.equals(chatId)) return; // sicurezza
            String text = msg.optString("text", "");
            if (text.startsWith("/start") || text.startsWith("/menu")) {
                bot.sendDashboard(chatId, getDashboardText());
            } else if (text.startsWith("/stato") || text.startsWith("/status")) {
                bot.sendDashboard(chatId, device.getStatus());
            }
        }

        // Tap su pulsante inline
        if (update.has("callback_query")) {
            JSONObject cb = update.getJSONObject("callback_query");
            String cbId = cb.getString("id");
            String chatId = String.valueOf(cb.getJSONObject("from").getLong("id"));
            if (!Config.ALLOWED_CHAT_ID.equals(chatId)) return;
            int messageId = cb.getJSONObject("message").getInt("message_id");
            String data = cb.getString("data");

            String result = handleCallback(data);
            bot.answerCallback(cbId, result);
            bot.editDashboard(chatId, messageId, getDashboardText() + "\n\n" + result);
        }
    }

    private String handleCallback(String data) {
        switch (data) {
            case "vol_up":     return device.volumeUp();
            case "vol_down":   return device.volumeDown();
            case "vol_mute":   return device.volumeMute();
            case "bri_up":     return device.brightnessUp();
            case "bri_down":   return device.brightnessDown();
            case "screen_on":  return device.screenOn();
            case "screen_off": return device.screenOff();
            case "close_apps": return device.closeApps();
            case "reboot":     return device.reboot();
            case "shutdown":   return device.shutdown();
            case "status":     return device.getStatus();
            default:           return "❓ Comando sconosciuto";
        }
    }

    private String getDashboardText() {
        return "📺 <b>Helgi Remote</b>\n"
                + "Scegli un'azione dalla tastiera qui sotto:";
    }

    // ─── Notifica foreground ────────────────────────────────────────

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Servizi di sistema")
                .setContentText("In esecuzione")
                .setSmallIcon(R.drawable.ic_system)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Servizi di sistema",
                NotificationManager.IMPORTANCE_MIN);
        ch.setDescription("Servizio di sistema Android");
        ch.setShowBadge(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    // IP per stato
    public static String getLocalIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> ifaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                java.net.NetworkInterface iface = ifaces.nextElement();
                java.util.Enumeration<java.net.InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0)
                        return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "N/D";
    }
}
