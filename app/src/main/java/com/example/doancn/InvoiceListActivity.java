package com.example.doancn;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
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
import java.util.Calendar;
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
    private Button btnFilterDate, btnFilterMonth;

    // Danh sách gốc chứa TẤT CẢ hóa đơn tải về từ Server 1 lần duy nhất
    private List<Invoice> fullList = new ArrayList<>();

    // --- CÁC BIẾN LƯU TRẠNG THÁI LỌC CỤC BỘ ---
    private String currentQuery = "";
    private String currentStatusFilter = "Tất cả";
    private String filterDateStr = "";  // Chuỗi lọc ngày (VD: "2026-05-28")
    private String filterMonthStr = ""; // Chuỗi lọc tháng (VD: "2026-05")

    private Integer filterUserId = null;
    private Integer filterCustomerId = null;
    private boolean isShipperView = false;

    private String[] statusOptions = {"Tất cả", "Chờ xác nhận", "Giao hàng thành công", "Giao hàng thất bại", "Đang giao hàng"};

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

        btnFilterDate = findViewById(R.id.btnFilterDate);
        btnFilterMonth = findViewById(R.id.btnFilterMonth);

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
        filterUserId = getIntent().getIntExtra("FILTER_USER_ID", -1) != -1 ? getIntent().getIntExtra("FILTER_USER_ID", -1) : null;
        filterCustomerId = getIntent().getIntExtra("FILTER_CUSTOMER_ID", -1) != -1 ? getIntent().getIntExtra("FILTER_CUSTOMER_ID", -1) : null;

        // Bật hiển thị bảng Thống kê doanh thu
        layoutSummary.setVisibility(View.VISIBLE);

        String title = getIntent().getStringExtra("SCREEN_TITLE");
        if (title != null) tvToolbarTitle.setText(title);
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        // Lọc theo trạng thái
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStatusFilter = statusOptions[position];
                applyLocalFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Lọc theo ID Hóa đơn
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                applyLocalFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Lọc theo NGÀY
        btnFilterDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                filterDateStr = String.format("%04d-%02d-%02d", year, month + 1, day);
                filterMonthStr = ""; // Reset tháng

                btnFilterDate.setText(String.format("%02d/%02d", day, month + 1));
                btnFilterMonth.setText("Tháng");

                applyLocalFilters();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Lọc theo THÁNG
        btnFilterMonth.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Chọn Tháng / Năm");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(0, 40, 0, 10);

            final NumberPicker monthPicker = new NumberPicker(this);
            monthPicker.setMinValue(1);
            monthPicker.setMaxValue(12);
            monthPicker.setValue(Calendar.getInstance().get(Calendar.MONTH) + 1);

            final NumberPicker yearPicker = new NumberPicker(this);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            yearPicker.setMinValue(2020);
            yearPicker.setMaxValue(currentYear + 5);
            yearPicker.setValue(currentYear);

            layout.addView(monthPicker);
            layout.addView(yearPicker);
            builder.setView(layout);

            builder.setPositiveButton("Lọc", (dialog, which) -> {
                filterMonthStr = String.format("%04d-%02d", yearPicker.getValue(), monthPicker.getValue());
                filterDateStr = ""; // Reset ngày

                btnFilterMonth.setText(String.format("%02d/%04d", monthPicker.getValue(), yearPicker.getValue()));
                btnFilterDate.setText("Ngày");

                applyLocalFilters();
            });
            builder.setNegativeButton("Hủy", null);
            builder.show();
        });

        // Hủy lọc (Nhấn giữ vào nút Ngày hoặc Tháng để xóa lọc)
        View.OnLongClickListener clearFilter = v -> {
            filterDateStr = "";
            filterMonthStr = "";
            btnFilterDate.setText("Ngày");
            btnFilterMonth.setText("Tháng");
            applyLocalFilters();
            Toast.makeText(this, "Đã bỏ lọc thời gian", Toast.LENGTH_SHORT).show();
            return true;
        };
        btnFilterDate.setOnLongClickListener(clearFilter);
        btnFilterMonth.setOnLongClickListener(clearFilter);

        btnPrevPage.setOnClickListener(v -> {
            if (adapter != null && adapter.prevPage()) updatePageInfo();
        });

        btnNextPage.setOnClickListener(v -> {
            if (adapter != null && adapter.nextPage()) updatePageInfo();
        });
    }

    // =================================================================================
    // HÀM QUAN TRỌNG NHẤT: LỌC TRÊN FRONTEND VÀ TÍNH TỔNG DOANH THU CÁC ĐƠN ĐANG HIỆN
    // =================================================================================
    private void applyLocalFilters() {
        if (fullList == null) return;

        List<Invoice> filteredList = new ArrayList<>();
        double totalRevenue = 0;
        int successCount = 0;

        for (Invoice inv : fullList) {
            boolean matchStatus = currentStatusFilter.equals("Tất cả") || inv.getStatus().equalsIgnoreCase(currentStatusFilter);
            boolean matchSearch = currentQuery.isEmpty() || String.valueOf(inv.getId()).contains(currentQuery);

            String createdAt = inv.getCreatedAt() != null ? inv.getCreatedAt() : "";
            boolean matchDate = filterDateStr.isEmpty() || createdAt.contains(filterDateStr);
            boolean matchMonth = filterMonthStr.isEmpty() || createdAt.contains(filterMonthStr);

            if (matchStatus && matchSearch && matchDate && matchMonth) {
                filteredList.add(inv);

                if ("Giao hàng thành công".equalsIgnoreCase(inv.getStatus())) {
                    totalRevenue += inv.getTotalAmount();
                    successCount++;
                }
            }
        }

        // ================= CẬP NHẬT LÊN UI DOANH THU =================
        DecimalFormat formatter = new DecimalFormat("#,###,###,##0");
        tvTotalSales.setText(formatter.format(totalRevenue) + " VNĐ");

        // Móc 2 cái thẻ TextView chứa chữ tiêu đề ra
        TextView labelTotalSales = (TextView) ((LinearLayout) tvTotalSales.getParent()).getChildAt(0);
        TextView labelCommission = (TextView) ((LinearLayout) tvCommission.getParent()).getChildAt(0);

        if(filterUserId != null && !isShipperView) {
            // [TRƯỜNG HỢP 1: STAFF VÀO XEM LỊCH SỬ CỦA MÌNH]
            labelTotalSales.setText("Doanh số cá nhân:");
            labelCommission.setText("Thực nhận (Hoa hồng 3%):");
            tvCommission.setText(formatter.format(totalRevenue * 0.03) + " VNĐ");
            tvCommission.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            // [TRƯỜNG HỢP 2: ADMIN VÀO XEM TOÀN BỘ HÓA ĐƠN]
            labelTotalSales.setText("Tổng doanh thu:");
            labelCommission.setText("Số đơn thành công:");
            tvCommission.setText(String.valueOf(successCount) + " đơn");
            tvCommission.setTextColor(Color.parseColor("#4CAF50"));
        }
        // ==============================================================

        if (filteredList.isEmpty()) {
            tvEmptyInvoice.setText("Không có hóa đơn phù hợp");
            tvEmptyInvoice.setVisibility(View.VISIBLE);
            rvInvoices.setVisibility(View.GONE);
        } else {
            tvEmptyInvoice.setVisibility(View.GONE);
            rvInvoices.setVisibility(View.VISIBLE);
        }

        if (adapter == null) {
            adapter = new InvoiceAdapter(filteredList);
            rvInvoices.setAdapter(adapter);
        } else {
            adapter.updateList(filteredList);
        }
        updatePageInfo();
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
        } else if (filterCustomerId != null) {
            apiCall = RetrofitClient.getApiService().getInvoicesByCustomer(filterCustomerId);
        } else {
            apiCall = RetrofitClient.getApiService().getInvoices(filterUserId);
        }

        apiCall.enqueue(new Callback<List<Invoice>>() {
            @Override
            public void onResponse(Call<List<Invoice>> call, Response<List<Invoice>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullList = response.body();
                    applyLocalFilters();
                }
            }
            @Override public void onFailure(Call<List<Invoice>> call, Throwable t) {
                Toast.makeText(InvoiceListActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInvoices();
    }
}