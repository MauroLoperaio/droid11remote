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
    public static final String BOT_TOKEN = "INSERISCI_QUI_IL_TOKEN_DEL_BOT";
    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    // ▼▼▼ INSERISCI QUI IL TUO CHAT ID (es. "123456789") ▼▼▼
    public static final String ALLOWED_CHAT_ID = "INSERISCI_QUI_IL_TUO_CHAT_ID";
    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    // URL base API Telegram (non modificare)
    public static final String API_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/";
}
