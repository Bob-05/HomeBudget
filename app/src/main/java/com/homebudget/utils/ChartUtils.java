package com.homebudget.utils;

import android.graphics.Color;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.homebudget.database.repositories.ReportRepository;
import java.util.ArrayList;
import java.util.List;

public class ChartUtils {

    /**
     * Настраивает круговую диаграмму для расходов или доходов по категориям
     * @param pieChart - компонент диаграммы
     * @param expenses - список категорий с суммами
     * @param type - тип ("expense" или "income")
     */
    public static void setupPieChart(PieChart pieChart, List<ReportRepository.CategoryExpense> expenses, String type) {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setCenterText(type.equals("expense") ? "Расходы" : "Доходы");
        pieChart.setCenterTextSize(14f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelTextSize(10f);

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        int colorIndex = 0;
        for (ReportRepository.CategoryExpense expense : expenses) {
            if (expense.total > 0 && expense.type.equals(type)) {
                entries.add(new PieEntry((float) expense.total, expense.categoryName));
                colors.add(ColorTemplate.MATERIAL_COLORS[colorIndex % ColorTemplate.MATERIAL_COLORS.length]);
                colorIndex++;
            }
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "Нет данных"));
            colors.add(Color.GRAY);
        }

        PieDataSet dataSet = new PieDataSet(entries, type.equals("expense") ? "Расходы по категориям" : "Доходы по категориям");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    /**
     * Настраивает круговую диаграмму для сравнения доходов и расходов (сводный отчёт)
     * @param pieChart - компонент диаграммы
     * @param comparison - список для сравнения (доходы и расходы)
     */
    public static void setupComparisonPieChart(PieChart pieChart, List<ReportRepository.CategoryExpense> comparison) {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setCenterText("Доходы/Расходы");
        pieChart.setCenterTextSize(14f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelTextSize(10f);

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (ReportRepository.CategoryExpense item : comparison) {
            entries.add(new PieEntry((float) item.total, item.categoryName));
            if ("income".equals(item.type)) {
                colors.add(Color.parseColor("#4CAF50")); // Зелёный для доходов
            } else {
                colors.add(Color.parseColor("#F44336")); // Красный для расходов
            }
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "Нет данных"));
            colors.add(Color.GRAY);
        }

        PieDataSet dataSet = new PieDataSet(entries, "Доходы и расходы");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    /**
     * Настраивает круговую диаграмму для списка транзакций
     * @param pieChart - компонент диаграммы
     * @param transactions - список транзакций
     * @param totalAmount - общая сумма для расчёта процентов
     * @param type - тип ("expense" или "income")
     */
    public static void setupTransactionsPieChart(PieChart pieChart, List<ReportRepository.CategoryExpense> transactions,
                                                 double totalAmount, String type) {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setCenterText(type.equals("expense") ? "Расходы" : "Доходы");
        pieChart.setCenterTextSize(14f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelTextSize(10f);

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        int colorIndex = 0;
        for (ReportRepository.CategoryExpense transaction : transactions) {
            if (transaction.total > 0) {
                entries.add(new PieEntry((float) transaction.total, transaction.categoryName));
                colors.add(ColorTemplate.MATERIAL_COLORS[colorIndex % ColorTemplate.MATERIAL_COLORS.length]);
                colorIndex++;
            }
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "Нет данных"));
            colors.add(Color.GRAY);
        }

        PieDataSet dataSet = new PieDataSet(entries, type.equals("expense") ? "Расходы" : "Доходы");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    /**
     * Проверяет, есть ли расходы в списке категорий
     * @param expenses - список категорий
     * @return true если есть хотя бы один расход
     */
    public static boolean hasExpenses(List<ReportRepository.CategoryExpense> expenses) {
        for (ReportRepository.CategoryExpense ce : expenses) {
            if (ce.type.equals("expense") && ce.total > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет, есть ли доходы в списке категорий
     * @param expenses - список категорий
     * @return true если есть хотя бы один доход
     */
    public static boolean hasIncomes(List<ReportRepository.CategoryExpense> expenses) {
        for (ReportRepository.CategoryExpense ce : expenses) {
            if (ce.type.equals("income") && ce.total > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Преобразует список транзакций в список CategoryExpense для диаграммы
     * @param transactions - список транзакций из отчёта
     * @param type - тип ("expense" или "income")
     * @return список CategoryExpense
     */
    public static List<ReportRepository.CategoryExpense> convertTransactionsToCategoryExpense(
            List<com.homebudget.database.entities.Transaction> transactions, String type) {
        List<ReportRepository.CategoryExpense> result = new ArrayList<>();
        for (com.homebudget.database.entities.Transaction t : transactions) {
            if (type.equals(t.getType())) {
                ReportRepository.CategoryExpense ce = new ReportRepository.CategoryExpense();
                ce.categoryName = t.getNote() != null && !t.getNote().isEmpty() ? t.getNote() : "Без названия";
                ce.total = t.getAmount();
                ce.type = type;
                result.add(ce);
            }
        }
        return result;
    }
}