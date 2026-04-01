package com.homebudget.ui.base;

import android.content.Intent;
import android.os.Bundle;
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

    private ImageButton btnBack;
    protected UserRepository userRepository;
    protected ThemeManager themeManager;
    protected SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = BudgetApplication.getInstance().getThemeManager();
        super.onCreate(savedInstanceState);

        userRepository = new UserRepository(this);
        sessionManager = SessionManager.getInstance(this);

        // Обновляем время последней активности при создании
        updateLastActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем время при возобновлении
        updateLastActivity();
        // Проверяем сессию
        checkSessionAndLogout();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Обновляем время при уходе в фон
        updateLastActivity();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        // Обновляем время при любом взаимодействии пользователя
        updateLastActivity();
    }

    protected void updateLastActivity() {
        if (sessionManager != null) {
            sessionManager.updateLastActivity();
        }
    }

    protected void checkSessionAndLogout() {
        if (sessionManager != null && sessionManager.isSessionExpired()) {
            performLogout();
        }
    }

    protected void performLogout() {
        // Очищаем ID пользователя
        if (userRepository != null) {
            userRepository.clearSession();
        }

        // Очищаем время активности
        if (sessionManager != null) {
            sessionManager.clearSession();
        }

        // Применяем системную тему и сохраняем состояние выхода
        if (themeManager != null) {
            themeManager.applyThemeAfterLogout();
        }

        // Переход на экран входа
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
}