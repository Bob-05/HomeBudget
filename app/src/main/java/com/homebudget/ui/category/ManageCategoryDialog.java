package com.homebudget.ui.category;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.homebudget.R;
import com.homebudget.database.entities.Category;
import java.util.List;

public class ManageCategoryDialog {

    private Context context;
    private int userId;
    private List<Category> existingCategories;
    private Category existingCategory; // Для редактирования
    private OnCategorySaveListener saveListener;
    private OnCategoryDeleteListener deleteListener;

    private AlertDialog dialog;
    private EditText etCategoryName;
    private Button btnSave, btnCancel, btnDelete;

    public interface OnCategorySaveListener {
        void onSave(Category category);
    }

    public interface OnCategoryDeleteListener {
        void onDelete(Category category);
    }

    // Конструктор для создания новой категории
    public ManageCategoryDialog(Context context, int userId, List<Category> existingCategories,
                                OnCategorySaveListener saveListener) {
        this.context = context;
        this.userId = userId;
        this.existingCategories = existingCategories;
        this.saveListener = saveListener;
        this.deleteListener = null;
        this.existingCategory = null;
        createDialog(false);
    }

    // Конструктор для редактирования/удаления существующей категории
    public ManageCategoryDialog(Context context, int userId, List<Category> existingCategories,
                                Category category, OnCategorySaveListener saveListener,
                                OnCategoryDeleteListener deleteListener) {
        this.context = context;
        this.userId = userId;
        this.existingCategories = existingCategories;
        this.existingCategory = category;
        this.saveListener = saveListener;
        this.deleteListener = deleteListener;
        createDialog(true);
        populateFields();
    }

    private void createDialog(boolean isEditMode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.RoundedCornersDialog);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_manage_category, null);

        etCategoryName = view.findViewById(R.id.et_category_name);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnDelete = view.findViewById(R.id.btn_delete);

        // Меняем текст кнопки сохранения
        if (isEditMode) {
            btnSave.setText("Сохранить");
        } else {
            btnSave.setText("Создать");
        }

        btnSave.setOnClickListener(v -> saveCategory());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Показываем кнопку удаления только в режиме редактирования
        if (isEditMode && existingCategory != null) {
            // Не показываем кнопку удаления для категории "Прочее"
            if ("Прочее".equals(existingCategory.getName())) {
                btnDelete.setVisibility(View.GONE);
            } else {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    // Подтверждение удаления
                    new AlertDialog.Builder(context, R.style.RoundedCornersDialog)
                            .setTitle("Удаление категории")
                            .setMessage("Удалить категорию \"" + existingCategory.getName() + "\"?\n\n" +
                                    "Все транзакции этой категории будут перенесены в категорию \"Прочее\".")
                            .setPositiveButton("Удалить", (d, w) -> {
                                if (deleteListener != null) {
                                    deleteListener.onDelete(existingCategory);
                                }
                                dialog.dismiss();
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                });
            }
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        builder.setView(view);
        dialog = builder.create();
    }

    private void populateFields() {
        if (existingCategory != null) {
            etCategoryName.setText(existingCategory.getName());
        }
    }

    private void saveCategory() {
        String name = etCategoryName.getText().toString().trim();

        if (name.isEmpty()) {
            etCategoryName.setError("Введите название категории");
            return;
        }

        // Проверка на уникальность
        if (existingCategories != null) {
            for (Category cat : existingCategories) {
                // При редактировании пропускаем саму себя
                if (existingCategory != null && cat.getId() == existingCategory.getId()) {
                    continue;
                }
                if (cat.getName().equalsIgnoreCase(name)) {
                    etCategoryName.setError("Категория с таким названием уже существует");
                    return;
                }
            }
        }

        Category category;
        if (existingCategory != null) {
            // Редактируем существующую
            category = existingCategory;
            category.setName(name);
        } else {
            // Создаём новую
            category = new Category(userId, name, false);
        }

        saveListener.onSave(category);
        dialog.dismiss();
    }

    public void show() {
        dialog.show();
    }
}