package com.helgi.remote;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║           CONFIGURAZIONE BOT TELEGRAM - HELGI REMOTE         ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║                                                              ║
 * ║  PASSO 1: Ottieni il token del bot                           ║
 * ║  → Apri Telegram → cerca @BotFather → scrivi /newbot        ║
 * ║  → Segui le istruzioni e copia il token                      ║
 * ║                                                              ║
 * ║  PASSO 2: Trova il tuo Chat ID                               ║
 * ║  → Apri Telegram → cerca @userinfobot → scrivi /start       ║
 * ║  → Copia il numero "Id:" che ti mostra                       ║
 * ║                                                              ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class Config {

    // ▼▼▼ INSERISCI QUI IL TOKEN DEL BOT (es. "123456:ABCdef...") ▼▼▼
    public static final String BOT_TOKEN = "9290443189:AAFnYfehW9TdaYKwHPk0rx352HWKsS5eMYE";
    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    // ▼▼▼ INSERISCI QUI IL TUO CHAT ID (es. "123456789") ▼▼▼
    public static final String ALLOWED_CHAT_ID = "9041042961";
    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    // URL base API Telegram (non modificare)
    public static final String API_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/";
}
