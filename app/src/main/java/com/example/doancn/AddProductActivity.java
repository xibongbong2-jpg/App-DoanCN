package com.example.doancn;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Product;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProductActivity extends AppCompatActivity {

    private EditText edtName, edtCode, edtStock, edtPrice;
    private Spinner spinnerCategory;
    private Button btnSave;
    private ImageView imgPreview;
    private FrameLayout layoutPicker;
    private ProgressDialog progressDialog;

    private Uri selectedImageUri;
    private String[] categories = {"IT", "AV", "GD"};

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(imgPreview);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        initViews();

        layoutPicker.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> handleSaveProduct());
    }

    private void initViews() {
        edtName = findViewById(R.id.edtProductName);
        edtCode = findViewById(R.id.edtProductCode);
        edtStock = findViewById(R.id.edtProductStock);
        edtPrice = findViewById(R.id.edtProductPrice);
        spinnerCategory = findViewById(R.id.spinnerAddCategory);
        imgPreview = findViewById(R.id.imgProductPreview);
        layoutPicker = findViewById(R.id.layoutProductImagePicker);
        btnSave = findViewById(R.id.btnSaveProduct);

        // Đảm bảo chữ trên nút là "THÊM MỚI"
        btnSave.setText("THÊM MỚI SẢN PHẨM");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang lưu sản phẩm mới...");
        progressDialog.setCancelable(false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void handleSaveProduct() {
        String name = edtName.getText().toString().trim();
        String code = edtCode.getText().toString().trim();
        String stock = edtStock.getText().toString().trim();
        String price = edtPrice.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        // VALIDATE: Thêm mới bắt buộc phải chọn ảnh
        if (name.isEmpty() || code.isEmpty() || stock.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh cho sản phẩm mới!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressDialog.show();

        try {
            // Đóng gói dữ liệu dạng RequestBody
            RequestBody rbName = RequestBody.create(MediaType.parse("text/plain"), name);
            RequestBody rbCode = RequestBody.create(MediaType.parse("text/plain"), code);
            RequestBody rbCate = RequestBody.create(MediaType.parse("text/plain"), category);
            RequestBody rbStock = RequestBody.create(MediaType.parse("text/plain"), stock);
            RequestBody rbPrice = RequestBody.create(MediaType.parse("text/plain"), price);

            // Xử lý File ảnh
            File file = getFileFromUri(selectedImageUri);
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part bodyImg = MultipartBody.Part.createFormData("image", file.getName(), reqFile);

            // Gọi API POST để thêm mới
            RetrofitClient.getApiService().addProduct(rbName, rbCode, rbCate, rbStock, rbPrice, bodyImg)
                    .enqueue(new Callback<Product>() {
                        @Override
                        public void onResponse(Call<Product> call, Response<Product> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                Toast.makeText(AddProductActivity.this, "Thêm sản phẩm thành công!", Toast.LENGTH_SHORT).show();

                                // Báo về cho MainActivity biết để gọi lại hàm loadProductList()
                                setResult(Activity.RESULT_OK);
                                finish();
                            } else {
                                btnSave.setEnabled(true);
                                Toast.makeText(AddProductActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Product> call, Throwable t) {
                            progressDialog.dismiss();
                            btnSave.setEnabled(true);
                            Log.e("API_ERROR", t.getMessage());
                            Toast.makeText(AddProductActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            progressDialog.dismiss();
            btnSave.setEnabled(true);
            Log.e("FILE_ERROR", e.getMessage());
            Toast.makeText(this, "Lỗi xử lý file ảnh!", Toast.LENGTH_SHORT).show();
        }
    }

    private File getFileFromUri(Uri uri) throws Exception {
        // Tạo file tạm trong bộ nhớ Cache của App
        File tempFile = new File(getCacheDir(), "pro_add_" + System.currentTimeMillis() + ".jpg");
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream os = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
        }
        return tempFile;
    }
}