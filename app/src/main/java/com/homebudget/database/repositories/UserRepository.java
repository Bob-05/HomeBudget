package com.homebudget.database.repositories;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.homebudget.database.AppDatabase;
import com.homebudget.database.dao.UserDao;
import com.homebudget.database.entities.User;
import com.homebudget.database.entities.Category;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class UserRepository {

    private UserDao userDao;
    private CategoryRepository categoryRepository;
    private Context context;
    private EncryptedSharedPreferences encryptedPrefs;

    public UserRepository(Context context) {
        this.context = context;
        AppDatabase db = AppDatabase.getInstance(context);
        userDao = db.userDao();
        categoryRepository = new CategoryRepository(context);
        initEncryptedPreferences();
    }

    /**
     * Инициализирует EncryptedSharedPreferences для безопасного хранения сессии
     */
    private void initEncryptedPreferences() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            encryptedPrefs = (EncryptedSharedPreferences)
                    EncryptedSharedPreferences.create(
                            "secure_session",
                            masterKeyAlias,
                            context,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );
        } catch (GeneralSecurityException | java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // ========== РЕГИСТРАЦИЯ И АУТЕНТИФИКАЦИЯ ==========

    public long registerUser(String login, String email, String password,
                             String securityQuestion, String securityAnswer) {
        // Проверка уникальности
        if (userDao.isLoginExists(login)) {
            return -1;
        }

        if (userDao.isEmailExists(email)) {
            return -2;
        }

        // Валидация пароля
        if (!isPasswordValid(password)) {
            return -3;
        }

        // Хеширование пароля и ответа
        String passwordHash = hashString(password);
        String answerHash = hashString(securityAnswer.toLowerCase());

        User user = new User(login, email, passwordHash, securityQuestion, answerHash);
        long userId = userDao.insert(user);

        if (userId > 0) {
            // Создаем 5 предустановленных категорий
            List<String> defaultCategories = Arrays.asList(
                    "Продукты", "Транспорт", "Развлечения", "Связь", "Прочее"
            );

            for (String categoryName : defaultCategories) {
                Category category = new Category((int) userId, categoryName, true);
                categoryRepository.insertCategory(category);
            }
        }

        return userId;
    }

    public User login(String login, String password) {
        String passwordHash = hashString(password);
        User user = userDao.authenticate(login, passwordHash);
        if (user != null) {
            userDao.updateLastLogin(user.getId(), new Date());
            saveUserId(user.getId()); // Только один вызов
        }
        return user;
    }

    // ========== УПРАВЛЕНИЕ СЕССИЕЙ (только ID пользователя) ==========

    /**
     * Сохраняет ID текущего пользователя в зашифрованном хранилище
     */
    public void saveUserId(int userId) {
        if (encryptedPrefs != null) {
            encryptedPrefs.edit()
                    .putInt("user_id", userId)
                    .apply();
        }
    }

    /**
     * Возвращает ID текущего пользователя из зашифрованного хранилища
     */
    public int getCurrentUserId() {
        if (encryptedPrefs != null) {
            return encryptedPrefs.getInt("user_id", -1);
        }
        return -1;
    }

    /**
     * Очищает данные сессии (только ID пользователя)
     */
    public void clearSession() {
        if (encryptedPrefs != null) {
            encryptedPrefs.edit()
                    .remove("user_id")
                    .apply();
        }
    }

    // ========== ВОССТАНОВЛЕНИЕ ПАРОЛЯ ==========

    public String getSecurityQuestion(String login) {
        return userDao.getSecurityQuestion(login);
    }

    public boolean verifySecurityAnswer(String login, String answer) {
        String answerHash = hashString(answer.toLowerCase());
        User user = userDao.verifySecurityAnswer(login, answerHash);
        return user != null;
    }

    public boolean resetPassword(String login, String newPassword) {
        if (!isPasswordValid(newPassword)) {
            return false;
        }
        String newPasswordHash = hashString(newPassword);
        userDao.updatePassword(login, newPasswordHash);
        return true;
    }

    // ========== НАСТРОЙКИ ПОЛЬЗОВАТЕЛЯ ==========

    public void updateThemePreference(int userId, String theme) {
        userDao.updateThemePreference(userId, theme);
    }

    public User getUserById(int userId) {
        return userDao.getUserById(userId);
    }

    public User getUserByLogin(String login) {
        return userDao.getUserByLogin(login);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private boolean isPasswordValid(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return input;
        }
    }
}