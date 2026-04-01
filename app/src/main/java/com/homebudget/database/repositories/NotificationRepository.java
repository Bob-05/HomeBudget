package com.homebudget.database.repositories;

import android.content.Context;
import android.util.Log;
import com.homebudget.database.AppDatabase;
import com.homebudget.database.dao.NotificationSettingsDao;
import com.homebudget.database.entities.NotificationSettings;
import java.util.List;

public class NotificationRepository {

    private NotificationSettingsDao notificationSettingsDao;

    public NotificationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        notificationSettingsDao = db.notificationSettingsDao();
    }

    public NotificationSettings getSettingsByUser(int userId) {
        NotificationSettings settings = notificationSettingsDao.getSettingsByUser(userId);
        if (settings == null) {
            settings = new NotificationSettings(userId);
            long id = notificationSettingsDao.insert(settings);
            settings.setId((int) id);
            Log.d("NotificationRepository", "Created new settings for user " + userId + " with id: " + id);
        } else {
            Log.d("NotificationRepository", "Found existing settings for user " + userId +
                    " with id: " + settings.getId() + ", enabled: " + settings.isEnabled());
        }
        return settings;
    }

    public void updateSettings(NotificationSettings settings) {
        Log.d("NotificationRepository", "=== UPDATING SETTINGS ===");
        Log.d("NotificationRepository", "User ID: " + settings.getUserId());
        Log.d("NotificationRepository", "Settings ID: " + settings.getId());
        Log.d("NotificationRepository", "Enabled before update: " + settings.isEnabled());

        // Проверяем, существует ли запись в БД
        NotificationSettings existing = notificationSettingsDao.getSettingsByUser(settings.getUserId());
        if (existing == null) {
            Log.d("NotificationRepository", "No existing record, inserting new");
            long id = notificationSettingsDao.insert(settings);
            settings.setId((int) id);
            Log.d("NotificationRepository", "Inserted with id: " + id);
        } else {
            Log.d("NotificationRepository", "Existing record found with id: " + existing.getId() +
                    ", current enabled: " + existing.isEnabled());

            // Обновляем все поля
            notificationSettingsDao.update(settings);
            Log.d("NotificationRepository", "Update called");
        }

        // Проверяем результат
        NotificationSettings check = notificationSettingsDao.getSettingsByUser(settings.getUserId());
        if (check != null) {
            Log.d("NotificationRepository", "After update - enabled: " + check.isEnabled() +
                    ", period: " + check.getPeriod() +
                    ", time: " + check.getHour() + ":" + check.getMinute());
        } else {
            Log.e("NotificationRepository", "After update - settings not found!");
        }
    }

    public void setEnabled(int userId, boolean enabled) {
        Log.d("NotificationRepository", "Setting enabled=" + enabled + " for user " + userId);
        notificationSettingsDao.setEnabled(userId, enabled);

        // Проверяем
        NotificationSettings check = notificationSettingsDao.getSettingsByUser(userId);
        if (check != null) {
            Log.d("NotificationRepository", "After setEnabled - enabled: " + check.isEnabled());
        }
    }

    public void updateSchedule(int userId, String period, int hour, int minute) {
        notificationSettingsDao.updateSchedule(userId, period, hour, minute);
    }

    public void setIncludeAiSummary(int userId, boolean includeAi) {
        notificationSettingsDao.setIncludeAiSummary(userId, includeAi);
    }

    public List<NotificationSettings> getAllEnabledSettings() {
        return notificationSettingsDao.getAllEnabledSettings();
    }

    public void updateLastNotificationSent(int userId, long timestamp) {
        notificationSettingsDao.updateLastNotificationSent(userId, timestamp);
    }
}