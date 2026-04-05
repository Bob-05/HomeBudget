package com.homebudget.ui.splash;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.homebudget.BudgetApplication;
import com.homebudget.R;
import com.homebudget.database.entities.User;
import com.homebudget.database.repositories.UserRepository;
import com.homebudget.ui.auth.LoginActivity;
import com.homebudget.ui.base.BaseActivity;
import com.homebudget.ui.main.MainActivity;
import com.homebudget.utils.NotificationScheduler;
import com.homebudget.utils.SessionManager;
import com.homebudget.utils.ThemeManager;

public class SplashActivity extends BaseActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;
    private static final int REQUEST_EXACT_ALARM_PERMISSION = 200;

    private UserRepository userRepository;
    private SessionManager sessionManager;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mainHandler = new Handler(Looper.getMainLooper());
        userRepository = new UserRepository(this);
        sessionManager = SessionManager.getInstance(this);

        // Запрос разрешений
        requestNotificationPermission();
        requestExactAlarmPermission();

        // Разрешение на управление внешним хранилищем (для PDF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        // Анимация (как в старой версии)
        initializeAnimation();

        // Загрузка данных пользователя и планирование уведомлений
        loadUserDataAndScheduleNotifications();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_EXACT_ALARM_PERMISSION);
            }
        }
    }

    // ========== АНИМАЦИЯ (из старой версии) ==========
    private void initializeAnimation() {
        ImageView logo = findViewById(R.id.imageView);
        TextView appName = findViewById(R.id.textViewAppName);

        if (logo != null) {
            logo.setAlpha(0f);
            logo.setScaleX(0.7f);
            logo.setScaleY(0.7f);
        }

        if (appName != null) {
            appName.setAlpha(0f);
            appName.setVisibility(View.VISIBLE);
        }

        startSplashAnimation(logo, appName);
    }

    private void startSplashAnimation(View logo, View appName) {
        if (logo == null) return;

        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .withEndAction(() -> {
                    if (logo != null) {
                        logo.animate()
                                .translationX(-150f)
                                .setDuration(500)
                                .start();
                    }
                    if (appName != null) {
                        appName.animate()
                                .alpha(1f)
                                .translationX(10f)
                                .setDuration(500)
                                .start();
                    }
                })
                .start();
    }
    // ========== КОНЕЦ АНИМАЦИИ ==========

    private void proceedToNext(Class<?> destination) {
        mainHandler.postDelayed(() -> {
            try {
                Intent intent = new Intent(SplashActivity.this, destination);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 2000);
    }

    private void loadUserDataAndScheduleNotifications() {
        new Thread(() -> {
            try {
                int userId = userRepository.getCurrentUserId();

                if (userId != -1) {
                    // Используем специальный метод проверки для запуска
                    boolean isSessionValid = !sessionManager.isSessionExpiredOnStart();

                    if (isSessionValid) {
                        sessionManager.updateLastActivity();
                        sessionManager.setAppInForeground(true);

                        User user = userRepository.getUserById(userId);
                        if (user != null) {
                            String theme = user.getThemePreference();
                            if (!theme.equals(themeManager.getCurrentTheme())) {
                                runOnUiThread(() -> themeManager.applyTheme(theme));
                            }

                            runOnUiThread(() -> {
                                NotificationScheduler.scheduleExactForUser(SplashActivity.this, userId);
                                proceedToNext(MainActivity.class);
                            });
                            return;
                        }
                    }
                }

                // Сессия невалидна
                if (userId != -1) {
                    userRepository.clearSession();
                    sessionManager.clearSession();
                }

                runOnUiThread(() -> {
                    themeManager.applyTheme("system");
                    themeManager.saveLoginState(false);
                    proceedToNext(LoginActivity.class);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    themeManager.applyTheme("system");
                    themeManager.saveLoginState(false);
                    proceedToNext(LoginActivity.class);
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("SplashActivity", "Notification permission granted");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EXACT_ALARM_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                    new Thread(() -> {
                        int userId = userRepository.getCurrentUserId();
                        if (userId != -1) {
                            NotificationScheduler.scheduleExactForUser(SplashActivity.this, userId);
                        }
                    }).start();
                }
            }
        }
    }
}