package com.example.doancn;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeListActivity extends AppCompatActivity {
    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private SearchView searchView;
    private ImageButton btnBack;


    // --- BIẾN PHÂN TRANG ---
    private Button btnPrevPage, btnNextPage;
    private TextView tvPageInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_list); // Nhớ check lại tên layout của ông cho khớp nhé

        initViews();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEmployees(); // Cứ quay lại màn hình này là nó tự gọi API lấy list mới
    }

    private void initViews() {
        // 1. Ánh xạ View
        rvEmployees = findViewById(R.id.rvEmployees);
        searchView = findViewById(R.id.searchView);

        // --- ĐÂY LÀ DÒNG ÔNG CẦN THÊM ---
        // Lưu ý: Đổi "R.id.btnBack" thành đúng cái ID ông đã đặt bên file XML nhé
        btnBack = findViewById(R.id.btnBackEmployeeList);

        // Ánh xạ các nút phân trang
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageInfo = findViewById(R.id.tvPageInfo);

        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
    }


    private void setupEvents() {
        // 2. Xử lý nút quay lại
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 3. Xử lý tìm kiếm
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filter(newText);
                    updatePageInfo(); // Cập nhật lại số trang sau khi tìm kiếm
                }
                return true;
            }
        });

        // 4. Xử lý nút phân trang
        btnPrevPage.setOnClickListener(v -> {
            if (adapter != null && adapter.prevPage()) {
                updatePageInfo();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (adapter != null && adapter.nextPage()) {
                updatePageInfo();
            }
        });
    }

    // --- HÀM CẬP NHẬT GIAO DIỆN SỐ TRANG ---
    private void updatePageInfo() {
        if (adapter != null) {
            tvPageInfo.setText(adapter.getCurrentPage() + " / " + adapter.getTotalPages());

            // Ẩn/hiện nút (làm mờ) nếu ở trang đầu hoặc trang cuối
            btnPrevPage.setEnabled(adapter.getCurrentPage() > 1);
            btnNextPage.setEnabled(adapter.getCurrentPage() < adapter.getTotalPages());
        }
    }

    private void loadEmployees() {
        RetrofitClient.getApiService().getAllEmployees().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Tối ưu: Nếu chưa có adapter thì tạo mới, có rồi thì chỉ cập nhật data
                    if (adapter == null) {
                        adapter = new EmployeeAdapter(response.body(), EmployeeListActivity.this);
                        rvEmployees.setAdapter(adapter);
                    } else {
                        adapter.updateData(response.body());
                    }
                    updatePageInfo(); // Load API xong thì hiển thị số trang
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage());
                Toast.makeText(EmployeeListActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}