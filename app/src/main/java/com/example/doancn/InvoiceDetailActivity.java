package com.example.doancn;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doancn.ShipperAdapter;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Customer;
import com.example.doancn.model.Invoice;
import com.example.doancn.model.Product;
import com.example.doancn.model.User;

// --- CÁC IMPORT THƯ VIỆN MÁY IN BLUETOOTH ---
import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceDetailActivity extends AppCompatActivity {
    private LinearLayout layoutShipperActions, layoutSuccessFail, layoutProof;
    private Button btnDeliverySuccess, btnDeliveryFail, btnPickImage, btnSubmitProof, btnAssignShipper, btnPrintInvoice;
    private ImageView ivProofImage;
    private android.net.Uri selectedImageUri;

    private TextView tvDate, tvName, tvPhone, tvAddress, tvPoints, tvTotal;
    private LinearLayout containerProductItems;

    private final DecimalFormat formatter = new DecimalFormat("#,###,###,##0");
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // --- VIEW MỚI CHO LÝ DO THẤT BẠI ---
    private Button btnXemLyDo;
    private View layoutItemLyDo;
    private TextView tvHistoryOrderId, tvHistoryCusId, tvHistoryStaffId, tvHistoryStatus;
    private ImageView ivHistoryProof;

    private String currentStatus = "Giao hàng thành công";
    private String currentReason = "";

    // --- CÁC BIẾN LƯU TRỮ DỮ LIỆU ĐỂ TRUYỀN VÀO MÁY IN ---
    private String printCusName = "Khách lẻ";
    private String printCusPhone = "N/A";
    private String printAddress = "Tại cửa hàng";
    private List<Product> printProductList = new ArrayList<>();

    private final String[] failureReasons = {
            "Khách không nghe máy (gọi 3 lần)",
            "Khách hẹn giao vào ngày khác",
            "Sai địa chỉ / Không tìm thấy khách",
            "Khách từ chối nhận (Hàng lỗi/vỡ)",
            "Khách không đủ tiền thanh toán"
    };

    private List<User> originalShipperList = new ArrayList<>();
    private List<User> filteredShipperList = new ArrayList<>();
    private ShipperAdapter shipperAdapter;
    private int currentPage = 0;
    private final int ITEMS_PER_PAGE = 15;

    private final androidx.activity.result.ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            ivProofImage.setImageURI(selectedImageUri);
                            ivProofImage.setVisibility(View.VISIBLE);
                            btnSubmitProof.setVisibility(View.VISIBLE);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_detail);

        initViews();

        Invoice historyInvoice = (Invoice) getIntent().getSerializableExtra("INVOICE_DATA");
        if (historyInvoice != null) {
            setupFromHistory(historyInvoice);
        } else {
            setupFromDirectCreation();
        }

        findViewById(R.id.btnBackToHome).setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvDate = findViewById(R.id.tvInvoiceDate);
        tvName = findViewById(R.id.tvDetailCusName);
        tvPhone = findViewById(R.id.tvDetailCusPhone);
        tvAddress = findViewById(R.id.tvDetailFullAddress);
        tvPoints = findViewById(R.id.tvDetailUsedPoints);
        tvTotal = findViewById(R.id.tvDetailTotalPay);
        containerProductItems = findViewById(R.id.containerProductItems);
        btnAssignShipper = findViewById(R.id.btnAssignShipper);
        layoutShipperActions = findViewById(R.id.layoutShipperActions);
        layoutSuccessFail = findViewById(R.id.layoutSuccessFail);
        layoutProof = findViewById(R.id.layoutProof);

        btnDeliverySuccess = findViewById(R.id.btnDeliverySuccess);
        btnDeliveryFail = findViewById(R.id.btnDeliveryFail);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnSubmitProof = findViewById(R.id.btnSubmitProof);
        ivProofImage = findViewById(R.id.ivProofImage);

        btnXemLyDo = findViewById(R.id.btnXemLyDo);
        layoutItemLyDo = findViewById(R.id.layoutItemLyDo);
        tvHistoryOrderId = findViewById(R.id.tvHistoryOrderId);
        tvHistoryCusId = findViewById(R.id.tvHistoryCusId);
        tvHistoryStaffId = findViewById(R.id.tvHistoryStaffId);
        tvHistoryStatus = findViewById(R.id.tvHistoryStatus);
        ivHistoryProof = findViewById(R.id.ivHistoryProof);

        // Ánh xạ Nút In hóa đơn
        btnPrintInvoice = findViewById(R.id.btnPrintInvoice);
        if (btnPrintInvoice != null) {
            btnPrintInvoice.setOnClickListener(v -> checkBluetoothPermissionsAndPrint());
        }
    }

    private void setupFromHistory(Invoice inv) {
        tvDate.setText("Ngày: " + formatDateTime(inv.getCreatedAt()));
        tvPoints.setText("-" + formatter.format(inv.getUsedPoints()) + "đ");
        tvTotal.setText("TỔNG THANH TOÁN: " + formatter.format(inv.getTotalAmount()) + " VNĐ");

        fetchCustomerDetails(inv.getCustomerId(), inv.getAddressDetail());

        SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
        String currentUserRole = pref.getString("role", "");

        if ("Giao hàng thất bại".equalsIgnoreCase(inv.getStatus())) {
            btnXemLyDo.setVisibility(View.VISIBLE);

            tvHistoryOrderId.setText("Đơn hàng: #" + inv.getId());
            tvHistoryCusId.setText("Mã khách hàng: " + inv.getCustomerId());
            tvHistoryStaffId.setText("Mã nhân viên giao: " + inv.getShipperId());

            String reason = (inv.getReason() != null) ? inv.getReason() : "Không xác định";
            tvHistoryStatus.setText("Trạng thái: Giao hàng thất bại (" + reason + ")");

            String imgUrl = inv.getImageProof();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                String fullUrl = RetrofitClient.BASE_URL + "uploads/" + imgUrl;
                Glide.with(this).load(fullUrl).placeholder(R.drawable.ic_product).into(ivHistoryProof);
            }

            btnXemLyDo.setOnClickListener(v -> {
                if (layoutItemLyDo.getVisibility() == View.GONE) {
                    layoutItemLyDo.setVisibility(View.VISIBLE);
                    btnXemLyDo.setText("Ẩn bằng chứng");
                } else {
                    layoutItemLyDo.setVisibility(View.GONE);
                    btnXemLyDo.setText("Xem lý do");
                }
            });
        } else {
            btnXemLyDo.setVisibility(View.GONE);
            layoutItemLyDo.setVisibility(View.GONE);
        }

        if ("Chờ xác nhận".equalsIgnoreCase(inv.getStatus()) && "admin".equalsIgnoreCase(currentUserRole)) {
            btnAssignShipper.setVisibility(View.VISIBLE);
            btnAssignShipper.setOnClickListener(v -> showShipperSelectionDialog(inv.getId()));
        } else {
            btnAssignShipper.setVisibility(View.GONE);
        }

        if ("Đang giao hàng".equalsIgnoreCase(inv.getStatus()) && "shipper".equalsIgnoreCase(currentUserRole)) {
            layoutShipperActions.setVisibility(View.VISIBLE);

            btnDeliverySuccess.setOnClickListener(v -> {
                currentStatus = "Giao hàng thành công";
                currentReason = "Giao hàng thành công";
                layoutSuccessFail.setVisibility(View.GONE);
                layoutProof.setVisibility(View.VISIBLE);
            });

            btnDeliveryFail.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Chọn lý do thất bại")
                        .setItems(failureReasons, (dialog, which) -> {
                            currentStatus = "Giao hàng thất bại";
                            currentReason = failureReasons[which];
                            layoutSuccessFail.setVisibility(View.GONE);
                            layoutProof.setVisibility(View.VISIBLE);
                            Toast.makeText(this, "Vui lòng chụp ảnh xác nhận trả hàng về kho", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            });

            btnPickImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            });

            btnSubmitProof.setOnClickListener(v -> {
                if (selectedImageUri == null) {
                    Toast.makeText(this, "Vui lòng chọn ảnh xác minh!", Toast.LENGTH_SHORT).show();
                    return;
                }

                File file = getFileFromUri(selectedImageUri);
                if (file == null) return;

                RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
                MultipartBody.Part bodyImg = MultipartBody.Part.createFormData("image", file.getName(), reqFile);
                RequestBody statusBody = RequestBody.create(MediaType.parse("text/plain"), currentStatus);
                RequestBody reasonBody = RequestBody.create(MediaType.parse("text/plain"), currentReason);

                Toast.makeText(this, "Đang xử lý dữ liệu...", Toast.LENGTH_SHORT).show();

                RetrofitClient.getApiService().confirmDelivery(inv.getId(), bodyImg, statusBody, reasonBody)
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                Toast.makeText(InvoiceDetailActivity.this, "Cập nhật đơn hàng thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                finish();
                            }
                        });
            });
        }
        loadItemsFromServer(inv.getId());
    }

    private void fetchCustomerDetails(int customerId, String addressDetailFromInvoice) {
        RetrofitClient.getApiService().getCustomerById(customerId).enqueue(new Callback<Customer>() {
            @Override
            public void onResponse(Call<Customer> call, Response<Customer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Customer cus = response.body();
                    tvName.setText("Khách hàng: " + cus.getCusName() + " (ID: " + cus.getId() + ")");
                    tvPhone.setText("SĐT: " + cus.getCusPhone());
                    String fullAddress = (addressDetailFromInvoice != null ? addressDetailFromInvoice.trim() : "...") + ", " + cus.getAddress();
                    tvAddress.setText("Địa chỉ giao: " + fullAddress);

                    // Lưu dữ liệu để in
                    printCusName = cus.getCusName();
                    printCusPhone = cus.getCusPhone();
                    printAddress = fullAddress;
                }
            }
            @Override public void onFailure(Call<Customer> call, Throwable t) {}
        });
    }

    private void setupFromDirectCreation() {
        Customer cus = (Customer) getIntent().getSerializableExtra("CUSTOMER");
        ArrayList<Product> products = (ArrayList<Product>) getIntent().getSerializableExtra("PRODUCT_LIST");

        String fullAddress = getIntent().getStringExtra("ADDRESS_DETAIL");
        String rawTotal = getIntent().getStringExtra("RAW_TOTAL");
        String totalPay = getIntent().getStringExtra("TOTAL_PAY");
        String usedPoints = getIntent().getStringExtra("USED_POINTS");

        if (cus != null) {
            tvName.setText("Khách hàng: " + cus.getCusName());
            tvPhone.setText("SĐT: " + cus.getCusPhone());

            printCusName = cus.getCusName();
            printCusPhone = cus.getCusPhone();

            if (fullAddress != null && !fullAddress.isEmpty()) {
                tvAddress.setText("Địa chỉ: " + fullAddress);
                printAddress = fullAddress;
            } else {
                tvAddress.setText("Địa chỉ: " + cus.getAddress());
                printAddress = cus.getAddress();
            }
        }

        if (usedPoints != null && !usedPoints.isEmpty()) {
            try {
                tvPoints.setText("-" + formatter.format(Integer.parseInt(usedPoints)) + "đ");
            } catch (Exception e) {
                tvPoints.setText("0đ");
            }
        } else {
            tvPoints.setText("0đ");
        }

        if (rawTotal != null && !rawTotal.isEmpty()) {
            try {
                double total = Double.parseDouble(rawTotal);
                tvTotal.setText("TỔNG THANH TOÁN: " + formatter.format(total) + "đ");
            } catch (Exception e) {
                tvTotal.setText("TỔNG THANH TOÁN: " + totalPay);
            }
        } else if (totalPay != null) {
            tvTotal.setText("TỔNG THANH TOÁN: " + totalPay);
        }

        tvDate.setText("Ngày: " + outputFormat.format(new java.util.Date()));

        if (products != null) {
            renderProductList(products);
            printProductList.clear();
            printProductList.addAll(products);
        }
    }

    private void renderProductList(List<Product> products) {
        containerProductItems.removeAllViews();
        for (Product p : products) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(createProductCell(p.getPro_name(), 3f, Gravity.START));
            row.addView(createProductCell(String.valueOf(p.getQuantity()), 1f, Gravity.CENTER));
            row.addView(createProductCell(p.getPro_code() != null ? p.getPro_code() : "N/A", 2f, Gravity.CENTER));
            row.addView(createProductCell(formatter.format(p.getPrice()), 2.5f, Gravity.END));
            containerProductItems.addView(row);
        }
    }

    private TextView createProductCell(String text, float weight, int gravity) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        textView.setText(text);
        textView.setGravity(gravity);
        textView.setTextColor(ContextCompat.getColor(this, R.color.black));
        return textView;
    }

    private void loadItemsFromServer(int invoiceId) {
        RetrofitClient.getApiService().getInvoiceItems(invoiceId).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderProductList(response.body());
                    // Lưu dữ liệu để in
                    printProductList.clear();
                    printProductList.addAll(response.body());
                }
            }
            @Override public void onFailure(Call<List<Product>> call, Throwable t) {}
        });
    }

    private String formatDateTime(String rawDate) {
        try { return outputFormat.format(inputFormat.parse(rawDate)); } catch (Exception e) { return rawDate; }
    }

    // ====================================================================
    // CÁC HÀM XỬ LÝ MÁY IN BLUETOOTH
    // ====================================================================

    private void checkBluetoothPermissionsAndPrint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 100);
                return;
            }
        }
        printBluetooth();
    }

    private void printBluetooth() {
        try {
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();

            if (connection != null) {
                // Truyền bảng mã Tiếng Việt vào máy in (Số 30 là mã phổ biến nhất)
                EscPosCharsetEncoding charset = new EscPosCharsetEncoding("windows-1258", 30);
                EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32, charset);

                StringBuilder printerData = new StringBuilder();
                printerData.append("[C]<b><font size='big'>HÓA ĐƠN BÁN HÀNG</font></b>\n");

                String dateStr = tvDate.getText().toString().replace("Ngày: ", "");
                printerData.append("[L]Ngày: ").append(dateStr).append("\n");
                printerData.append("[C]--------------------------------\n");

                String safeName = printCusName != null && printCusName.length() > 20 ? printCusName.substring(0, 20) : printCusName;
                printerData.append("[L]<b>Khách:</b> ").append(safeName).append("\n");
                printerData.append("[L]<b>SĐT:</b> ").append(printCusPhone).append("\n");

                String safeAddress = printAddress != null && printAddress.length() > 22 ? printAddress.substring(0, 22) + ".." : printAddress;
                printerData.append("[L]<b>Đ/C:</b> ").append(safeAddress).append("\n");

                printerData.append("[C]--------------------------------\n");

                // Tiêu đề cột
                printerData.append("[L]<b>Tên hàng</b>[R]<b>SL</b>[R]<b>Giá</b>\n");

                // Danh sách sản phẩm (Ép độ dài tên hàng không quá 15 ký tự)
                for (Product p : printProductList) {
                    String pName = p.getPro_name();
                    if (pName != null && pName.length() > 15) {
                        pName = pName.substring(0, 15) + "..";
                    }

                    printerData.append("[L]").append(pName).append("\n");
                    String code = p.getPro_code() != null ? p.getPro_code() : "N/A";
                    printerData.append("[L]  Mã: ").append(code)
                            .append("[R]").append(p.getQuantity()).append("x")
                            .append("[R]").append(formatter.format(p.getPrice())).append("\n");
                }

                printerData.append("[C]--------------------------------\n");
                printerData.append("[L]Điểm dùng:[R]").append(tvPoints.getText().toString()).append("\n");
                printerData.append("[L]<b>TỔNG TIỀN:</b>[R]<b>").append(tvTotal.getText().toString().replace("TỔNG THANH TOÁN: ", "")).append("</b>\n");
                printerData.append("[C]--------------------------------\n");
                printerData.append("[C]<i>Cảm ơn quý khách. Hẹn gặp lại!</i>\n");
                printerData.append("[L]\n[L]\n");

                // In trực tiếp chuỗi Tiếng Việt đã định dạng
                printer.printFormattedText(printerData.toString());
                Toast.makeText(this, "Đang in hóa đơn...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không tìm thấy máy in Bluetooth nào đã ghép đôi!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi in: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ====================================================================

    private void showShipperSelectionDialog(int invoiceId) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_assign_shipper);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 1600);

        EditText etSearch = dialog.findViewById(R.id.etSearchShipper);
        RecyclerView rvList = dialog.findViewById(R.id.rvShipperList);
        Button btnPrev = dialog.findViewById(R.id.btnPrevPage);
        Button btnNext = dialog.findViewById(R.id.btnNextPage);
        TextView tvPageNum = dialog.findViewById(R.id.tvPageNumber);
        Button btnClose = dialog.findViewById(R.id.btnCloseDialog);

        rvList.setLayoutManager(new LinearLayoutManager(this));
        shipperAdapter = new ShipperAdapter(new ArrayList<>(), selectedShipper -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận gán đơn")
                    .setMessage("Bạn có chắc chắn muốn gán đơn hàng này cho shipper: " + selectedShipper.getFull_name() + "?")
                    .setPositiveButton("Xác nhận", (confirmDialog, which) -> {
                        assignShipperToInvoice(invoiceId, selectedShipper.getId());
                        dialog.dismiss();
                    })
                    .setNegativeButton("Hủy", (confirmDialog, which) -> {
                        confirmDialog.dismiss();
                    })
                    .setCancelable(false)
                    .show();
        });
        rvList.setAdapter(shipperAdapter);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        RetrofitClient.getApiService().getAllShippers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    originalShipperList.clear();
                    originalShipperList.addAll(response.body());
                    filteredShipperList.clear();
                    filteredShipperList.addAll(originalShipperList);
                    currentPage = 0;
                    updateDialogPage(btnPrev, btnNext, tvPageNum);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().toLowerCase().trim();
                filteredShipperList.clear();
                for (User shipper : originalShipperList) {
                    String idStr = String.valueOf(shipper.getId());
                    String nameStr = shipper.getFull_name() != null ? shipper.getFull_name().toLowerCase() : "";
                    if (idStr.contains(keyword) || nameStr.contains(keyword)) {
                        filteredShipperList.add(shipper);
                    }
                }
                currentPage = 0;
                updateDialogPage(btnPrev, btnNext, tvPageNum);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                updateDialogPage(btnPrev, btnNext, tvPageNum);
            }
        });

        btnNext.setOnClickListener(v -> {
            int totalPages = (int) Math.ceil((double) filteredShipperList.size() / ITEMS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateDialogPage(btnPrev, btnNext, tvPageNum);
            }
        });

        dialog.show();
    }

    private void updateDialogPage(Button btnPrev, Button btnNext, TextView tvPageNum) {
        int totalPages = (int) Math.ceil((double) filteredShipperList.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        int startItem = currentPage * ITEMS_PER_PAGE;
        int endItem = Math.min(startItem + ITEMS_PER_PAGE, filteredShipperList.size());

        List<User> pageData = new ArrayList<>();
        if (startItem < filteredShipperList.size()) {
            pageData = filteredShipperList.subList(startItem, endItem);
        }

        shipperAdapter.updateData(pageData);
        tvPageNum.setText((currentPage + 1) + " / " + totalPages);

        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < totalPages - 1);
    }

    private void assignShipperToInvoice(int invoiceId, int shipperId) {
        RetrofitClient.getApiService().assignShipper(invoiceId, shipperId).enqueue(new Callback<String>() {
            @Override public void onResponse(Call<String> call, Response<String> response) { finish(); }
            @Override public void onFailure(Call<String> call, Throwable t) { finish(); }
        });
    }

    private File getFileFromUri(android.net.Uri uri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("proof_", ".jpg", getCacheDir());
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, len);
            outputStream.close(); inputStream.close();
            return tempFile;
        } catch (Exception e) { return null; }
    }
}