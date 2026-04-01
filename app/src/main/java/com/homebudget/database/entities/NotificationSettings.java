package com.homebudget.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import static androidx.room.ForeignKey.CASCADE;
import java.util.Date;

@Entity(
        tableName = "notification_settings",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = CASCADE
        ),
        indices = {@Index(value = {"user_id"})}
)
public class NotificationSettings {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "enabled")
    private boolean enabled;

    @ColumnInfo(name = "period")
    private String period;

    @ColumnInfo(name = "hour")
    private int hour;

    @ColumnInfo(name = "minute")
    private int minute;

    @ColumnInfo(name = "include_ai_summary")
    private boolean includeAiSummary;

    @ColumnInfo(name = "updated_at")
    private Date updatedAt;

    @ColumnInfo(name = "last_notification_sent")
    private long lastNotificationSent;

    public NotificationSettings() {}

    @Ignore
    public NotificationSettings(int userId) {
        this.userId = userId;
        this.enabled = false;
        this.period = "daily";
        this.hour = 9;
        this.minute = 0;
        this.includeAiSummary = false;
        this.updatedAt = new Date();
        this.lastNotificationSent = 0; // Явная инициализация
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }
    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }
    public boolean isIncludeAiSummary() { return includeAiSummary; }
    public void setIncludeAiSummary(boolean includeAiSummary) { this.includeAiSummary = includeAiSummary; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public long getLastNotificationSent() { return lastNotificationSent; }
    public void setLastNotificationSent(long lastNotificationSent) { this.lastNotificationSent = lastNotificationSent; }
}