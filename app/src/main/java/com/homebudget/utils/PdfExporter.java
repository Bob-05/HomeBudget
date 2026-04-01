package com.homebudget.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;
import com.homebudget.database.entities.Transaction;
import com.homebudget.database.repositories.ReportRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfExporter {

    private static final String TAG = "PdfExporter";
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 50;
    private static final int LINE_HEIGHT = 20;

    public static File exportReport(Context context, String reportType,
                                    ReportRepository.ReportData report,
                                    List<ReportRepository.CategoryExpense> categoryExpenses) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "budget_report_" + sdf.format(new Date()) + ".pdf";

            File pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);

            int y = MARGIN;

            // Заголовок с датой
            paint.setTextSize(22);
            paint.setFakeBoldText(true);
            canvas.drawText("📊 " + getReportTitle(reportType), MARGIN, y, paint);
            y += LINE_HEIGHT * 2;

            // Дата создания отчёта
            paint.setTextSize(10);
            paint.setFakeBoldText(false);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
            canvas.drawText("Дата создания: " + dateFormat.format(new Date()), MARGIN, y, paint);
            y += LINE_HEIGHT;

            // Период
            SimpleDateFormat periodFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            canvas.drawText("Период: " + periodFormat.format(report.startDate) + " - " +
                    periodFormat.format(report.endDate), MARGIN, y, paint);
            y += LINE_HEIGHT * 2;

            // Содержимое в зависимости от типа отчета
            switch (reportType) {
                case "income":
                    y = drawIncomeReport(canvas, paint, report, y);
                    if (report.totalIncome > 0 && !report.transactions.isEmpty()) {
                        y += LINE_HEIGHT;
                        y = drawIncomeChart(canvas, paint, report, y);
                    }
                    break;
                case "expense":
                    y = drawExpenseReport(canvas, paint, report, y);
                    if (report.totalExpense > 0 && !report.transactions.isEmpty()) {
                        y += LINE_HEIGHT;
                        y = drawExpenseChart(canvas, paint, report, y);
                    }
                    break;
                case "summary":
                    y = drawSummaryReport(canvas, paint, report, y);
                    if (report.totalIncome > 0 || report.totalExpense > 0) {
                        y += LINE_HEIGHT;
                        y = drawComparisonChart(canvas, paint, report, y);
                    }
                    break;
                case "categories":
                    y = drawCategoriesReport(canvas, paint, categoryExpenses, report, y);
                    break;
                case "period":
                default:
                    y = drawTransactionsReport(canvas, paint, report.transactions, report, y);
                    break;
            }


            // Нижний колонтитул
            paint.setTextSize(8);
            paint.setFakeBoldText(false);
            paint.setColor(Color.GRAY);
            canvas.drawText("Отчёт создан в приложении HomeBudget", MARGIN, PAGE_HEIGHT - 30, paint);
            canvas.drawText("©" + Calendar.getInstance().get(Calendar.YEAR) + " HomeBudget", MARGIN, PAGE_HEIGHT - 20, paint);

            document.finishPage(page);

            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Log.d(TAG, "PDF exported to: " + pdfFile.getAbsolutePath());
            return pdfFile;

        } catch (Exception e) {
            Log.e(TAG, "Error exporting PDF", e);
            return null;
        }
    }

    private static String getReportTitle(String reportType) {
        switch (reportType) {
            case "income": return "Отчет по доходам";
            case "expense": return "Отчет по расходам";
            case "summary": return "Сводный отчет";
            case "categories": return "Отчет по категориям";
            default: return "Отчет по транзакциям";
        }
    }

    private static int drawIncomeReport(Canvas canvas, Paint paint,
                                        ReportRepository.ReportData report, int y) {
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawText("💰 ДОХОДЫ", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        paint.setColor(Color.BLACK);
        canvas.drawText("Общая сумма доходов:", MARGIN, y, paint);
        paint.setTextSize(18);
        paint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawText(String.format("%.2f ₽", report.totalIncome), MARGIN + 150, y, paint);
        y += LINE_HEIGHT * 1.5;

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        paint.setColor(Color.BLACK);
        canvas.drawText("Количество транзакций: " + report.transactions.size(), MARGIN, y, paint);
        y += LINE_HEIGHT;

        double avgIncome = report.transactions.isEmpty() ? 0 : report.totalIncome / report.transactions.size();
        canvas.drawText(String.format("Средний доход: %.2f ₽", avgIncome), MARGIN, y, paint);
        y += LINE_HEIGHT * 2;

        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("ДЕТАЛИЗАЦИЯ", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(10);
        paint.setFakeBoldText(false);

        int count = 1;
        for (Transaction t : report.transactions) {
            if (y > PAGE_HEIGHT - 150) {
                y = MARGIN;
            }
            String line = String.format("%d. %.2f ₽", count, t.getAmount());
            if (t.getNote() != null && !t.getNote().isEmpty()) {
                line += " — " + t.getNote();
            }
            line += " (" + android.text.format.DateFormat.format("dd.MM.yyyy", t.getDateTime()) + ")";
            canvas.drawText(line, MARGIN, y, paint);
            y += LINE_HEIGHT;
            count++;
        }

        return y;
    }

    private static int drawExpenseReport(Canvas canvas, Paint paint,
                                         ReportRepository.ReportData report, int y) {
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#F44336"));
        canvas.drawText("💸 РАСХОДЫ", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        paint.setColor(Color.BLACK);
        canvas.drawText("Общая сумма расходов:", MARGIN, y, paint);
        paint.setTextSize(18);
        paint.setColor(Color.parseColor("#F44336"));
        canvas.drawText(String.format("%.2f ₽", report.totalExpense), MARGIN + 150, y, paint);
        y += LINE_HEIGHT * 1.5;

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        paint.setColor(Color.BLACK);
        canvas.drawText("Количество транзакций: " + report.transactions.size(), MARGIN, y, paint);
        y += LINE_HEIGHT;

        double avgExpense = report.transactions.isEmpty() ? 0 : report.totalExpense / report.transactions.size();
        canvas.drawText(String.format("Средний расход: %.2f ₽", avgExpense), MARGIN, y, paint);
        y += LINE_HEIGHT * 2;

        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("ДЕТАЛИЗАЦИЯ", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(10);
        paint.setFakeBoldText(false);

        int count = 1;
        for (Transaction t : report.transactions) {
            if (y > PAGE_HEIGHT - 150) {
                y = MARGIN;
            }
            String line = String.format("%d. %.2f ₽", count, t.getAmount());
            if (t.getNote() != null && !t.getNote().isEmpty()) {
                line += " — " + t.getNote();
            }
            line += " (" + android.text.format.DateFormat.format("dd.MM.yyyy", t.getDateTime()) + ")";
            canvas.drawText(line, MARGIN, y, paint);
            y += LINE_HEIGHT;
            count++;
        }

        return y;
    }

    private static int drawSummaryReport(Canvas canvas, Paint paint,
                                         ReportRepository.ReportData report, int y) {
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#2196F3"));
        canvas.drawText("📊 СВОДНЫЙ ОТЧЕТ", MARGIN, y, paint);
        y += LINE_HEIGHT * 1.5;

        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawText("ДОХОДЫ", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        paint.setColor(Color.BLACK);
        canvas.drawText(String.format("💰 Общая сумма: %.2f ₽", report.totalIncome), MARGIN + 20, y, paint);
        y += LINE_HEIGHT;
        double total = report.totalIncome + report.totalExpense;
        if (total > 0) {
            canvas.drawText(String.format("📊 Процент от общего оборота: %.1f%%",
                    (report.totalIncome / total) * 100), MARGIN + 20, y, paint);
            y += LINE_HEIGHT;
        }
        y += LINE_HEIGHT * 0.5;

        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#F44336"));
        canvas.drawText("РАСХОДЫ", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        paint.setColor(Color.BLACK);
        canvas.drawText(String.format("💸 Общая сумма: %.2f ₽", report.totalExpense), MARGIN + 20, y, paint);
        y += LINE_HEIGHT;
        if (total > 0) {
            canvas.drawText(String.format("📊 Процент от общего оборота: %.1f%%",
                    (report.totalExpense / total) * 100), MARGIN + 20, y, paint);
            y += LINE_HEIGHT;
        }
        y += LINE_HEIGHT * 0.5;

        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("БАЛАНС", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(16);
        if (report.balance >= 0) {
            paint.setColor(Color.parseColor("#4CAF50"));
            canvas.drawText(String.format("✅ +%.2f ₽", report.balance), MARGIN + 20, y, paint);
        } else {
            paint.setColor(Color.parseColor("#F44336"));
            canvas.drawText(String.format("⚠️ %.2f ₽", report.balance), MARGIN + 20, y, paint);
        }
        y += LINE_HEIGHT * 2;

        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        paint.setColor(Color.BLACK);
        canvas.drawText("АНАЛИЗ", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(11);
        paint.setFakeBoldText(false);

        if (report.balance > 0) {
            double savingsRate = (report.balance / report.totalIncome) * 100;
            canvas.drawText(String.format("✅ Ты откладываешь %.1f%% доходов. Это отличный результат!", savingsRate), MARGIN + 10, y, paint);
            y += LINE_HEIGHT;
            if (savingsRate < 20) {
                canvas.drawText("💡 Рекомендуем увеличить норму сбережений до 20%", MARGIN + 10, y, paint);
                y += LINE_HEIGHT;
            }
        } else if (report.balance < 0) {
            canvas.drawText("⚠️ Расходы превышают доходы. Рекомендуем сократить расходы.", MARGIN + 10, y, paint);
            y += LINE_HEIGHT;
            canvas.drawText("💡 Начни с категорий, которые приносят меньше всего пользы.", MARGIN + 10, y, paint);
            y += LINE_HEIGHT;
        } else {
            canvas.drawText("💰 Доходы равны расходам. Попробуй немного сократить траты.", MARGIN + 10, y, paint);
            y += LINE_HEIGHT;
        }

        return y;
    }

    private static int drawCategoriesReport(Canvas canvas, Paint paint,
                                            List<ReportRepository.CategoryExpense> expenses,
                                            ReportRepository.ReportData report, int y) {
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#9C27B0"));
        canvas.drawText("📂 ОТЧЕТ ПО КАТЕГОРИЯМ", MARGIN, y, paint);
        y += LINE_HEIGHT * 1.5;

        // Расходы по категориям
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#F44336"));
        canvas.drawText("РАСХОДЫ ПО КАТЕГОРИЯМ", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        paint.setColor(Color.BLACK);
        canvas.drawText(String.format("Общая сумма расходов: %.2f ₽", report.totalExpense), MARGIN + 20, y, paint);
        y += LINE_HEIGHT;

        // Диаграмма расходов
        if (report.totalExpense > 0) {
            y = drawCategoryChart(canvas, paint, expenses, "expense", report.totalExpense, y);
        }

        // Доходы по категориям
        boolean hasIncomes = false;
        for (ReportRepository.CategoryExpense ce : expenses) {
            if ("income".equals(ce.type)) {
                hasIncomes = true;
                break;
            }
        }

        if (hasIncomes) {
            y += LINE_HEIGHT * 2;
            paint.setTextSize(14);
            paint.setFakeBoldText(true);
            paint.setColor(Color.parseColor("#4CAF50"));
            canvas.drawText("ДОХОДЫ ПО КАТЕГОРИЯМ", MARGIN, y, paint);
            y += LINE_HEIGHT;

            paint.setTextSize(12);
            paint.setFakeBoldText(false);
            paint.setColor(Color.BLACK);
            canvas.drawText(String.format("Общая сумма доходов: %.2f ₽", report.totalIncome), MARGIN + 20, y, paint);
            y += LINE_HEIGHT;

            // Диаграмма доходов
            if (report.totalIncome > 0) {
                y = drawCategoryChart(canvas, paint, expenses, "income", report.totalIncome, y);
            }
        }

        return y;
    }

    private static int drawCategoryChart(Canvas canvas, Paint paint,
                                         List<ReportRepository.CategoryExpense> expenses,
                                         String type, double total, int startY) {
        int y = startY + 10;
        int centerX = PAGE_WIDTH / 2;
        int centerY = y + 80;
        int radius = 70;

        paint.setTextSize(11);
        paint.setFakeBoldText(true);
        canvas.drawText(type.equals("expense") ? "Круговая диаграмма расходов" : "Круговая диаграмма доходов",
                MARGIN, y - 10, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(centerX, centerY, radius, paint);

        float startAngle = 0;
        int[] colors = {0xFF4CAF50, 0xFFFF9800, 0xFFF44336, 0xFF2196F3, 0xFF9C27B0, 0xFFFFEB3B, 0xFF00BCD4, 0xFF795548};
        int colorIndex = 0;

        for (ReportRepository.CategoryExpense ce : expenses) {
            if (ce.type.equals(type) && ce.total > 0) {
                float sweepAngle = (float) (ce.total / total * 360);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(colors[colorIndex % colors.length]);
                canvas.drawArc(centerX - radius, centerY - radius,
                        centerX + radius, centerY + radius,
                        startAngle, sweepAngle, true, paint);

                startAngle += sweepAngle;
                colorIndex++;
            }
        }

        // Легенда
        y = centerY + radius + 20;
        paint.setTextSize(9);
        paint.setStyle(Paint.Style.FILL);
        paint.setFakeBoldText(false);

        colorIndex = 0;
        for (ReportRepository.CategoryExpense ce : expenses) {
            if (ce.type.equals(type) && ce.total > 0) {
                if (y > PAGE_HEIGHT - 50) {
                    y = MARGIN;
                }
                paint.setColor(colors[colorIndex % colors.length]);
                canvas.drawRect(MARGIN, y - 12, MARGIN + 15, y, paint);
                paint.setColor(Color.BLACK);
                String legend = ce.categoryName + " (" + String.format("%.1f", ce.percentage) + "%)";
                canvas.drawText(legend, MARGIN + 25, y, paint);
                y += LINE_HEIGHT;
                colorIndex++;
            }
        }

        return y + LINE_HEIGHT;
    }

    private static int drawIncomeChart(Canvas canvas, Paint paint,
                                       ReportRepository.ReportData report, int startY) {
        int y = startY;
        int centerX = PAGE_WIDTH / 2;
        int centerY = y + 80;
        int radius = 70;

        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        canvas.drawText("Круговая диаграмма доходов", MARGIN, y, paint);
        y += 10;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(centerX, centerY, radius, paint);

        float startAngle = 0;
        int[] colors = {0xFF4CAF50, 0xFFFF9800, 0xFF2196F3, 0xFF9C27B0, 0xFFFFEB3B, 0xFF00BCD4};
        int colorIndex = 0;

        for (Transaction t : report.transactions) {
            if ("income".equals(t.getType())) {
                float sweepAngle = (float) (t.getAmount() / report.totalIncome * 360);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(colors[colorIndex % colors.length]);
                canvas.drawArc(centerX - radius, centerY - radius,
                        centerX + radius, centerY + radius,
                        startAngle, sweepAngle, true, paint);

                startAngle += sweepAngle;
                colorIndex++;
            }
        }

        // Легенда
        y = centerY + radius + 20;
        paint.setTextSize(9);
        paint.setStyle(Paint.Style.FILL);
        paint.setFakeBoldText(false);

        colorIndex = 0;
        for (Transaction t : report.transactions) {
            if ("income".equals(t.getType())) {
                if (y > PAGE_HEIGHT - 50) {
                    y = MARGIN;
                }
                paint.setColor(colors[colorIndex % colors.length]);
                canvas.drawRect(MARGIN, y - 12, MARGIN + 15, y, paint);
                paint.setColor(Color.BLACK);
                double percent = (t.getAmount() / report.totalIncome) * 100;
                String note = t.getNote() != null ? t.getNote() : "Доход";
                String legend = note + " (" + String.format("%.1f", percent) + "%)";
                if (legend.length() > 30) {
                    legend = legend.substring(0, 27) + "...";
                }
                canvas.drawText(legend, MARGIN + 25, y, paint);
                y += LINE_HEIGHT;
                colorIndex++;
            }
        }

        return y + LINE_HEIGHT;
    }

    private static int drawExpenseChart(Canvas canvas, Paint paint,
                                        ReportRepository.ReportData report, int startY) {
        int y = startY;
        int centerX = PAGE_WIDTH / 2;
        int centerY = y + 80;
        int radius = 70;

        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        canvas.drawText("Круговая диаграмма расходов", MARGIN, y, paint);
        y += 10;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(centerX, centerY, radius, paint);

        float startAngle = 0;
        int[] colors = {0xFFF44336, 0xFFFF9800, 0xFF2196F3, 0xFF9C27B0, 0xFFFFEB3B, 0xFF00BCD4};
        int colorIndex = 0;

        for (Transaction t : report.transactions) {
            if ("expense".equals(t.getType())) {
                float sweepAngle = (float) (t.getAmount() / report.totalExpense * 360);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(colors[colorIndex % colors.length]);
                canvas.drawArc(centerX - radius, centerY - radius,
                        centerX + radius, centerY + radius,
                        startAngle, sweepAngle, true, paint);

                startAngle += sweepAngle;
                colorIndex++;
            }
        }

        // Легенда
        y = centerY + radius + 20;
        paint.setTextSize(9);
        paint.setStyle(Paint.Style.FILL);
        paint.setFakeBoldText(false);

        colorIndex = 0;
        for (Transaction t : report.transactions) {
            if ("expense".equals(t.getType())) {
                if (y > PAGE_HEIGHT - 50) {
                    y = MARGIN;
                }
                paint.setColor(colors[colorIndex % colors.length]);
                canvas.drawRect(MARGIN, y - 12, MARGIN + 15, y, paint);
                paint.setColor(Color.BLACK);
                double percent = (t.getAmount() / report.totalExpense) * 100;
                String note = t.getNote() != null ? t.getNote() : "Расход";
                String legend = note + " (" + String.format("%.1f", percent) + "%)";
                if (legend.length() > 30) {
                    legend = legend.substring(0, 27) + "...";
                }
                canvas.drawText(legend, MARGIN + 25, y, paint);
                y += LINE_HEIGHT;
                colorIndex++;
            }
        }

        return y + LINE_HEIGHT;
    }

    private static int drawComparisonChart(Canvas canvas, Paint paint,
                                           ReportRepository.ReportData report, int startY) {
        int y = startY;
        int centerX = PAGE_WIDTH / 2;
        int centerY = y + 80;
        int radius = 70;
        double total = report.totalIncome + report.totalExpense;

        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        canvas.drawText("Сравнение доходов и расходов", MARGIN, y, paint);
        y += 10;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(centerX, centerY, radius, paint);

        float startAngle = 0;

        if (report.totalIncome > 0) {
            float sweepAngle = (float) (report.totalIncome / total * 360);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#4CAF50"));
            canvas.drawArc(centerX - radius, centerY - radius,
                    centerX + radius, centerY + radius,
                    startAngle, sweepAngle, true, paint);
            startAngle += sweepAngle;
        }

        if (report.totalExpense > 0) {
            float sweepAngle = (float) (report.totalExpense / total * 360);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#F44336"));
            canvas.drawArc(centerX - radius, centerY - radius,
                    centerX + radius, centerY + radius,
                    startAngle, sweepAngle, true, paint);
        }

        // Легенда
        y = centerY + radius + 20;
        paint.setTextSize(10);
        paint.setStyle(Paint.Style.FILL);
        paint.setFakeBoldText(false);

        paint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawRect(MARGIN, y - 12, MARGIN + 15, y, paint);
        paint.setColor(Color.BLACK);
        double incomePercent = (report.totalIncome / total) * 100;
        canvas.drawText("Доходы (" + String.format("%.1f", incomePercent) + "%)", MARGIN + 25, y, paint);
        y += LINE_HEIGHT;

        paint.setColor(Color.parseColor("#F44336"));
        canvas.drawRect(MARGIN, y - 12, MARGIN + 15, y, paint);
        paint.setColor(Color.BLACK);
        double expensePercent = (report.totalExpense / total) * 100;
        canvas.drawText("Расходы (" + String.format("%.1f", expensePercent) + "%)", MARGIN + 25, y, paint);

        return y + LINE_HEIGHT;
    }

    private static int drawTransactionsReport(Canvas canvas, Paint paint,
                                              List<Transaction> transactions,
                                              ReportRepository.ReportData report, int y) {
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#FF9800"));
        canvas.drawText("📅 ТРАНЗАКЦИИ ЗА ПЕРИОД", MARGIN, y, paint);
        y += LINE_HEIGHT * 1.5;

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        paint.setColor(Color.BLACK);
        canvas.drawText(String.format("Всего транзакций: %d", transactions.size()), MARGIN, y, paint);
        y += LINE_HEIGHT;
        canvas.drawText(String.format("💰 Доходы: %.2f ₽", report.totalIncome), MARGIN, y, paint);
        y += LINE_HEIGHT;
        canvas.drawText(String.format("💸 Расходы: %.2f ₽", report.totalExpense), MARGIN, y, paint);
        y += LINE_HEIGHT;
        canvas.drawText(String.format("📊 Баланс: %.2f ₽", report.balance), MARGIN, y, paint);
        y += LINE_HEIGHT * 2;

        // Диаграмма для транзакций, если есть данные
        if (transactions.size() <= 20 && report.totalIncome > 0 && report.totalExpense > 0) {
            y = drawComparisonChart(canvas, paint, report, y);
            y += LINE_HEIGHT;
        }

        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("ДЕТАЛИЗАЦИЯ", MARGIN, y, paint);
        y += LINE_HEIGHT;

        paint.setTextSize(9);
        paint.setFakeBoldText(false);

        int count = 1;
        for (Transaction t : transactions) {
            if (y > PAGE_HEIGHT - 100) {
                y = MARGIN;
            }
            String typeIcon = "income".equals(t.getType()) ? "💰" : "💸";
            String line = String.format("%d. %s %.2f ₽", count, typeIcon, t.getAmount());
            if (t.getNote() != null && !t.getNote().isEmpty()) {
                line += " — " + t.getNote();
            }
            line += " (" + android.text.format.DateFormat.format("dd.MM.yyyy HH:mm", t.getDateTime()) + ")";
            canvas.drawText(line, MARGIN, y, paint);
            y += LINE_HEIGHT;
            count++;
        }

        return y;
    }
}