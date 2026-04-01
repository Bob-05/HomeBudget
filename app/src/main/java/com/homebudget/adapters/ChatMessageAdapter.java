package com.homebudget.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.homebudget.R;
import com.homebudget.database.entities.AiChatHistory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    private List<AiChatHistory> messages = new ArrayList<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiChatHistory message = messages.get(position);

        holder.tvQuestion.setText(message.getQuestion() != null ? message.getQuestion() : "");
        holder.tvAnswer.setText(message.getAnswer() != null ? message.getAnswer() : "");

        Date createdAt = message.getCreatedAt();
        if (createdAt != null) {
            holder.tvTime.setText(sdf.format(createdAt));
        } else {
            holder.tvTime.setText("");
        }

        if (message.getQuestion() == null || message.getQuestion().isEmpty()) {
            holder.questionContainer.setVisibility(View.GONE);
        } else {
            holder.questionContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateList(List<AiChatHistory> newList) {
        this.messages = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View questionContainer;
        TextView tvQuestion, tvAnswer, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            questionContainer = itemView.findViewById(R.id.question_container);
            tvQuestion = itemView.findViewById(R.id.tv_question);
            tvAnswer = itemView.findViewById(R.id.tv_answer);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }

    public List<AiChatHistory> getMessages() {
        return messages;
    }
}