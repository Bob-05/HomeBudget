package com.homebudget.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.homebudget.BudgetApplication;
import com.homebudget.R;
import com.homebudget.database.repositories.UserRepository;
import com.homebudget.ui.auth.LoginActivity;
import com.homebudget.utils.SessionManager;
import com.homebudget.utils.ThemeManager;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";
    private ImageButton btnBack;
    protected UserRepository userRepository;
    protected ThemeManager themeManager;
    protected SessionManager sessionManager;

    private Handler checkHandler;
    private Runnable checkRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = BudgetApplication.getInstance().getThemeManager();
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: " + getClass().getSimpleName());

        userRepository = new UserRepository(this);
        sessionManager = SessionManager.getInstance(this);

        // УДАЛЕНО: Проверка сессии при создании Activity для SplashActivity убрана отсюда.
        // Это предотвратит мгновенное переключение на LoginActivity и сохранит анимацию.

        updateLastActivity();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sessionManager.setAppInForeground(true);
        updateLastActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: " + getClass().getSimpleName());
        updateLastActivity();

        // ИЗМЕНЕНО: Проверка и запуск таймера происходят только там, где это разрешено
        if (shouldCheckSession()) {
            checkSessionAndLogout();
            startPeriodicCheck();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: " + getClass().getSimpleName());

        // ИЗМЕНЕНО: Остановка таймера вызывается только там, где он реально запускался
        if (shouldCheckSession()) {
            stopPeriodicCheck();
        }
        updateLastActivity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sessionManager.setAppInForeground(false);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        Log.d(TAG, "👆 User interaction in " + getClass().getSimpleName());
        updateLastActivity();
    }

    protected void updateLastActivity() {
        if (sessionManager != null) {
            sessionManager.updateLastActivity();
        }
    }

    /**
     * Метод определяет, нужно ли проверять сессию на текущем экране.
     * Проверяем по имени класса, чтобы избежать ошибок компиляции.
     */
    protected boolean shouldCheckSession() {
        String currentActivityName = this.getClass().getSimpleName();

        // Возвращаем false, если это SplashActivity или LoginActivity
        return !currentActivityName.equals("SplashActivity") &&
                !currentActivityName.equals("LoginActivity");
    }

    /**
     * Проверка сессии при запуске (теперь этот метод может быть вызван из SplashActivity по необходимости)
     */
    protected void checkSessionOnStart() {
        Log.d(TAG, "🔍 Checking session on start in " + getClass().getSimpleName());
        if (sessionManager != null && sessionManager.isSessionExpiredOnStart()) {
            Log.d(TAG, "🚪 SESSION EXPIRED ON START! Logging out...");
            performLogout();
        }
    }

    protected void checkSessionAndLogout() {
        Log.d(TAG, "🔍 Checking session in " + getClass().getSimpleName());
        if (sessionManager != null && sessionManager.isSessionExpired()) {
            Log.d(TAG, "🚪 SESSION EXPIRED! Logging out...");
            performLogout();
        }
    }

    protected void performLogout() {
        Log.d(TAG, "🚪 PERFORMING LOGOUT");

        if (userRepository != null) {
            userRepository.clearSession();
        }
        if (sessionManager != null) {
            sessionManager.clearSession();
        }
        if (themeManager != null) {
            themeManager.applyThemeAfterLogout();
        }

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected int getCurrentUserId() {
        if (userRepository != null) {
            return userRepository.getCurrentUserId();
        }
        return -1;
    }

    protected void setupBackButton() {
        btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
    }

    protected void hideBackButton() {
        if (btnBack != null) {
            btnBack.setVisibility(View.GONE);
        }
    }

    protected void showBackButton() {
        if (btnBack != null) {
            btnBack.setVisibility(View.VISIBLE);
        }
    }

    private void startPeriodicCheck() {
        stopPeriodicCheck();
        checkHandler = new Handler(Looper.getMainLooper());
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "⏰ Periodic session check in " + getClass().getSimpleName());
                checkSessionAndLogout();
                if (checkHandler != null) {
                    checkHandler.postDelayed(this, 30000);
                }
            }
        };
        checkHandler.postDelayed(checkRunnable, 30000);
    }

    private void stopPeriodicCheck() {
        if (checkHandler != null && checkRunnable != null) {
            checkHandler.removeCallbacks(checkRunnable);
            checkHandler = null;
            checkRunnable = null;
        }
    }
}