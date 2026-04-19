package com.example.doancn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Invoice;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private String userRole, full_name, dept, avatar, dob;
    private int currentUserId;

    private TextView tvHeaderName, tvHeaderRoleDept, tvHomeFullName, tvHomeRole, tvHomeDept, tvHomeDob;
    private ImageView imgHeaderAvatar, imgHomeAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Lấy dữ liệu từ SharedPreferences
        SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
        currentUserId = pref.getInt("id", -1);
        full_name = pref.getString("full_name", "N/A");
        userRole = pref.getString("role", "staff");
        dept = pref.getString("dept", "Cửa hàng");
        avatar = pref.getString("avatar", "");
        dob = pref.getString("dob", "Chưa cập nhật");

        // 2. Setup Toolbar và Drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 3. Ánh xạ View màn hình chính
        imgHomeAvatar = findViewById(R.id.imgHomeAvatar);
        tvHomeFullName = findViewById(R.id.tvHomeFullName);
        tvHomeRole = findViewById(R.id.tvHomeRole);
        tvHomeDept = findViewById(R.id.tvHomeDept);
        tvHomeDob = findViewById(R.id.tvHomeDob);

        // 4. Ánh xạ Header Menu
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            tvHeaderName = headerView.findViewById(R.id.tvHeaderName);
            tvHeaderRoleDept = headerView.findViewById(R.id.tvHeaderRoleDept);
            imgHeaderAvatar = headerView.findViewById(R.id.imgHeaderAvatar);
        }

        updateUI();
        setupNavigationMenuPermissions();

        // 5. Xử lý Click Menu
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_invoice) {
                startActivity(new Intent(this, CreateInvoiceActivity.class));
            }
            else if (id == R.id.nav_products) {
                startActivity(new Intent(this, ProductListActivity.class));
            }
            else if (id == R.id.nav_customers) {
                startActivity(new Intent(this, CustomerListActivity.class));
            }
            else if (id == R.id.nav_employee_list) {
                startActivity(new Intent(this, EmployeeListActivity.class));
            }
            else if (id == R.id.nav_add_employee) {
                startActivity(new Intent(this, AddEmployeeActivity.class));
            }
            else if (id == R.id.nav_history_personal) {
                if (currentUserId != -1) {
                    Intent intent = new Intent(this, InvoiceListActivity.class);
                    intent.putExtra("FILTER_USER_ID", currentUserId);
                    intent.putExtra("SCREEN_TITLE", "LỊCH SỬ CÁ NHÂN");
                    startActivity(intent);
                }
            }
            else if (id == R.id.nav_history_all) {
                Intent intent = new Intent(this, InvoiceListActivity.class);
                intent.putExtra("FILTER_USER_ID", -1);
                intent.putExtra("SCREEN_TITLE", "TẤT CẢ HÓA ĐƠN");
                startActivity(intent);
            }
            else if (id == R.id.nav_shipper_orders) {
                Intent intent = new Intent(this, InvoiceListActivity.class);
                intent.putExtra("IS_SHIPPER_VIEW", true);
                intent.putExtra("SCREEN_TITLE", "ĐƠN HÀNG CẦN GIAO");
                startActivity(intent);
            }
            else if (id == R.id.nav_shipper_history) {
                startActivity(new Intent(this, DeliveryHistoryActivity.class));
            }
            else if (id == R.id.nav_logout) {
                logout();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mỗi lần quay lại trang chủ, kiểm tra để bật/tắt đèn nháy
        checkPendingInvoices();
    }

    /**
     * Kiểm tra danh sách hóa đơn để hiển thị thông báo nhấp nháy trên Menu
     */
    private void checkPendingInvoices() {
        RetrofitClient.getApiService().getInvoices(null).enqueue(new Callback<List<Invoice>>() {
            @Override
            public void onResponse(Call<List<Invoice>> call, Response<List<Invoice>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Invoice> allInvoices = response.body();
                    boolean hasPersonalPending = false;
                    boolean hasAnyPending = false;

                    for (Invoice inv : allInvoices) {
                        if ("Chờ xác nhận".equalsIgnoreCase(inv.getStatus())) {
                            hasAnyPending = true; // Có ít nhất 1 đơn chờ trong hệ thống
                            if (inv.getUserId() == currentUserId) {
                                hasPersonalPending = true; // Có đơn chờ của riêng mình
                            }
                        }
                    }

                    // Cập nhật đèn cho mục Cá nhân
                    updateMenuBadge(R.id.nav_history_personal, hasPersonalPending);

                    // Cập nhật đèn cho mục Toàn bộ (Chỉ hiện nếu là Admin)
                    if ("admin".equalsIgnoreCase(userRole)) {
                        updateMenuBadge(R.id.nav_history_all, hasAnyPending);
                    }
                }
            }
            @Override public void onFailure(Call<List<Invoice>> call, Throwable t) {}
        });
    }

    /**
     * Hàm helper để bật/tắt và chạy animation cho dấu chấm cam
     */
    private void updateMenuBadge(int menuItemId, boolean show) {
        MenuItem item = navigationView.getMenu().findItem(menuItemId);
        if (item != null && item.getActionView() != null) {
            View dot = item.getActionView().findViewById(R.id.menuAlertDot);
            if (dot != null) {
                if (show) {
                    dot.setVisibility(View.VISIBLE);
                    Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_warning);
                    dot.startAnimation(pulse);
                } else {
                    dot.clearAnimation();
                    dot.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setupNavigationMenuPermissions() {
        boolean isAdmin = "admin".equalsIgnoreCase(userRole);
        boolean isShipper = "shipper".equalsIgnoreCase(userRole);
        Menu menu = navigationView.getMenu();

        if (isShipper) {
            // SHIPPER permissions
            menu.findItem(R.id.menu_invoice).setVisible(false);
            menu.findItem(R.id.nav_products).setVisible(false);
            menu.findItem(R.id.nav_history_personal).setVisible(false);
            menu.findItem(R.id.nav_history_all).setVisible(false);
            menu.findItem(R.id.nav_shipper_orders).setVisible(true);
            menu.findItem(R.id.nav_shipper_history).setVisible(true);
        } else {
            // ADMIN / STAFF permissions
            menu.findItem(R.id.nav_shipper_orders).setVisible(false);
            menu.findItem(R.id.nav_shipper_history).setVisible(false);
            menu.findItem(R.id.nav_history_all).setVisible(isAdmin);
            menu.findItem(R.id.nav_employee_list).setVisible(isAdmin);
            menu.findItem(R.id.nav_add_employee).setVisible(isAdmin);
        }
    }

    private void updateUI() {
        if (tvHeaderName != null) tvHeaderName.setText(full_name);
        if (tvHeaderRoleDept != null) tvHeaderRoleDept.setText(userRole + " - " + dept);

        tvHomeFullName.setText(full_name);
        tvHomeRole.setText("Chức vụ: " + userRole);
        tvHomeDept.setText("Bộ phận: " + dept);
        tvHomeDob.setText("Ngày sinh: " + dob);

        String fullImageUrl = RetrofitClient.BASE_URL + "uploads/" + avatar;
        Glide.with(this).load(fullImageUrl).placeholder(R.drawable.ca_nhan).circleCrop().into(imgHeaderAvatar);
        Glide.with(this).load(fullImageUrl).placeholder(R.drawable.ca_nhan).into(imgHomeAvatar);
    }

    private void logout() {
        getSharedPreferences("USER_DATA", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}