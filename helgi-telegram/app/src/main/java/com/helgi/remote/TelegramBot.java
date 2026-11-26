package com.helgi.remote;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TelegramBot {
    private static final String TAG = "TelegramBot";
    private long offset = 0;

    // Chiamata POST all'API Telegram
    private JSONObject apiCall(String method, JSONObject params) {
        try {
            URL url = new URL(Config.API_URL + method);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(35000);

            byte[] data = params.toString().getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(data);

            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code == 200 ? conn.getInputStream() : conn.getErrorStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();

            return new JSONObject(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "apiCall error: " + e.getMessage());
            return null;
        }
    }

    // Long polling: riceve aggiornamenti da Telegram
    public JSONArray getUpdates() {
        try {
            JSONObject params = new JSONObject();
            params.put("offset", offset);
            params.put("timeout", 30);
            params.put("limit", 10);
            JSONObject result = apiCall("getUpdates", params);
            if (result != null && result.getBoolean("ok")) {
                JSONArray updates = result.getJSONArray("result");
                if (updates.length() > 0) {
                    offset = updates.getJSONObject(updates.length() - 1).getLong("update_id") + 1;
                }
                return updates;
            }
        } catch (Exception e) {
            Log.e(TAG, "getUpdates error: " + e.getMessage());
        }
        return new JSONArray();
    }

    // Invia messaggio con la dashboard
    public void sendDashboard(String chatId, String text) {
        try {
            JSONObject params = new JSONObject();
            params.put("chat_id", chatId);
            params.put("text", text);
            params.put("parse_mode", "HTML");
            params.put("reply_markup", buildKeyboard());
            apiCall("sendMessage", params);
        } catch (Exception e) {
            Log.e(TAG, "sendDashboard error: " + e.getMessage());
        }
    }

    // Modifica messaggio esistente (risposta ai pulsanti)
    public void editDashboard(String chatId, int messageId, String text) {
        try {
            JSONObject params = new JSONObject();
            params.put("chat_id", chatId);
            params.put("message_id", messageId);
            params.put("text", text);
            params.put("parse_mode", "HTML");
            params.put("reply_markup", buildKeyboard());
            apiCall("editMessageText", params);
        } catch (Exception e) {
            Log.e(TAG, "editDashboard error: " + e.getMessage());
        }
    }

    // Risponde al tap su un pulsante (rimuove il caricamento)
    public void answerCallback(String callbackId, String text) {
        try {
            JSONObject params = new JSONObject();
            params.put("callback_query_id", callbackId);
            params.put("text", text);
            params.put("show_alert", false);
            apiCall("answerCallbackQuery", params);
        } catch (Exception e) {
            Log.e(TAG, "answerCallback error: " + e.getMessage());
        }
    }

    // Costruisce la tastiera inline con tutti i controlli
    private JSONObject buildKeyboard() throws Exception {
        JSONArray keyboard = new JSONArray();
        keyboard.put(row(
                btn("🔊 Vol +", "vol_up"),
                btn("🔇 Muto", "vol_mute"),
                btn("🔊 Vol -", "vol_down")));
        keyboard.put(row(
                btn("💡 Lum +", "bri_up"),
                btn("💡 Lum -", "bri_down")));
        keyboard.put(row(
                btn("📺 Accendi schermo", "screen_on"),
                btn("🔒 Blocca schermo", "screen_off")));
        keyboard.put(row(
                btn("🏠 Vai alla Home", "close_apps")));
        keyboard.put(row(
                btn("📊 Stato dispositivo", "status")));
        keyboard.put(row(
                btn("🔄 Riavvia", "reboot"),
                btn("⏹️ Spegni", "shutdown")));
        JSONObject markup = new JSONObject();
        markup.put("inline_keyboard", keyboard);
        return markup;
    }

    private JSONArray row(JSONObject... buttons) throws Exception {
        JSONArray row = new JSONArray();
        for (JSONObject btn : buttons) row.put(btn);
        return row;
    }

    private JSONObject btn(String text, String data) throws Exception {
        JSONObject b = new JSONObject();
        b.put("text", text);
        b.put("callback_data", data);
        return b;
    }
}
