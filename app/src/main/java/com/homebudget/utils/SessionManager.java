package com.homebudget.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String KEY_LAST_ACTIVITY_TIME = "last_activity_time";
    private static final String KEY_LAST_FOREGROUND_TIME = "last_foreground_time";
    private static final String KEY_LAST_BACKGROUND_TIME = "last_background_time";
    private static final long SESSION_TIMEOUT_MS = 60 * 1000; // 1 минута для теста

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private boolean isAppInForeground = true;

    private SessionManager(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d(TAG, "SessionManager initialized");
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    public void updateLastActivity() {
        long currentTime = System.currentTimeMillis();
        prefs.edit().putLong(KEY_LAST_ACTIVITY_TIME, currentTime).apply();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Log.d(TAG, "🕐 UPDATE: " + sdf.format(new Date(currentTime)));
    }

    public void updateForegroundTime() {
        long currentTime = System.currentTimeMillis();
        prefs.edit().putLong(KEY_LAST_FOREGROUND_TIME, currentTime).apply();
        Log.d(TAG, "🟢 App went to foreground at: " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(currentTime)));
    }

    public void updateBackgroundTime() {
        long currentTime = System.currentTimeMillis();
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, currentTime).apply();
        Log.d(TAG, "🔴 App went to background at: " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(currentTime)));
    }

    public void setAppInForeground(boolean inForeground) {
        this.isAppInForeground = inForeground;
        if (inForeground) {
            updateForegroundTime();
        } else {
            updateBackgroundTime();
        }
    }

    public boolean isSessionExpired() {
        long lastActivityTime = prefs.getLong(KEY_LAST_ACTIVITY_TIME, 0);
        long lastForegroundTime = prefs.getLong(KEY_LAST_FOREGROUND_TIME, 0);
        long lastBackgroundTime = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0);
        long currentTime = System.currentTimeMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        // Определяем, какое время использовать для проверки
        long timeToCheck;
        String source;

        if (isAppInForeground) {
            // Приложение активно - используем время последнего взаимодействия
            timeToCheck = lastActivityTime;
            source = "activity (foreground)";
        } else {
            // Приложение в фоне или закрыто - используем время ухода в фон
            // Если приложение было убито, lastBackgroundTime могло не сохраниться,
            // тогда используем lastForegroundTime как fallback
            if (lastBackgroundTime > 0) {
                timeToCheck = lastBackgroundTime;
                source = "background exit";
            } else if (lastForegroundTime > 0) {
                timeToCheck = lastForegroundTime;
                source = "last foreground (fallback)";
            } else {
                timeToCheck = lastActivityTime;
                source = "last activity (fallback)";
            }
        }

        if (timeToCheck == 0) {
            Log.d(TAG, "❌ EXPIRED: No time recorded");
            return true;
        }

        long diff = currentTime - timeToCheck;

        Log.d(TAG, "⏱️ CHECK: Source=" + source);
        Log.d(TAG, "⏱️ CHECK: Last=" + sdf.format(new Date(timeToCheck)));
        Log.d(TAG, "⏱️ CHECK: Now=" + sdf.format(new Date(currentTime)));
        Log.d(TAG, "⏱️ CHECK: Diff=" + diff + "ms (" + (diff / 1000) + " sec)");
        Log.d(TAG, "⏱️ CHECK: Timeout=" + SESSION_TIMEOUT_MS + "ms (" + (SESSION_TIMEOUT_MS / 1000) + " sec)");

        boolean expired = diff > SESSION_TIMEOUT_MS;
        Log.d(TAG, expired ? "❌ SESSION EXPIRED!" : "✅ Session valid");
        return expired;
    }

    /**
     * Проверяет сессию при запуске приложения (процесс мог быть убит)
     */
    public boolean isSessionExpiredOnStart() {
        long lastForegroundTime = prefs.getLong(KEY_LAST_FOREGROUND_TIME, 0);
        long lastBackgroundTime = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0);
        long currentTime = System.currentTimeMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        // Используем последнее известное время (когда приложение было активно)
        long timeToCheck = Math.max(lastForegroundTime, lastBackgroundTime);

        if (timeToCheck == 0) {
            Log.d(TAG, "❌ EXPIRED ON START: No time recorded");
            return true;
        }

        long diff = currentTime - timeToCheck;

        Log.d(TAG, "⏱️ CHECK ON START: Last=" + sdf.format(new Date(timeToCheck)));
        Log.d(TAG, "⏱️ CHECK ON START: Now=" + sdf.format(new Date(currentTime)));
        Log.d(TAG, "⏱️ CHECK ON START: Diff=" + diff + "ms (" + (diff / 1000) + " sec)");
        Log.d(TAG, "⏱️ CHECK ON START: Timeout=" + SESSION_TIMEOUT_MS + "ms (" + (SESSION_TIMEOUT_MS / 1000) + " sec)");

        boolean expired = diff > SESSION_TIMEOUT_MS;
        Log.d(TAG, expired ? "❌ SESSION EXPIRED ON START!" : "✅ Session valid on start");
        return expired;
    }

    public void clearSession() {
        prefs.edit()
                .remove(KEY_LAST_ACTIVITY_TIME)
                .remove(KEY_LAST_FOREGROUND_TIME)
                .remove(KEY_LAST_BACKGROUND_TIME)
                .apply();
        Log.d(TAG, "🗑️ Session cleared");
    }

    public long getLastActivityTime() {
        return prefs.getLong(KEY_LAST_ACTIVITY_TIME, 0);
    }
}