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

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEmployeeActivity extends AppCompatActivity {
    private EditText edtUsername, edtPassword, edtFullName, edtDept, edtDob;
    private Spinner spinnerRole;
    private Button btnSave;
    private ImageView imgAvatar;
    private FrameLayout layoutAvatarPicker;

    private Uri selectedImageUri;
    private User userToEdit;
    private boolean isEditMode = false;

    // Bộ chọn ảnh chuẩn Android hiện đại
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

        // Gộp các sự kiện click
        layoutAvatarPicker.setOnClickListener(v -> openGallery());
        imgAvatar.setOnClickListener(v -> openGallery());
        edtDob.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> startSaveProcess());
    }

    private void initViews() {
        edtUsername = findViewById(R.id.edtNewUsername);
        edtPassword = findViewById(R.id.edtNewPassword);
        edtFullName = findViewById(R.id.edtNewFullName);
        edtDept = findViewById(R.id.edtNewDepartment);
        edtDob = findViewById(R.id.edtNewDob);
        spinnerRole = findViewById(R.id.spinnerRole);
        imgAvatar = findViewById(R.id.imgNewAvatar);
        layoutAvatarPicker = findViewById(R.id.layoutAvatarPicker);
        btnSave = findViewById(R.id.btnSaveEmployee);

        String[] roles = {"staff", "admin","shipper"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void checkMode() {
        userToEdit = (User) getIntent().getSerializableExtra("EDIT_USER_DATA");
        if (userToEdit != null) {
            isEditMode = true;
            btnSave.setText("Cập nhật thông tin");
            edtUsername.setText(userToEdit.getUsername());
            edtUsername.setEnabled(false); // Không cho sửa Username
            edtPassword.setText(userToEdit.getPassword());
            edtFullName.setText(userToEdit.getFull_name());
            edtDept.setText(userToEdit.getDepartment());
            edtDob.setText(userToEdit.getDob());
            spinnerRole.setSelection(userToEdit.getRole().equals("admin") ? 1 : 0);

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

    // --- XỬ LÝ THÊM MỚI ---
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
        newUser.setDepartment(edtDept.getText().toString().trim());
        newUser.setDob(edtDob.getText().toString().trim());
        newUser.setUser_image(fileName);

        RetrofitClient.getApiService().addEmployee(newUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddEmployeeActivity.this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    // --- XỬ LÝ CẬP NHẬT ---
    private void performUpdate() {
        userToEdit.setFull_name(edtFullName.getText().toString().trim());
        userToEdit.setDepartment(edtDept.getText().toString().trim());
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