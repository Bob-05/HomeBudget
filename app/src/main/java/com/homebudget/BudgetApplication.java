package com.homebudget;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.homebudget.utils.ThemeManager;

public class BudgetApplication extends Application {

    private static BudgetApplication instance;
    private ThemeManager themeManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        themeManager = ThemeManager.getInstance(this);
        themeManager.applyThemeOnStart();

        createNotificationChannel();
    }

    public static BudgetApplication getInstance() {
        return instance;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "budget_channel",
                    "Уведомления бюджета",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Уведомления о финансовой статистике");
            channel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}