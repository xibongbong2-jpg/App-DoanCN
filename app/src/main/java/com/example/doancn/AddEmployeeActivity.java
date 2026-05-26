package com.example.doancn;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.User;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Arrays;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEmployeeActivity extends AppCompatActivity {
    private EditText edtUsername, edtPassword, edtFullName, edtDob;

    // FIX: Thêm Spinner mới cho phòng ban
    private Spinner spinnerRole, spinnerDepartment;

    private Button btnSave;
    private ImageView imgAvatar;
    private FrameLayout layoutAvatarPicker;

    private Uri selectedImageUri;
    private User userToEdit;
    private boolean isEditMode = false;

    // Danh sách bộ phận cố định
    private final String[] departments = {"IT", "GD", "TV", "DEL"};
    private final String[] roles = {"staff", "admin", "shipper"};

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).circleCrop().into(imgAvatar);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        initViews();
        checkMode();

        layoutAvatarPicker.setOnClickListener(v -> openGallery());
        imgAvatar.setOnClickListener(v -> openGallery());
        edtDob.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> startSaveProcess());
    }

    private void initViews() {
        edtUsername = findViewById(R.id.edtNewUsername);
        edtPassword = findViewById(R.id.edtNewPassword);
        edtFullName = findViewById(R.id.edtNewFullName);
        edtDob = findViewById(R.id.edtNewDob);

        spinnerRole = findViewById(R.id.spinnerRole);

        // FIX: Ánh xạ ID của Spinner Bộ phận (Đảm bảo trong XML ID là @+id/spinnerDepartment)
        spinnerDepartment = findViewById(R.id.spinnerDepartment);

        imgAvatar = findViewById(R.id.imgNewAvatar);
        layoutAvatarPicker = findViewById(R.id.layoutAvatarPicker);
        btnSave = findViewById(R.id.btnSaveEmployee);

        // Setup Adapter cho Vai trò
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        // FIX: Setup Adapter cho Bộ phận
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, departments);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(deptAdapter);
    }

    private void checkMode() {
        userToEdit = (User) getIntent().getSerializableExtra("EDIT_USER_DATA");
        if (userToEdit != null) {
            isEditMode = true;
            btnSave.setText("Cập nhật thông tin");
            edtUsername.setText(userToEdit.getUsername());
            edtUsername.setEnabled(false);
            edtPassword.setText(userToEdit.getPassword());
            edtFullName.setText(userToEdit.getFull_name());
            edtDob.setText(userToEdit.getDob());

            // Set giá trị mặc định cho Spinner Vai Trò
            int roleIndex = Arrays.asList(roles).indexOf(userToEdit.getRole());
            if (roleIndex >= 0) spinnerRole.setSelection(roleIndex);

            // FIX: Set giá trị mặc định cho Spinner Bộ Phận dựa trên dữ liệu cũ
            int deptIndex = Arrays.asList(departments).indexOf(userToEdit.getDepartment());
            if (deptIndex >= 0) spinnerDepartment.setSelection(deptIndex);

            String oldImg = RetrofitClient.BASE_URL + "uploads/" + userToEdit.getUser_image();
            Glide.with(this).load(oldImg).circleCrop().placeholder(R.drawable.ic_add_pp).into(imgAvatar);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void startSaveProcess() {
        if (isEditMode) performUpdate();
        else performAdd();
    }

    private void performAdd() {
        if (selectedImageUri != null) uploadAvatarThenSave();
        else saveToDb("default_avatar.png");
    }

    private void uploadAvatarThenSave() {
        try {
            File file = getFileFromUri(selectedImageUri);
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), reqFile);

            RetrofitClient.getApiService().uploadAvatar(body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful()) saveToDb(response.body().string());
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveToDb(String fileName) {
        User newUser = new User();
        newUser.setUsername(edtUsername.getText().toString().trim());
        newUser.setPassword(edtPassword.getText().toString().trim());
        newUser.setFull_name(edtFullName.getText().toString().trim());
        newUser.setRole(spinnerRole.getSelectedItem().toString());

        // Lấy dữ liệu từ Spinner thay vì EditText
        newUser.setDepartment(spinnerDepartment.getSelectedItem().toString());

        newUser.setDob(edtDob.getText().toString().trim());
        newUser.setUser_image(fileName);

        RetrofitClient.getApiService().addEmployee(newUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddEmployeeActivity.this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    // Xử lý khi Backend báo lỗi (ví dụ: HTTP 400, 409 do trùng Username)
                    String errorMessage = "Thêm thất bại! Tên đăng nhập có thể đã tồn tại.";
                    try {
                        if (response.errorBody() != null) {
                            // Nếu Backend Spring Boot của ông có trả về chuỗi báo lỗi cụ thể, có thể mở comment dòng dưới
                            // errorMessage = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Hiển thị Toast thông báo lỗi cho người dùng
                    Toast.makeText(AddEmployeeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Xử lý khi không gọi được API (mất mạng, server XAMPP chưa bật...)
                Toast.makeText(AddEmployeeActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performUpdate() {
        userToEdit.setFull_name(edtFullName.getText().toString().trim());

        // FIX: Lấy dữ liệu từ Spinner thay vì EditText
        userToEdit.setDepartment(spinnerDepartment.getSelectedItem().toString());

        userToEdit.setDob(edtDob.getText().toString().trim());
        userToEdit.setRole(spinnerRole.getSelectedItem().toString());
        userToEdit.setPassword(edtPassword.getText().toString().trim());

        RequestBody userPart = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(userToEdit));
        MultipartBody.Part imagePart = null;

        if (selectedImageUri != null) {
            try {
                File file = getFileFromUri(selectedImageUri);
                RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), file);
                imagePart = MultipartBody.Part.createFormData("image", file.getName(), fileBody);
            } catch (Exception e) { e.printStackTrace(); }
        }

        RetrofitClient.getApiService().updateEmployeeWithImage(userToEdit.getId(), userPart, imagePart)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AddEmployeeActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (v, y, m, d) -> edtDob.setText(y + "-" + (m + 1) + "-" + d),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private File getFileFromUri(Uri uri) throws Exception {
        File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream os = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
        }
        return tempFile;
    }
}