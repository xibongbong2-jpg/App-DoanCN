package com.example.doancn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Customer;
import com.example.doancn.model.Product;
import com.example.doancn.model.User;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateInvoiceActivity extends AppCompatActivity {

    private SearchView svCustomer, svProduct;
    private RecyclerView rvCustomer, rvProduct, rvSelectedProducts;
    private EditText etCusName, etCusPhone, etBaseAddress, etAddressDetail, etUsedPoints, etTotalBillAuto;
    private TextView tvPointWarning, tvCusCreditLabel, tvQrInfo;
    private LinearLayout layoutCustomerInfo, layoutCashPayment, layoutTransferPayment;
    private ImageButton btnClearCustomer;
    private Button btnAddNewCustomer, btnConfirmInvoice;

    private RadioGroup rgPaymentMethod;
    private CheckBox cbConfirmCash;
    private ImageView ivQrCode;
    private String paymentMethod = "Tiền mặt";

    private CustomerAdapter customerAdapter;
    private String currentPaymentCode = "";
    private ProductAdapter productAdapter;
    private SelectedProductAdapter selectedAdapter;

    private List<Customer> allCustomers = new ArrayList<>();
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> selectedProductsList = new ArrayList<>();

    // === GIỮ NGUYÊN DANH SÁCH SHIPPER CỦA ÔNG ===
    private List<User> allShippers = new ArrayList<>();

    private Customer selectedCustomer;
    private String userRole;
    private static final int REQUEST_CODE_ADD_CUSTOMER = 101;

    // Logic kiểm soát thanh toán
    private Handler paymentCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable paymentCheckRunnable;
    private boolean isCheckingPayment = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_invoice);

        // Giữ nguyên phân quyền User
        userRole = getIntent().getStringExtra("ROLE");
        if (userRole == null) userRole = "staff";

        initViews();
        fetchData(); // Load cả Khách, Hàng và Shipper
        setupSearchLogic();
        setupPointCalculation();
        setupPaymentLogic();

        btnClearCustomer.setOnClickListener(v -> clearCustomer());
        btnAddNewCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(CreateInvoiceActivity.this, AddCustomerActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_CUSTOMER);
        });

        btnConfirmInvoice.setOnClickListener(v -> handleConfirmAction());
    }

    private void initViews() {
        svCustomer = findViewById(R.id.svCustomer);
        svProduct = findViewById(R.id.svProduct);
        rvCustomer = findViewById(R.id.rvCustomer);
        rvProduct = findViewById(R.id.rvProduct);
        rvSelectedProducts = findViewById(R.id.rvSelectedProducts);

        layoutCustomerInfo = findViewById(R.id.layoutCustomerInfo);
        etCusName = findViewById(R.id.etInvoiceCusName);
        etCusPhone = findViewById(R.id.etInvoiceCusPhone);
        etBaseAddress = findViewById(R.id.etInvoiceBaseAddress);
        etAddressDetail = findViewById(R.id.etInvoiceAddressDetail);
        etUsedPoints = findViewById(R.id.etUsedPoints);
        etTotalBillAuto = findViewById(R.id.etTotalBillAuto);

        tvPointWarning = findViewById(R.id.tvPointWarning);
        tvCusCreditLabel = findViewById(R.id.tvCusCreditLabel);
        btnClearCustomer = findViewById(R.id.btnClearCustomer);
        btnAddNewCustomer = findViewById(R.id.btnAddNewCustomer);
        btnConfirmInvoice = findViewById(R.id.btnConfirmInvoice);

        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        layoutCashPayment = findViewById(R.id.layoutCashPayment);
        layoutTransferPayment = findViewById(R.id.layoutTransferPayment);
        cbConfirmCash = findViewById(R.id.cbConfirmCash);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvQrInfo = findViewById(R.id.tvQrInfo);

        rvCustomer.setLayoutManager(new LinearLayoutManager(this));
        rvProduct.setLayoutManager(new LinearLayoutManager(this));
        rvSelectedProducts.setLayoutManager(new LinearLayoutManager(this));

        selectedAdapter = new SelectedProductAdapter(selectedProductsList, this::calculateTotal);
        rvSelectedProducts.setAdapter(selectedAdapter);
    }

    private void setupPaymentLogic() {
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCash) {
                paymentMethod = "Tiền mặt";
                layoutCashPayment.setVisibility(View.VISIBLE);
                layoutTransferPayment.setVisibility(View.GONE);

                // Trạng thái mặc định: Màu xanh
                btnConfirmInvoice.setText("XÁC NHẬN HÓA ĐƠN");
                btnConfirmInvoice.setBackgroundColor(Color.parseColor("#2E7D32"));
                btnConfirmInvoice.setEnabled(true);
                stopCheckingPayment();
            } else if (checkedId == R.id.rbTransfer) {
                paymentMethod = "Chuyển khoản";
                layoutCashPayment.setVisibility(View.GONE);
                layoutTransferPayment.setVisibility(View.VISIBLE);

                // Chế độ chuẩn bị: Chờ bấm tạo QR
                btnConfirmInvoice.setText("TẠO MÃ QR THANH TOÁN");
                btnConfirmInvoice.setBackgroundColor(Color.parseColor("#1976D2")); // Màu xanh dương
                btnConfirmInvoice.setEnabled(true);
            }
        });
    }

    private void handleConfirmAction() {
        if (selectedCustomer == null || selectedProductsList.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (paymentMethod.equals("Tiền mặt")) {
            if (!cbConfirmCash.isChecked()) {
                Toast.makeText(this, "Hãy xác nhận đã nhận tiền mặt!", Toast.LENGTH_SHORT).show();
                return;
            }
            saveInvoice(true); // Lưu và nhảy màn hình luôn
        } else {
            // Lưu PENDING để có dữ liệu cho Webhook, sau đó hiện QR và đổi sang MÀU CAM
            saveInvoice(false);
        }
    }

    private void saveInvoice(boolean shouldFinishImmediately) {
        SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
        int currentUserId = pref.getInt("id", -1);

        InvoiceRequest request = new InvoiceRequest();
        request.setCusID(selectedCustomer.getId());
        request.setUser_id(currentUserId);
        request.setTotalAmount(Double.parseDouble(etTotalBillAuto.getText().toString().replaceAll("[^0-9]", "")));
        request.setPaymentMethod(paymentMethod);
        request.setPaymentCode(currentPaymentCode);
        request.setAddressDetail(etAddressDetail.getText().toString());

        List<Integer> ids = new ArrayList<>();
        List<Integer> qtys = new ArrayList<>();
        for (Product p : selectedProductsList) {
            ids.add(p.getId());
            qtys.add(p.getQuantity());
        }
        request.setProductIds(ids);
        request.setQuantities(qtys);

        RetrofitClient.getApiService().createInvoice(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (shouldFinishImmediately) {
                        goToDetailScreen();
                    } else {
                        // HIỆN MÀU CAM "CHỜ THANH TOÁN" ĐÚNG Ý ÔNG
                        btnConfirmInvoice.setText("ĐANG CHỜ THANH TOÁN...");
                        btnConfirmInvoice.setBackgroundColor(Color.parseColor("#FF9800"));
                        btnConfirmInvoice.setEnabled(false);
                        generateVietQR();
                    }
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(CreateInvoiceActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateVietQR() {
        String totalRaw = etTotalBillAuto.getText().toString().replaceAll("[^0-9]", "");
        String bankId = "BIDV";
        String accountNo = "0355220770";
        String accountName = "DANG VAN HIEU";

        String qrUrl = "https://img.vietqr.io/image/" + bankId + "-" + accountNo + "-compact2.png" +
                "?amount=" + totalRaw +
                "&addInfo=" + currentPaymentCode +
                "&accountName=" + accountName.replace(" ", "%20");

        Glide.with(this).load(qrUrl).into(ivQrCode);
        tvQrInfo.setText("Nội dung CK: " + currentPaymentCode + "\n⏳ Đang chờ khách quét mã...");

        startCheckingPayment();
    }

    private void startCheckingPayment() {
        isCheckingPayment = true;
        paymentCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isCheckingPayment) return;

                RetrofitClient.getApiService().checkStatus(currentPaymentCode).enqueue(new Callback<Boolean>() {
                    @Override
                    public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                        // Dùng đúng hàm checkStatus để tránh lỗi 404
                        if (response.isSuccessful() && response.body() != null && response.body()) {
                            stopCheckingPayment();
                            Toast.makeText(CreateInvoiceActivity.this, "THANH TOÁN THÀNH CÔNG!", Toast.LENGTH_SHORT).show();
                            goToDetailScreen();
                        } else {
                            if (isCheckingPayment) paymentCheckHandler.postDelayed(paymentCheckRunnable, 3000);
                        }
                    }
                    @Override
                    public void onFailure(Call<Boolean> call, Throwable t) {
                        if (isCheckingPayment) paymentCheckHandler.postDelayed(paymentCheckRunnable, 5000);
                    }
                });
            }
        };
        paymentCheckHandler.post(paymentCheckRunnable);
    }

    private void goToDetailScreen() {
        Intent intent = new Intent(CreateInvoiceActivity.this, InvoiceDetailActivity.class);
        intent.putExtra("CUSTOMER", selectedCustomer);
        intent.putExtra("PRODUCT_LIST", (ArrayList<Product>) selectedProductsList);
        intent.putExtra("TOTAL_PAY", etTotalBillAuto.getText().toString());
        intent.putExtra("USED_POINTS", etUsedPoints.getText().toString());
        intent.putExtra("ADDRESS_DETAIL", etAddressDetail.getText().toString());
        startActivity(intent);
        finish();
    }

    private void stopCheckingPayment() {
        isCheckingPayment = false;
        if (paymentCheckRunnable != null) paymentCheckHandler.removeCallbacks(paymentCheckRunnable);
    }

    // === CÁC HÀM FETCH DATA (GIỮ NGUYÊN SHIPPER CỦA ÔNG) ===
    private void fetchData() {
        RetrofitClient.getApiService().getAllCustomers().enqueue(new Callback<List<Customer>>() {
            @Override
            public void onResponse(Call<List<Customer>> call, Response<List<Customer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCustomers = response.body();
                    customerAdapter = new CustomerAdapter(allCustomers, CreateInvoiceActivity.this, true, new CustomerAdapter.OnCustomerActionListener() {
                        @Override public void onCustomerClick(Customer customer) { selectCustomer(customer); }
                        @Override public void onDetailClick(Customer customer) {}
                        @Override public void onEditClick(Customer customer) {}
                        @Override public void onDeleteClick(Customer customer) {}
                    });
                    rvCustomer.setAdapter(customerAdapter);
                }
            }
            @Override public void onFailure(Call<List<Customer>> call, Throwable t) {}
        });

        RetrofitClient.getApiService().getAllProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allProducts = response.body();
                    productAdapter = new ProductAdapter(allProducts, CreateInvoiceActivity.this, userRole, product -> addProduct(product));
                    rvProduct.setAdapter(productAdapter);
                }
            }
            @Override public void onFailure(Call<List<Product>> call, Throwable t) {}
        });

        // GIỮ NGUYÊN LOGIC LOAD SHIPPER
        RetrofitClient.getApiService().getAllShippers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allShippers = response.body();
                    Log.d("SHIPPER", "Đã load " + allShippers.size() + " shipper");
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    // ... (Các hàm setupSearchLogic, calculateTotal, selectCustomer giữ nguyên) ...

    private void selectCustomer(Customer customer) {
        this.selectedCustomer = customer;
        layoutCustomerInfo.setVisibility(View.VISIBLE);
        rvCustomer.setVisibility(View.GONE);
        svCustomer.setQuery("", false);
        etCusName.setText(customer.getCusName());
        etCusPhone.setText(customer.getCusPhone());
        etBaseAddress.setText(customer.getAddress());
        tvCusCreditLabel.setText("Dùng điểm (Hiện có: " + customer.getCusCredit() + ")");
        String phoneSuffix = customer.getCusPhone().substring(Math.max(0, customer.getCusPhone().length() - 4));
        currentPaymentCode = "HDMART" + phoneSuffix + (System.currentTimeMillis() % 1000);
        calculateTotal();
    }

    private void calculateTotal() {
        double total = 0;
        for (Product p : selectedProductsList) total += (p.getPrice() * p.getQuantity());
        String pStr = etUsedPoints.getText().toString().trim();
        if (!pStr.isEmpty()) {
            try {
                int points = Integer.parseInt(pStr);
                if (selectedCustomer != null && points <= selectedCustomer.getCusCredit()) total -= points;
            } catch (Exception ignored) {}
        }
        if (total < 0) total = 0;
        etTotalBillAuto.setText(String.format("%,.0f", total) + " VNĐ");
    }

    private void addProduct(Product product) {
        if (product.getStock() <= 0) {
            Toast.makeText(this, "Hết hàng!", Toast.LENGTH_SHORT).show();
            return;
        }
        for (Product p : selectedProductsList) {
            if (p.getId() == product.getId()) {
                if (p.getQuantity() < product.getStock()) {
                    p.setQuantity(p.getQuantity() + 1);
                    selectedAdapter.notifyDataSetChanged();
                    calculateTotal();
                }
                return;
            }
        }
        Product n = new Product(); n.setId(product.getId()); n.setPro_name(product.getPro_name());
        n.setPrice(product.getPrice()); n.setStock(product.getStock()); n.setQuantity(1);
        selectedProductsList.add(n);
        selectedAdapter.notifyDataSetChanged();
        calculateTotal();
    }

    private void setupSearchLogic() {
        svCustomer.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                rvCustomer.setVisibility(newText.isEmpty() ? View.GONE : View.VISIBLE);
                if (customerAdapter != null) customerAdapter.filter(newText);
                return true;
            }
            @Override public boolean onQueryTextSubmit(String q) { return false; }
        });

        svProduct.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                rvProduct.setVisibility(newText.isEmpty() ? View.GONE : View.VISIBLE);
                if (productAdapter != null) productAdapter.filter(newText);
                return true;
            }
            @Override public boolean onQueryTextSubmit(String q) { return false; }
        });
    }

    private void setupPointCalculation() {
        etUsedPoints.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (selectedCustomer == null) return;
                String val = s.toString().trim();
                if (val.isEmpty()) tvPointWarning.setVisibility(View.GONE);
                else {
                    try {
                        int p = Integer.parseInt(val);
                        tvPointWarning.setVisibility(p > selectedCustomer.getCusCredit() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) { tvPointWarning.setVisibility(View.GONE); }
                }
                calculateTotal();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void clearCustomer() {
        selectedCustomer = null;
        layoutCustomerInfo.setVisibility(View.GONE);
        etUsedPoints.setText("");
        calculateTotal();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCheckingPayment();
    }
}