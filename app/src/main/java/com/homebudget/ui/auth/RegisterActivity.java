package com.homebudget.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.homebudget.R;
import com.homebudget.viewmodels.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private EditText etLogin, etEmail, etPassword, etConfirmPassword, etSecurityAnswer;
    private Spinner spSecurityQuestion;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private AuthViewModel viewModel;

    private String[] securityQuestions = {
            "Ваше любимое животное?",
            "Имя вашего первого питомца?",
            "Ваша любимая книга?",
            "Город вашего рождения?",
            "Ваша любимая еда?"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupViewModel();
        setupSpinner();
        setupClickListeners();

        // Добавляем кнопку назад в ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Регистрация");
        }
    }

    private void initViews() {
        etLogin = findViewById(R.id.et_login);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        spSecurityQuestion = findViewById(R.id.sp_security_question);
        etSecurityAnswer = findViewById(R.id.et_security_answer);
        btnRegister = findViewById(R.id.btn_register);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        viewModel.getRegisterResult().observe(this, result -> {
            if (result > 0) {
                Toast.makeText(this, "Регистрация успешна! Теперь войдите.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            } else if (result == -1) {
                Toast.makeText(this, "Логин уже существует", Toast.LENGTH_SHORT).show();
            } else if (result == -2) {
                Toast.makeText(this, "Email уже существует", Toast.LENGTH_SHORT).show();
            } else if (result == -3) {
                Toast.makeText(this, "Пароль должен быть не менее 8 символов и содержать буквы и цифры", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, securityQuestions);
        /*
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, securityQuestions);
         */
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSecurityQuestion.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();
            String securityQuestion = spSecurityQuestion.getSelectedItem().toString();
            String securityAnswer = etSecurityAnswer.getText().toString().trim();

            if (login.isEmpty()) {
                etLogin.setError("Введите логин");
                return;
            }
            if (email.isEmpty()) {
                etEmail.setError("Введите email");
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Введите корректный email");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Введите пароль");
                return;
            }
            if (password.length() < 8) {
                etPassword.setError("Пароль должен быть не менее 8 символов");
                return;
            }
            if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError("Пароли не совпадают");
                return;
            }
            if (securityAnswer.isEmpty()) {
                etSecurityAnswer.setError("Введите ответ на секретный вопрос");
                return;
            }

            viewModel.register(login, email, password, securityQuestion, securityAnswer);
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}