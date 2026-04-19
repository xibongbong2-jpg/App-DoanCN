package com.example.doancn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        User userLogin = new User(username, password);

        RetrofitClient.getApiService().login(userLogin).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User userResponse = response.body();

                    // 1. LƯU DỮ LIỆU VÀO SharedPreferences
                    SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();

                    editor.putInt("id", userResponse.getId());
                    editor.putString("full_name", userResponse.getFull_name());
                    editor.putString("role", userResponse.getRole());
                    editor.putString("dept", userResponse.getDepartment());
                    editor.putString("avatar", userResponse.getUser_image());
                    editor.putString("dob", userResponse.getDob());

                    editor.apply();

                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    intent.putExtra("FULL_NAME", userResponse.getFull_name());
                    intent.putExtra("ROLE", userResponse.getRole());
                    intent.putExtra("DEPT", userResponse.getDepartment());
                    intent.putExtra("AVATAR", userResponse.getUser_image());
                    intent.putExtra("USER_ID", userResponse.getId());
                    intent.putExtra("DOB", userResponse.getDob());

                    startActivity(intent);
                    finish(); // Đóng trang Login

                } else {
                    Toast.makeText(MainActivity.this, "Tài khoản hoặc mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage());
                Toast.makeText(MainActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_LONG).show();
            }
        });
    }
}