package com.homebudget.database.dao;

import androidx.room.*;
import com.homebudget.database.entities.User;
import java.util.Date;  // ← добавлен импорт Date

@Dao
public interface UserDao {

    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserById(int userId);

    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    User getUserByLogin(String login);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE login = :login)")
    boolean isLoginExists(String login);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    boolean isEmailExists(String email);

    @Query("SELECT * FROM users WHERE login = :login AND password_hash = :passwordHash LIMIT 1")
    User authenticate(String login, String passwordHash);

    @Query("SELECT security_question FROM users WHERE login = :login LIMIT 1")
    String getSecurityQuestion(String login);

    @Query("SELECT * FROM users WHERE login = :login AND security_answer_hash = :answerHash LIMIT 1")
    User verifySecurityAnswer(String login, String answerHash);

    @Query("UPDATE users SET password_hash = :newPasswordHash WHERE login = :login")
    void updatePassword(String login, String newPasswordHash);

    @Query("UPDATE users SET theme_preference = :theme WHERE id = :userId")
    void updateThemePreference(int userId, String theme);

    // Добавляем метод для last_login
    @Query("UPDATE users SET last_login = :lastLogin WHERE id = :userId")
    void updateLastLogin(int userId, Date lastLogin);
}