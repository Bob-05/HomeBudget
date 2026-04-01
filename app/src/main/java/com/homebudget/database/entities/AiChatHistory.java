package com.homebudget.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import static androidx.room.ForeignKey.CASCADE;
import java.util.Date;

@Entity(
        tableName = "ai_chat_history",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = CASCADE
        ),
        indices = {
                @Index(value = {"user_id", "created_at"}),
                @Index(value = {"user_id"})
        }
)
public class AiChatHistory {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "question")
    private String question;

    @ColumnInfo(name = "answer")
    private String answer;

    @ColumnInfo(name = "period_start")
    private Date periodStart;

    @ColumnInfo(name = "period_end")
    private Date periodEnd;

    @ColumnInfo(name = "created_at")
    private Date createdAt;

    public AiChatHistory() {}

    @Ignore
    public AiChatHistory(int userId, String question, String answer) {
        this.userId = userId;
        this.question = question;
        this.answer = answer;
        this.createdAt = new Date();
    }

    @Ignore
    public AiChatHistory(int userId, String question, String answer,
                         Date periodStart, Date periodEnd) {
        this.userId = userId;
        this.question = question;
        this.answer = answer;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.createdAt = new Date();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Date getPeriodStart() { return periodStart; }
    public void setPeriodStart(Date periodStart) { this.periodStart = periodStart; }
    public Date getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Date periodEnd) { this.periodEnd = periodEnd; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}