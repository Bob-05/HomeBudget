package com.homebudget.database.dao;

import androidx.room.*;
import com.homebudget.database.entities.Category;
import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Insert
    List<Long> insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    Category getCategoryById(int categoryId);

    @Query("SELECT * FROM categories WHERE user_id = :userId ORDER BY name ASC")
    List<Category> getCategoriesByUser(int userId);

    @Query("SELECT * FROM categories WHERE user_id = :userId AND is_default = 1")
    List<Category> getDefaultCategories(int userId);

    @Query("SELECT * FROM categories WHERE user_id = :userId AND name = :name LIMIT 1")
    Category getCategoryByName(int userId, String name);

    @Query("SELECT COUNT(*) FROM categories WHERE user_id = :userId")
    int getCategoryCount(int userId);

    @Query("SELECT * FROM categories WHERE user_id = :userId AND is_default = 0")
    List<Category> getUserDefinedCategories(int userId);

    @Query("UPDATE categories SET name = :newName WHERE id = :categoryId")
    void updateCategoryName(int categoryId, String newName);

    @Query("DELETE FROM categories WHERE id = :categoryId AND is_default = 0")
    void deleteUserCategory(int categoryId);

    @Query("SELECT id FROM categories WHERE user_id = :userId AND name = 'Прочее' LIMIT 1")
    Integer getDefaultOtherCategoryId(int userId);
}