package com.example.doancn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Product;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListActivity extends AppCompatActivity {

    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private Button btnAddProduct;
    private Spinner spinnerCategory;
    private SearchView searchView;
    private ImageButton btnBack;

    // --- THÊM BIẾN CHO PHÂN TRANG ---
    private Button btnPrevPage, btnNextPage;
    private TextView tvPageInfo;

    private String currentRole;
    private String[] categories = {"TẤT CẢ", "IT", "TV", "GD"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_product_list);

        initViews();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }

    private void initViews() {
        rvProducts = findViewById(R.id.rvProducts);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        searchView = findViewById(R.id.searchView);

        // ĐƯA LỆNH TÌM ID VÀO ĐÂY
        btnBack = findViewById(R.id.btnBackProductList);

        // --- ÁNH XẠ NÚT PHÂN TRANG ---
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageInfo = findViewById(R.id.tvPageInfo);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences pref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        currentRole = pref.getString("role", "staff");
        btnAddProduct.setVisibility("admin".equalsIgnoreCase(currentRole) ? View.VISIBLE : View.GONE);

        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinAdapter);
    }

    private void setupEvents() {
        // ĐƯA LỆNH BẮT SỰ KIỆN CLICK VÀO ĐÂY
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Lọc theo danh mục
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (adapter != null) {
                    adapter.filterByCategory(categories[pos]);
                    updatePageInfo(); // Cập nhật số trang sau khi lọc
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });

        // Tìm kiếm
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filter(newText);
                    updatePageInfo(); // Cập nhật số trang sau khi tìm kiếm
                }
                return true;
            }
        });

        // Nút Thêm sản phẩm
        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(ProductListActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        // --- SỰ KIỆN NÚT PHÂN TRANG ---
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

    private void loadProducts() {
        RetrofitClient.getApiService().getAllProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (adapter == null) {
                        adapter = new ProductAdapter(response.body(), ProductListActivity.this, currentRole, null);
                        rvProducts.setAdapter(adapter);
                    } else {
                        adapter.updateData(response.body());
                    }
                    updatePageInfo(); // Load xong API thì cập nhật số trang hiển thị
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Toast.makeText(ProductListActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}