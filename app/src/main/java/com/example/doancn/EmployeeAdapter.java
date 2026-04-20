package com.example.doancn;

import static retrofit2.Response.error;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.User;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {

    private List<User> userList; // Danh sách hiển thị trên 1 trang
    private List<User> userListFull; // Danh sách gốc chứa tất cả nhân viên
    private List<User> currentFilteredList; // Danh sách kết quả sau khi tìm kiếm
    private Context context;

    // --- BIẾN PHÂN TRANG ---
    private int currentPage = 0;
    private int ITEMS_PER_PAGE = 5; // Có thể thay đổi số lượng ở đây

    public EmployeeAdapter(List<User> userList, Context context) {
        this.context = context;
        this.userListFull = new ArrayList<>(userList);
        this.currentFilteredList = new ArrayList<>(userList);
        this.userList = new ArrayList<>();

        loadCurrentPage(); // Cắt danh sách lần đầu tiên
    }

    // Hàm cập nhật lại danh sách gốc khi dữ liệu từ Server thay đổi
    public void updateData(List<User> newList) {
        this.userListFull = new ArrayList<>(newList);
        this.currentFilteredList = new ArrayList<>(newList);
        this.currentPage = 0; // Đặt lại về trang 1
        loadCurrentPage();
    }

    // ================= LOGIC PHÂN TRANG =================
    private void loadCurrentPage() {
        int startItem = currentPage * ITEMS_PER_PAGE;
        int endItem = Math.min(startItem + ITEMS_PER_PAGE, currentFilteredList.size());

        this.userList.clear();
        if (startItem < currentFilteredList.size()) {
            this.userList.addAll(currentFilteredList.subList(startItem, endItem));
        }
        notifyDataSetChanged();
    }

    public boolean nextPage() {
        if ((currentPage + 1) * ITEMS_PER_PAGE < currentFilteredList.size()) {
            currentPage++;
            loadCurrentPage();
            return true;
        }
        return false;
    }

    public boolean prevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadCurrentPage();
            return true;
        }
        return false;
    }

    public int getCurrentPage() {
        return currentPage + 1;
    }

    public int getTotalPages() {
        int total = (int) Math.ceil((double) currentFilteredList.size() / ITEMS_PER_PAGE);
        return total == 0 ? 1 : total;
    }

    // Hàm này cho phép ông đổi số lượng hiển thị từ Activity (nếu cần)
    public void setItemsPerPage(int number) {
        this.ITEMS_PER_PAGE = number;
        this.currentPage = 0;
        loadCurrentPage();
    }
    // ====================================================

    // ================= LOGIC TÌM KIẾM =================
    public void filter(String text) {
        currentFilteredList.clear();
        if (text.isEmpty()) {
            currentFilteredList.addAll(userListFull);
        } else {
            String filterPattern = removeAccent(text.toLowerCase().trim());
            for (User user : userListFull) {
                String nameNormalized = removeAccent(user.getFull_name() != null ? user.getFull_name().toLowerCase() : "");
                String idString = String.valueOf(user.getId());

                if (nameNormalized.contains(filterPattern) || idString.contains(filterPattern)) {
                    currentFilteredList.add(user);
                }
            }
        }
        currentPage = 0; // Trở về trang 1 khi tìm kiếm
        loadCurrentPage();
    }
    // ====================================================

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvName.setText(user.getFull_name());
        holder.tvInfo.setText(user.getRole() + " - " + user.getDepartment());

        String imgUrl = RetrofitClient.BASE_URL + "uploads/" + user.getUser_image();
        Log.d("DEBUG_IMAGE", "Link ảnh đang tải: " + imgUrl);
        Glide.with(context).load(imgUrl).circleCrop().into(holder.imgAvatar);

        // XỬ LÝ XÓA - Đã cập nhật cho phù hợp với phân trang
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa " + user.getFull_name() + "?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        RetrofitClient.getApiService().deleteEmployee(user.getId()).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    // Xóa khỏi danh sách gốc và danh sách lọc
                                    userListFull.remove(user);
                                    currentFilteredList.remove(user);

                                    // Load lại trang hiện tại (tự động điền phần tử khác lên)
                                    loadCurrentPage();

                                    Toast.makeText(context, "Đã xóa thành công!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Server từ chối xóa!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(context, "Lỗi kết nối khi xóa!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Hủy", null).show();
        });

        holder.btnEdit.setOnClickListener(v -> {
            Toast.makeText(context, "Đang mở sửa: " + user.getFull_name(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context, AddEmployeeActivity.class);
            Log.d("ADAPTER_ID", "ID gốc trong danh sách: " + user.getId());
            intent.putExtra("EDIT_USER_DATA", user);
            context.startActivity(intent);
        });
    }

    // Hàm hỗ trợ xóa dấu Tiếng Việt
    public String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    @Override
    public int getItemCount() {
        return userList == null ? 0 : userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imgAvatar;
        public TextView tvName, tvInfo;
        public ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgEmpItem);
            tvName = itemView.findViewById(R.id.tvEmpName);
            tvInfo = itemView.findViewById(R.id.tvEmpInfo);
            btnEdit = itemView.findViewById(R.id.btnEditEmp);
            btnDelete = itemView.findViewById(R.id.btnDeleteEmp);
        }
    }
}