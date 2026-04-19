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
import java.util.Arrays;

// --- ĐỐNG IMPORT NÀY LÀ ĐỂ HẾT ĐỎ ---
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateProductActivity extends AppCompatActivity {

    private EditText edtName, edtCode, edtStock, edtPrice;
    private Spinner spinnerCategory;
    private Button btnUpdate;
    private ImageView imgPreview;
    private FrameLayout layoutPicker;
    private ProgressDialog progressDialog;

    private Uri selectedImageUri;
    private String[] categories = {"IT", "TV", "GD"};
    private Product currentProduct;
    private boolean isImageChanged = false;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    isImageChanged = true;
                    Glide.with(this).load(selectedImageUri).into(imgPreview);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        initViews();
        getDataFromIntent();

        layoutPicker.setOnClickListener(v -> openGallery());
        btnUpdate.setOnClickListener(v -> handleUpdateProduct());
    }

    private void initViews() {
        TextView tvTitle = findViewById(R.id.tvTitleProduct);
        if (tvTitle != null) {
            tvTitle.setText("CẬP NHÂT SẢN PHẨM");
        }
        edtName = findViewById(R.id.edtProductName);
        edtCode = findViewById(R.id.edtProductCode);
        edtStock = findViewById(R.id.edtProductStock);
        edtPrice = findViewById(R.id.edtProductPrice);
        spinnerCategory = findViewById(R.id.spinnerAddCategory);
        imgPreview = findViewById(R.id.imgProductPreview);
        layoutPicker = findViewById(R.id.layoutProductImagePicker);
        btnUpdate = findViewById(R.id.btnSaveProduct);
        btnUpdate.setText("CẬP NHẬT SẢN PHẨM");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang cập nhật...");
        progressDialog.setCancelable(false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void getDataFromIntent() {
        // Lấy dữ liệu sản phẩm
        currentProduct = (Product) getIntent().getSerializableExtra("PRODUCT_DATA");

        if (currentProduct != null) {
            // SỬA LẠI ĐÚNG TÊN GETTER CỦA ÔNG (Có dấu gạch dưới _)
            edtName.setText(currentProduct.getPro_name());
            edtCode.setText(currentProduct.getPro_code());
            edtStock.setText(String.valueOf(currentProduct.getStock()));
            edtPrice.setText(String.valueOf(currentProduct.getPrice()));

            int index = Arrays.asList(categories).indexOf(currentProduct.getCategory());
            if (index >= 0) spinnerCategory.setSelection(index);

            String imgUrl = RetrofitClient.BASE_URL + "uploads/" + currentProduct.getPro_image();
            Glide.with(this).load(imgUrl).placeholder(R.drawable.ic_product).into(imgPreview);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void handleUpdateProduct() {
        String name = edtName.getText().toString().trim();
        String code = edtCode.getText().toString().trim();
        String stock = edtStock.getText().toString().trim();
        String price = edtPrice.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (name.isEmpty() || code.isEmpty() || stock.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpdate.setEnabled(false);
        progressDialog.show();

        try {
            RequestBody rbName = RequestBody.create(MediaType.parse("text/plain"), name);
            RequestBody rbCode = RequestBody.create(MediaType.parse("text/plain"), code);
            RequestBody rbCate = RequestBody.create(MediaType.parse("text/plain"), category);
            RequestBody rbStock = RequestBody.create(MediaType.parse("text/plain"), stock);
            RequestBody rbPrice = RequestBody.create(MediaType.parse("text/plain"), price);

            MultipartBody.Part bodyImg = null;
            if (isImageChanged && selectedImageUri != null) {
                File file = getFileFromUri(selectedImageUri);
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
                bodyImg = MultipartBody.Part.createFormData("image", file.getName(), reqFile);
            }

            // Gọi API Update
            RetrofitClient.getApiService().updateProduct(currentProduct.getId(),
                            rbName, rbCode, rbCate, rbStock, rbPrice, bodyImg)
                    .enqueue(new Callback<Product>() {
                        @Override
                        public void onResponse(Call<Product> call, Response<Product> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                Toast.makeText(UpdateProductActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                setResult(Activity.RESULT_OK);
                                finish();
                            } else {
                                btnUpdate.setEnabled(true);
                                Toast.makeText(UpdateProductActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Product> call, Throwable t) {
                            progressDialog.dismiss();
                            btnUpdate.setEnabled(true);
                            Log.e("API_ERROR", t.getMessage());
                        }
                    });

        } catch (Exception e) {
            progressDialog.dismiss();
            btnUpdate.setEnabled(true);
        }
    }

    private File getFileFromUri(Uri uri) throws Exception {
        File tempFile = new File(getCacheDir(), "temp_up_" + System.currentTimeMillis() + ".jpg");
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream os = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
        }
        return tempFile;
    }
}