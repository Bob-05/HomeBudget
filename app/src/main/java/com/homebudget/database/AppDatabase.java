package com.homebudget.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.homebudget.database.dao.*;
import com.homebudget.database.entities.*;
import com.homebudget.database.migrations.Migration_1_2;

@Database(
        entities = {
                User.class,
                Category.class,
                Transaction.class,
                NotificationSettings.class,
                AiChatHistory.class
        },
        version = 2,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "homebudget_database";

    public abstract UserDao userDao();
    public abstract CategoryDao categoryDao();
    public abstract TransactionDao transactionDao();
    public abstract NotificationSettingsDao notificationSettingsDao();
    public abstract AiChatHistoryDao aiChatHistoryDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    RoomDatabase.Builder<AppDatabase> builder = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .addMigrations(new Migration_1_2());

                    // ВАЖНО: Убираем fallbackToDestructiveMigration() для релиза!
                    // Он удаляет все данные при ошибке миграции
                    // .fallbackToDestructiveMigration()

                    INSTANCE = builder.build();
                }
            }
        }
        return INSTANCE;
    }
}