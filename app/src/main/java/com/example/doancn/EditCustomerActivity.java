package com.example.doancn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Customer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditCustomerActivity extends AppCompatActivity {

    private EditText etName, etPhone;
    private Spinner spnProvince, spnDistrict, spnWard;
    private Button btnUpdate;
    private List<ProvinceData> provinceList;
    private Customer currentCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dùng chung layout với trang Add
        setContentView(R.layout.activity_add_customer);

        initViews();
        loadJsonData();
        setupSpinners();
        getDataFromIntent();

        btnUpdate.setOnClickListener(v -> updateCustomer());
    }

    private void initViews() {
        // Khớp ID với file activity_add_customer.xml của ông
        etName = findViewById(R.id.etAddCusName);
        etPhone = findViewById(R.id.etAddCusPhone);
        spnProvince = findViewById(R.id.spnProvince);
        spnDistrict = findViewById(R.id.spnDistrict);
        spnWard = findViewById(R.id.spnWard);
        btnUpdate = findViewById(R.id.btnSaveCustomer);

        // Cập nhật text cho nút bấm
        btnUpdate.setText("CẬP NHẬT");

        // Nếu ông có TextView tiêu đề ở trên cùng, hãy đổi tên nó luôn (ví dụ tvTitleAdd)
        TextView tvTitle = findViewById(androidx.appcompat.R.id.title);
        if (tvTitle != null) tvTitle.setText("SỬA THÔNG TIN KHÁCH");
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

    private void getDataFromIntent() {
        currentCustomer = (Customer) getIntent().getSerializableExtra("CUSTOMER_DATA");
        if (currentCustomer != null) {
            etName.setText(currentCustomer.getCusName());
            etPhone.setText(currentCustomer.getCusPhone());

            // Logic để Spinner tự chọn lại Tỉnh, Huyện, Xã cũ
            preSelectAddress(currentCustomer.getAddress());
        }
    }

    private void preSelectAddress(String fullAddress) {
        if (fullAddress == null || !fullAddress.contains(", ") || provinceList == null) return;

        // Giả sử địa chỉ lưu là: "Phường Mai Dịch, Quận Cầu Giấy, Thành phố Hà Nội"
        String[] parts = fullAddress.split(", ");
        if (parts.length < 3) return;

        String wardName = parts[0].trim();
        String districtName = parts[1].trim();
        String provinceName = parts[2].trim();

        // 1. Tìm và chọn Tỉnh
        for (int i = 0; i < provinceList.size(); i++) {
            if (provinceList.get(i).name.equals(provinceName)) {
                spnProvince.setSelection(i);

                // 2. Tìm và chọn Huyện (Cần load list huyện của tỉnh đó trước)
                List<DistrictData> districts = provinceList.get(i).districts;
                updateDistrict(districts);
                for (int j = 0; j < districts.size(); j++) {
                    if (districts.get(j).name.equals(districtName)) {
                        spnDistrict.setSelection(j);

                        // 3. Tìm và chọn Xã
                        List<String> wards = districts.get(j).wards;
                        updateWard(wards);
                        for (int k = 0; k < wards.size(); k++) {
                            if (wards.get(k).equals(wardName)) {
                                spnWard.setSelection(k);
                                break;
                            }
                        }
                        break;
                    }
                }
                break;
            }
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

    private void updateCustomer() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ tên và SĐT", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gộp địa chỉ mới từ Spinner
        String fullAddress = spnWard.getSelectedItem().toString() + ", " +
                spnDistrict.getSelectedItem().toString() + ", " +
                spnProvince.getSelectedItem().toString();

        currentCustomer.setCusName(name);
        currentCustomer.setCusPhone(phone);
        currentCustomer.setAddress(fullAddress);

        // Gọi API Update
        RetrofitClient.getApiService().updateCustomer(currentCustomer.getId(), currentCustomer).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditCustomerActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditCustomerActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EditCustomerActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Model để đọc JSON
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