package com.homebudget.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class SessionManager {

    private static final String KEY_LAST_ACTIVITY_TIME = "last_activity_time";
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 минут

    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Обновляет время последней активности
     */
    public void updateLastActivity() {
        prefs.edit().putLong(KEY_LAST_ACTIVITY_TIME, System.currentTimeMillis()).apply();
    }

    /**
     * Проверяет, не истекла ли сессия
     */
    public boolean isSessionExpired() {
        long lastActivityTime = prefs.getLong(KEY_LAST_ACTIVITY_TIME, 0);

        if (lastActivityTime == 0) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastActivityTime) > SESSION_TIMEOUT_MS;
    }

    /**
     * Очищает время последней активности
     */
    public void clearSession() {
        prefs.edit().remove(KEY_LAST_ACTIVITY_TIME).apply();
    }

    /**
     * Получает время последней активности
     */
    public long getLastActivityTime() {
        return prefs.getLong(KEY_LAST_ACTIVITY_TIME, 0);
    }
}