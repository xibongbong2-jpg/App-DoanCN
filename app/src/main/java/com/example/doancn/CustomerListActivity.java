package com.example.doancn;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Customer;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerListActivity extends AppCompatActivity {
    private RecyclerView rvCustomers;
    private CustomerAdapter adapter;
    private SearchView searchView;
    private Button btnPrev, btnNext;
    private TextView tvPageInfo;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);

        initViews();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers(); // Tải lại danh sách để cập nhật dữ liệu mới nhất
    }

    private void initViews() {
        rvCustomers = findViewById(R.id.rvCustomers);
        searchView = findViewById(R.id.searchView);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        btnBack = findViewById(R.id.btnBackCustomerList);

        rvCustomers.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupEvents() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String txt) {
                if (adapter != null) {
                    adapter.filter(txt);
                    updatePageText();
                }
                return true;
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (adapter != null && adapter.prevPage()) updatePageText();
        });

        btnNext.setOnClickListener(v -> {
            if (adapter != null && adapter.nextPage()) updatePageText();
        });
    }

    private void loadCustomers() {
        RetrofitClient.getApiService().getAllCustomers().enqueue(new Callback<List<Customer>>() {
            @Override
            public void onResponse(Call<List<Customer>> call, Response<List<Customer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (adapter == null) {
                        // --- KHỞI TẠO ADAPTER VỚI INTERFACE MỚI ---
                        adapter = new CustomerAdapter(response.body(), CustomerListActivity.this, false, new CustomerAdapter.OnCustomerActionListener() {
                            @Override
                            public void onCustomerClick(Customer customer) {
                                // Không dùng trong màn hình danh sách
                            }

                            @Override
                            public void onDetailClick(Customer customer) {
                                // 1. Xem lịch sử hóa đơn
                                Intent intent = new Intent(CustomerListActivity.this, InvoiceListActivity.class);
                                intent.putExtra("FILTER_CUSTOMER_ID", customer.getId());
                                intent.putExtra("SCREEN_TITLE", "LỊCH SỬ: " + customer.getCusName().toUpperCase());
                                startActivity(intent);
                            }

                            @Override
                            public void onEditClick(Customer customer) {

                                Intent intent = new Intent(CustomerListActivity.this, EditCustomerActivity.class);
                                intent.putExtra("CUSTOMER_DATA", customer);
                                startActivity(intent);
                                Toast.makeText(CustomerListActivity.this, "Sửa khách hàng: " + customer.getCusName(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onDeleteClick(Customer customer) {
                                // 3. Xác nhận xóa
                                showDeleteConfirmDialog(customer);
                            }
                        });
                        rvCustomers.setAdapter(adapter);
                    } else {
                        adapter.updateData(response.body());
                    }
                    updatePageText();
                }
            }

            @Override public void onFailure(Call<List<Customer>> call, Throwable t) {
                Toast.makeText(CustomerListActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmDialog(Customer customer) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa khách hàng " + customer.getCusName() + " không?")
                .setPositiveButton("Xác nhận", (dialog, which) -> deleteCustomer(customer.getId()))
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteCustomer(int id) {
        RetrofitClient.getApiService().deleteCustomer(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CustomerListActivity.this, "Đã xóa khách hàng!", Toast.LENGTH_SHORT).show();
                    loadCustomers(); // Tải lại danh sách sau khi xóa thành công
                } else {
                    Toast.makeText(CustomerListActivity.this, "Không thể xóa khách hàng đã có lịch sử mua hàng!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(CustomerListActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePageText() {
        if (adapter != null) {
            tvPageInfo.setText(adapter.getCurrentPage() + " / " + adapter.getTotalPages());
            btnPrev.setEnabled(adapter.getCurrentPage() > 1);
            btnNext.setEnabled(adapter.getCurrentPage() < adapter.getTotalPages());
        }
    }
}