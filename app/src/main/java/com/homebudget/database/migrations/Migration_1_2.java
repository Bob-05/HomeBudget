package com.homebudget.database.migrations;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration_1_2 extends Migration {

    public Migration_1_2() {
        super(1, 2);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Проверяем существование колонок перед добавлением
        try {
            // Добавляем поле last_login в users (только если его нет)
            database.execSQL("ALTER TABLE users ADD COLUMN last_login INTEGER DEFAULT 0");
        } catch (Exception e) {
            // Колонка уже существует, игнорируем
        }

        try {
            // Удаляем колонку synced из transactions, если она существует
            database.execSQL("ALTER TABLE transactions DROP COLUMN synced");
        } catch (Exception e) {
            // Колонки нет - игнорируем
        }

        try {
            // Добавляем поле last_notification_sent в notification_settings
            database.execSQL("ALTER TABLE notification_settings ADD COLUMN last_notification_sent INTEGER DEFAULT 0");
        } catch (Exception e) {
            // Колонка уже существует
        }
    }
}