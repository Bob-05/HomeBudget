package com.homebudget.database.repositories;

import android.content.Context;
import com.homebudget.database.AppDatabase;
import com.homebudget.database.dao.CategoryDao;
import com.homebudget.database.entities.Category;
import java.util.List;

public class CategoryRepository {

    private CategoryDao categoryDao;
    private TransactionRepository transactionRepository;

    public CategoryRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        categoryDao = db.categoryDao();
        transactionRepository = new TransactionRepository(context);
    }

    public long insertCategory(Category category) {
        // Проверка на уникальность имени
        Category existing = categoryDao.getCategoryByName(category.getUserId(), category.getName());
        if (existing != null) {
            return -1;
        }
        return categoryDao.insert(category);
    }

    public void updateCategory(Category category) {
        categoryDao.update(category);
    }

    public boolean deleteCategory(int categoryId, int userId) {
        Category category = categoryDao.getCategoryById(categoryId);

        if (category == null) {
            return false;
        }

        // Нельзя удалить категорию "Прочее"
        if ("Прочее".equals(category.getName())) {
            return false;
        }

        // Проверяем, что это не последняя категория
        int categoryCount = categoryDao.getCategoryCount(userId);
        if (categoryCount <= 1) {
            return false; // Нельзя удалить последнюю категорию
        }

        // Получаем ID категории "Прочее"
        Integer otherCategoryId = categoryDao.getDefaultOtherCategoryId(userId);

        // Если категории "Прочее" нет, создаём её
        if (otherCategoryId == null) {
            Category otherCategory = new Category(userId, "Прочее", true);
            long id = categoryDao.insert(otherCategory);
            otherCategoryId = (int) id;
        }

        // Переносим все транзакции в категорию "Прочее"
        if (otherCategoryId != null) {
            transactionRepository.updateTransactionsCategory(userId, categoryId, otherCategoryId);
        }

        // Удаляем категорию
        categoryDao.deleteUserCategory(categoryId);

        return true;
    }

    public void renameCategory(int categoryId, String newName, int userId) {
        // Проверяем уникальность нового имени
        Category existing = categoryDao.getCategoryByName(userId, newName);
        if (existing == null || existing.getId() == categoryId) {
            categoryDao.updateCategoryName(categoryId, newName);
        }
    }

    public List<Category> getCategoriesByUser(int userId) {
        return categoryDao.getCategoriesByUser(userId);
    }

    public List<Category> getUserDefinedCategories(int userId) {
        return categoryDao.getUserDefinedCategories(userId);
    }

    public Category getCategoryById(int categoryId) {
        return categoryDao.getCategoryById(categoryId);
    }
}