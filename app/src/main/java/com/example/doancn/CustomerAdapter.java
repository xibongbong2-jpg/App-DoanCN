package com.example.doancn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doancn.model.Customer;
import java.util.ArrayList;
import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {

    private List<Customer> displayList;
    private List<Customer> listFull;
    private List<Customer> filteredList;
    private Context context;
    private OnCustomerActionListener actionListener; // Đổi tên listener cho chuyên nghiệp
    private boolean isSelectionMode = false;

    private int currentPage = 0;
    private final int ITEMS_PER_PAGE = 10;

    // --- INTERFACE MỚI: Đa năng hơn ---
    public interface OnCustomerActionListener {
        void onCustomerClick(Customer customer); // Dùng cho chọn khách hàng (Invoice)
        void onDetailClick(Customer customer);  // Dùng cho nút "Chi tiết >"
        void onEditClick(Customer customer);    // Dùng cho nút "Sửa"
        void onDeleteClick(Customer customer);  // Dùng cho nút "Xóa"
    }

    // Constructor cho màn hình Danh sách (Mode mặc định)
    public CustomerAdapter(List<Customer> list, Context context, OnCustomerActionListener listener) {
        this(list, context, false, listener);
    }

    // Constructor đầy đủ
    public CustomerAdapter(List<Customer> list, Context context, boolean isSelectionMode, OnCustomerActionListener listener) {
        this.listFull = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
        this.displayList = new ArrayList<>();
        this.context = context;
        this.isSelectionMode = isSelectionMode;
        this.actionListener = listener;
        loadCurrentPage();
    }

    public void updateData(List<Customer> newList) {
        this.listFull = new ArrayList<>(newList);
        this.filteredList = new ArrayList<>(newList);
        this.currentPage = 0;
        loadCurrentPage();
    }

    private void loadCurrentPage() {
        if (isSelectionMode) {
            displayList.clear();
            displayList.addAll(filteredList);
        } else {
            int start = currentPage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, filteredList.size());
            displayList.clear();
            if (start < filteredList.size()) {
                displayList.addAll(filteredList.subList(start, end));
            }
        }
        notifyDataSetChanged();
    }

    public void filter(String text) {
        filteredList = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            filteredList.addAll(listFull);
        } else {
            String p = text.toLowerCase().trim();
            for (Customer c : listFull) {
                if (c.getCusName().toLowerCase().contains(p) || c.getCusPhone().contains(p)) {
                    filteredList.add(c);
                }
            }
        }
        currentPage = 0;
        loadCurrentPage();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isSelectionMode ? R.layout.item_customer : R.layout.item_customer_list;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer c = displayList.get(position);

        if (holder.tvName != null) holder.tvName.setText(c.getCusName());
        if (holder.tvPhone != null) holder.tvPhone.setText("SĐT: " + c.getCusPhone());
        if (holder.tvCredit != null) holder.tvCredit.setText("Điểm: " + (int)c.getCusCredit());
        if (holder.tvId != null) holder.tvId.setText("Mã KH: #" + c.getId());

        if (holder.tvAddress != null) {
            String address = c.getAddress();
            holder.tvAddress.setText("Địa chỉ: " + (address != null && !address.isEmpty() ? address : "Chưa cập nhật"));
        }

        // --- XỬ LÝ CLICK ---
        if (isSelectionMode) {
            // Mode chọn khách hàng (Tạo hóa đơn)
            holder.itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onCustomerClick(c);
            });
        } else {
            // Mode Danh sách khách hàng: Có 3 nút riêng biệt

            // 1. Nút Chi tiết
            if (holder.tvChiTiet != null) {
                holder.tvChiTiet.setOnClickListener(v -> actionListener.onDetailClick(c));
            }

            // 2. Nút Sửa
            if (holder.btnEdit != null) {
                holder.btnEdit.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onEditClick(c); // Nó phải gọi hàm này thì Activity mới nhận được lệnh
                    }
                });
            }

            // 3. Nút Xóa
            if (holder.btnDelete != null) {
                holder.btnDelete.setOnClickListener(v -> actionListener.onDeleteClick(c));
            }

            // Hủy click trên toàn bộ item để tránh nhầm lẫn
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() { return displayList.size(); }

    public boolean nextPage() { if ((currentPage + 1) * ITEMS_PER_PAGE < filteredList.size()) { currentPage++; loadCurrentPage(); return true; } return false; }
    public boolean prevPage() { if (currentPage > 0) { currentPage--; loadCurrentPage(); return true; } return false; }
    public int getCurrentPage() { return currentPage + 1; }
    public int getTotalPages() { int total = (int) Math.ceil((double) filteredList.size() / ITEMS_PER_PAGE); return total == 0 ? 1 : total; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvCredit, tvId, tvChiTiet, tvAddress;
        Button btnEdit, btnDelete; // Khai báo thêm 2 nút

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
            if (tvName == null) tvName = itemView.findViewById(R.id.tvCusName);

            tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
            if (tvPhone == null) tvPhone = itemView.findViewById(R.id.tvCusPhone);

            tvCredit = itemView.findViewById(R.id.tvCustomerCredit);
            if (tvCredit == null) tvCredit = itemView.findViewById(R.id.tvCusCredit);

            tvId = itemView.findViewById(R.id.tvCusId);
            tvChiTiet = itemView.findViewById(R.id.tvChiTiet);
            tvAddress = itemView.findViewById(R.id.tvCusAddress);

            // --- Ánh xạ 2 nút mới ---
            btnEdit = itemView.findViewById(R.id.btnEditCustomer);
            btnDelete = itemView.findViewById(R.id.btnDeleteCustomer);
        }
    }
}