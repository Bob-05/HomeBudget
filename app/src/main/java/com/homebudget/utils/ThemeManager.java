package com.homebudget.utils;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import com.homebudget.database.entities.User;
import com.homebudget.database.repositories.UserRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThemeManager {

    private static final String KEY_USER_LOGGED_IN = "user_logged_in";
    private static final String KEY_THEME = "theme";

    private static ThemeManager instance;
    private final Application application;
    private final UserRepository userRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String currentTheme = "system";

    private ThemeManager(Application application) {
        this.application = application;
        this.userRepository = new UserRepository(application);
    }

    public static synchronized ThemeManager getInstance(Application application) {
        if (instance == null) {
            instance = new ThemeManager(application);
        }
        return instance;
    }

    /**
     * Сохраняет состояние входа в аккаунт
     */
    public void saveLoginState(boolean isLoggedIn) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
        prefs.edit().putBoolean(KEY_USER_LOGGED_IN, isLoggedIn).apply();
    }

    /**
     * Применяет тему при запуске приложения (синхронно, без БД)
     */
    public void applyThemeOnStart() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
        boolean isLoggedIn = prefs.getBoolean(KEY_USER_LOGGED_IN, false);

        if (!isLoggedIn) {
            setNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            currentTheme = "system";
            return;
        }

        String cachedTheme = prefs.getString(KEY_THEME, "system");
        applyTheme(cachedTheme);
        currentTheme = cachedTheme;
        loadThemeFromDatabaseAsync();
    }

    private void loadThemeFromDatabaseAsync() {
        executor.execute(() -> {
            int userId = userRepository.getCurrentUserId();
            if (userId != -1) {
                User user = userRepository.getUserById(userId);
                if (user != null) {
                    String dbTheme = user.getThemePreference();
                    String cachedTheme = getCachedTheme();

                    if (!dbTheme.equals(cachedTheme)) {
                        mainHandler.post(() -> {
                            applyTheme(dbTheme);
                            saveCachedTheme(dbTheme);
                        });
                    }
                }
            }
        });
    }

    public void applyThemeAfterLogin(int userId) {
        executor.execute(() -> {
            User user = userRepository.getUserById(userId);
            if (user != null) {
                String theme = user.getThemePreference();
                mainHandler.post(() -> {
                    applyTheme(theme);
                    saveCachedTheme(theme);
                    saveLoginState(true);
                });
            } else {
                mainHandler.post(() -> {
                    applyTheme("system");
                    saveCachedTheme("system");
                    saveLoginState(true);
                });
            }
        });
    }

    public void applyThemeAfterLogout() {
        mainHandler.post(() -> {
            applyTheme("system");
            saveCachedTheme("system");
            saveLoginState(false);
        });
    }

    public void saveAndApplyTheme(int userId, String theme, Runnable onComplete) {
        executor.execute(() -> {
            userRepository.updateThemePreference(userId, theme);

            mainHandler.post(() -> {
                applyTheme(theme);
                saveCachedTheme(theme);
                saveLoginState(true);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }

    public void applyTheme(String theme) {
        int nightMode = getNightModeFromTheme(theme);
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        }
        currentTheme = theme;
    }

    private void setNightMode(int mode) {
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    private int getNightModeFromTheme(String theme) {
        switch (theme) {
            case "light":
                return AppCompatDelegate.MODE_NIGHT_NO;
            case "dark":
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    private void saveCachedTheme(String theme) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
        prefs.edit().putString(KEY_THEME, theme).apply();
    }

    private String getCachedTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
        return prefs.getString(KEY_THEME, "system");
    }

    public String getCurrentTheme() {
        return currentTheme;
    }
}