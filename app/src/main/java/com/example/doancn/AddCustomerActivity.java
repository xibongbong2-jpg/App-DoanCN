package com.example.doancn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Customer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddCustomerActivity extends AppCompatActivity {

    private EditText etName, etPhone;
    private Spinner spnProvince, spnDistrict, spnWard;
    private Button btnSave;
    private List<ProvinceData> provinceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_customer);

        initViews();
        loadJsonData();
        setupSpinners();

        btnSave.setOnClickListener(v -> saveCustomer());
    }

    private void initViews() {
        etName = findViewById(R.id.etAddCusName);
        etPhone = findViewById(R.id.etAddCusPhone);
        spnProvince = findViewById(R.id.spnProvince);
        spnDistrict = findViewById(R.id.spnDistrict);
        spnWard = findViewById(R.id.spnWard);
        btnSave = findViewById(R.id.btnSaveCustomer);
    }

    private void loadJsonData() {
        try {
            InputStream is = getAssets().open("address_data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            provinceList = new Gson().fromJson(json, new TypeToken<List<ProvinceData>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSpinners() {
        if (provinceList == null) return;

        ArrayAdapter<ProvinceData> pAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, provinceList);
        spnProvince.setAdapter(pAdapter);

        spnProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateDistrict(provinceList.get(position).districts);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateDistrict(List<DistrictData> districts) {
        ArrayAdapter<DistrictData> dAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, districts);
        spnDistrict.setAdapter(dAdapter);

        spnDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateWard(districts.get(position).wards);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateWard(List<String> wards) {
        ArrayAdapter<String> wAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, wards);
        spnWard.setAdapter(wAdapter);
    }

    private void saveCustomer() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ tên và SĐT", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gộp địa chỉ khung (Thôn, Xã, Tỉnh)
        String fullBaseAddress = spnWard.getSelectedItem().toString() + ", " +
                spnDistrict.getSelectedItem().toString() + ", " +
                spnProvince.getSelectedItem().toString();

        Customer c = new Customer();
        c.setCusName(name);
        c.setCusPhone(phone);
        c.setAddress(fullBaseAddress);
        c.setCusCredit(0); // Mặc định 0

        RetrofitClient.getApiService().addCustomer(c).enqueue(new Callback<Customer>() {
            @Override
            public void onResponse(Call<Customer> call, Response<Customer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Intent intent = new Intent();
                        // Đưa đối tượng khách hàng vừa tạo vào Intent
                        intent.putExtra("NEW_CUSTOMER", response.body());

                        setResult(RESULT_OK, intent); // Đánh dấu thành công
                        finish(); // Lệnh này sẽ đóng màn hình và quay về trang Hóa đơn

                    } catch (Exception e) {
                        Toast.makeText(AddCustomerActivity.this, "Lỗi gửi dữ liệu về trang Hóa đơn!", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(AddCustomerActivity.this, "Server trả về lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Customer> call, Throwable t) {
                Toast.makeText(AddCustomerActivity.this, "Lỗi mạng, không lưu được!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Model đơn giản để đọc JSON ---
    class ProvinceData {
        String name;
        List<DistrictData> districts;
        @Override public String toString() { return name; }
    }
    class DistrictData {
        String name;
        List<String> wards;
        @Override public String toString() { return name; }
    }
}