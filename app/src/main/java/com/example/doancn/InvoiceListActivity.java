package com.example.doancn;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Invoice;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceListActivity extends AppCompatActivity {
    private RecyclerView rvInvoices;
    private InvoiceAdapter adapter;
    private TextView tvToolbarTitle, tvEmptyInvoice;
    private ImageButton btnBack;

    private LinearLayout layoutSummary;
    private TextView tvTotalSales, tvCommission;

    private EditText etSearch;
    private Spinner spinnerStatus;

    private List<Invoice> fullList = new ArrayList<>();

    // --- CÁC BIẾN LỌC DỮ LIỆU ---
    private Integer filterUserId = null;      // Lọc theo nhân viên
    private Integer filterCustomerId = null;  // Lọc theo khách hàng (MỚI)
    private boolean isShipperView = false;

    private String currentQuery = "";
    private String currentStatusFilter = "Tất cả";
    private String[] statusOptions = {"Tất cả", "Chờ xác nhận", "Giao hàng thành công", "Giao hàng thất bại","Đang giao hàng"};

    private Button btnPrevPage, btnNextPage;
    private TextView tvPageInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_list);

        initViews();
        setupDataFromIntent();
        setupEvents();
    }

    private void initViews() {
        rvInvoices = findViewById(R.id.rvInvoiceList);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvEmptyInvoice = findViewById(R.id.tvEmptyInvoice);
        btnBack = findViewById(R.id.btnBackInvoiceList);
        etSearch = findViewById(R.id.etSearchInvoice);
        layoutSummary = findViewById(R.id.layoutSummary);
        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvCommission = findViewById(R.id.tvCommission);

        spinnerStatus = findViewById(R.id.spinnerInvoiceStatus);
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(spinAdapter);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageInfo = findViewById(R.id.tvPageInfo);

        rvInvoices.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupDataFromIntent() {
        isShipperView = getIntent().getBooleanExtra("IS_SHIPPER_VIEW", false);

        // Nhận ID nhân viên (Lịch sử cá nhân)
        int userId = getIntent().getIntExtra("FILTER_USER_ID", -1);

        // --- ĐOẠN MỚI THÊM: Nhận ID khách hàng ---
        int cusId = getIntent().getIntExtra("FILTER_CUSTOMER_ID", -1);

        if (userId != -1 && !isShipperView) {
            filterUserId = userId;
            layoutSummary.setVisibility(View.VISIBLE); // Hiện bảng doanh số cho staff
        } else if (cusId != -1) {
            filterCustomerId = cusId;
            layoutSummary.setVisibility(View.GONE);    // Ẩn bảng doanh số khi xem khách hàng
        } else {
            filterUserId = null;
            filterCustomerId = null;
            layoutSummary.setVisibility(View.GONE);
        }

        String title = getIntent().getStringExtra("SCREEN_TITLE");
        if (title != null && tvToolbarTitle != null) {
            tvToolbarTitle.setText(title);
        }
    }

    private void setupEvents() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStatusFilter = statusOptions[position];
                if (adapter != null) {
                    adapter.filterList(currentQuery, currentStatusFilter);
                    updatePageInfo();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s.toString();
                    if (adapter != null) {
                        adapter.filterList(currentQuery, currentStatusFilter);
                        updatePageInfo();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        btnPrevPage.setOnClickListener(v -> {
            if (adapter != null && adapter.prevPage()) updatePageInfo();
        });

        btnNextPage.setOnClickListener(v -> {
            if (adapter != null && adapter.nextPage()) updatePageInfo();
        });
    }

    private void updatePageInfo() {
        if (adapter != null) {
            tvPageInfo.setText(adapter.getCurrentPage() + " / " + adapter.getTotalPages());
            btnPrevPage.setEnabled(adapter.getCurrentPage() > 1);
            btnNextPage.setEnabled(adapter.getCurrentPage() < adapter.getTotalPages());
        }
    }

    private void loadInvoices() {
        Call<List<Invoice>> apiCall;

        if (isShipperView) {
            SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
            int shipperId = pref.getInt("id", -1);
            apiCall = RetrofitClient.getApiService().getPendingOrdersForShipper(shipperId);
        }
        // --- LOGIC MỚI: Gọi API lấy hóa đơn theo khách hàng ---
        else if (filterCustomerId != null) {
            apiCall = RetrofitClient.getApiService().getInvoicesByCustomer(filterCustomerId);
        }
        else {
            apiCall = RetrofitClient.getApiService().getInvoices(filterUserId);
        }

        apiCall.enqueue(new Callback<List<Invoice>>() {
            @Override
            public void onResponse(Call<List<Invoice>> call, Response<List<Invoice>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullList = response.body();

                    if (fullList.isEmpty()) {
                        tvEmptyInvoice.setText("Không có dữ liệu hóa đơn");
                        tvEmptyInvoice.setVisibility(View.VISIBLE);
                        rvInvoices.setVisibility(View.GONE);
                    } else {
                        tvEmptyInvoice.setVisibility(View.GONE);
                        rvInvoices.setVisibility(View.VISIBLE);

                        // Chỉ tính doanh số nếu là xem lịch sử Staff
                        if (filterUserId != null && !isShipperView && filterCustomerId == null) {
                            calculateAndDisplaySummary(fullList);
                        }

                        if (adapter == null) {
                            adapter = new InvoiceAdapter(new ArrayList<>(fullList));
                            rvInvoices.setAdapter(adapter);
                        } else {
                            adapter.updateList(fullList);
                        }

                        adapter.filterList(currentQuery, currentStatusFilter);
                        updatePageInfo();
                    }
                }
            }

            @Override public void onFailure(Call<List<Invoice>> call, Throwable t) {
                Toast.makeText(InvoiceListActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateAndDisplaySummary(List<Invoice> list) {
        double totalSales = 0;
        for (Invoice inv : list) {
            if ("Giao hàng thành công".equalsIgnoreCase(inv.getStatus())) {
                totalSales += inv.getTotalAmount();
            }
        }
        double commission = totalSales * 0.03;
        DecimalFormat formatter = new DecimalFormat("#,###,###,##0");
        tvTotalSales.setText(formatter.format(totalSales) + " VNĐ");
        tvCommission.setText(formatter.format(commission) + " VNĐ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInvoices();
    }
}