package com.example.doancn;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Đã sửa lại lỗi dư dấu chấm ở dòng này
import com.example.doancn.DeliveryHistoryAdapter;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Invoice;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryHistoryActivity extends AppCompatActivity {
    private RecyclerView rvHistoryList;
    private DeliveryHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_history);

        rvHistoryList = findViewById(R.id.rvHistoryList);
        rvHistoryList.setLayoutManager(new LinearLayoutManager(this));

        loadHistoryData();
    }

    private void loadHistoryData() {
        // Lấy ID của ông Shipper đang đăng nhập
        SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
        int currentShipperId = pref.getInt("id", -1);

        if (currentShipperId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID Shipper!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gọi API lấy lịch sử từ Spring Boot
        RetrofitClient.getApiService().getHistoryForShipper(currentShipperId).enqueue(new Callback<List<Invoice>>() {
            @Override
            public void onResponse(Call<List<Invoice>> call, Response<List<Invoice>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Invoice> historyList = response.body();
                    if (historyList.isEmpty()) {
                        Toast.makeText(DeliveryHistoryActivity.this, "Bạn chưa giao thành công đơn nào!", Toast.LENGTH_SHORT).show();
                    }

                    // Đổ dữ liệu vào cái Adapter có ảnh Glide hôm trước
                    adapter = new DeliveryHistoryAdapter(DeliveryHistoryActivity.this, historyList);
                    rvHistoryList.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<Invoice>> call, Throwable t) {
                Toast.makeText(DeliveryHistoryActivity.this, "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}