package com.homebudget.database.dao;

import androidx.room.*;
import com.homebudget.database.entities.NotificationSettings;
import java.util.List;

@Dao
public interface NotificationSettingsDao {

    @Insert
    long insert(NotificationSettings settings);

    @Update
    void update(NotificationSettings settings);

    // Добавьте этот метод для отладки
    @Query("SELECT * FROM notification_settings WHERE user_id = :userId")
    NotificationSettings getSettingsByUser(int userId);

    @Query("SELECT * FROM notification_settings WHERE id = :settingsId")
    NotificationSettings getSettingsById(int settingsId);

    @Query("SELECT * FROM notification_settings WHERE enabled = 1")
    List<NotificationSettings> getAllEnabledSettings();

    @Query("UPDATE notification_settings SET enabled = :enabled, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId")
    void setEnabled(int userId, boolean enabled);

    @Query("UPDATE notification_settings SET period = :period, hour = :hour, minute = :minute, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId")
    void updateSchedule(int userId, String period, int hour, int minute);

    @Query("UPDATE notification_settings SET include_ai_summary = :includeAi, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId")
    void setIncludeAiSummary(int userId, boolean includeAi);

    @Query("UPDATE notification_settings SET last_notification_sent = :timestamp WHERE user_id = :userId")
    void updateLastNotificationSent(int userId, long timestamp);

}