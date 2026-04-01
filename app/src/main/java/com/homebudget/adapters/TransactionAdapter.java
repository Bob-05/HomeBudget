package com.homebudget.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.homebudget.R;
import com.homebudget.database.entities.Category;
import com.homebudget.database.entities.Transaction;
import com.homebudget.database.repositories.CategoryRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactions = new ArrayList<>();
    private Map<Integer, String> categoryNames = new HashMap<>(); // Кэш названий категорий
    private OnTransactionClickListener clickListener;
    private OnTransactionDeleteListener deleteListener;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private Context context;

    public interface OnTransactionClickListener {
        void onEdit(Transaction transaction);
    }

    public interface OnTransactionDeleteListener {
        void onDelete(Transaction transaction);
    }

    public TransactionAdapter(OnTransactionClickListener clickListener,
                              OnTransactionDeleteListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    /**
     * Устанавливает список категорий для кэширования
     */
    public void setCategories(List<Category> categories) {
        categoryNames.clear();
        if (categories != null) {
            for (Category category : categories) {
                categoryNames.put(category.getId(), category.getName());
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        holder.tvAmount.setText(String.format("%.2f ₽", transaction.getAmount()));
        holder.tvNote.setText(transaction.getNote() != null && !transaction.getNote().isEmpty()
                ? transaction.getNote() : "Без описания");
        holder.tvDateTime.setText(sdf.format(transaction.getDateTime()));

        // Устанавливаем название категории из кэша
        String categoryName = categoryNames.get(transaction.getCategoryId());
        holder.tvCategory.setText(categoryName != null ? categoryName : "Загрузка...");

        if ("income".equals(transaction.getType())) {
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.income_green));
            holder.tvAmount.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_up, 0);
        } else {
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.expense_red));
            holder.tvAmount.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onEdit(transaction));
        holder.itemView.setOnLongClickListener(v -> {
            deleteListener.onDelete(transaction);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateList(List<Transaction> newList) {
        this.transactions = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvNote, tvDateTime, tvCategory;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            tvCategory = itemView.findViewById(R.id.tv_category); // ← ДОБАВЛЯЕМ
        }
    }
}