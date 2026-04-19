package com.example.doancn;

import android.content.Intent;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doancn.model.Invoice;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    private List<Invoice> list;
    private List<Invoice> invoiceListFull;
    private List<Invoice> currentFilteredList;

    private int currentPage = 0;
    private int ITEMS_PER_PAGE = 5;

    public InvoiceAdapter(List<Invoice> list) {
        this.invoiceListFull = new ArrayList<>(list);
        this.currentFilteredList = new ArrayList<>(list);
        this.list = new ArrayList<>();
        loadCurrentPage();
    }

    public void updateList(List<Invoice> newList) {
        this.invoiceListFull = new ArrayList<>(newList);
        this.currentFilteredList = new ArrayList<>(newList);
        this.currentPage = 0;
        loadCurrentPage();
    }

    private void loadCurrentPage() {
        int startItem = currentPage * ITEMS_PER_PAGE;
        int endItem = Math.min(startItem + ITEMS_PER_PAGE, currentFilteredList.size());

        this.list.clear();
        if (startItem < currentFilteredList.size()) {
            this.list.addAll(currentFilteredList.subList(startItem, endItem));
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

    public int getCurrentPage() { return currentPage + 1; }

    public int getTotalPages() {
        int total = (int) Math.ceil((double) currentFilteredList.size() / ITEMS_PER_PAGE);
        return total == 0 ? 1 : total;
    }

    public void setItemsPerPage(int number) {
        this.ITEMS_PER_PAGE = number;
        this.currentPage = 0;
        loadCurrentPage();
    }

    public void filterList(String query, String status) {
        currentFilteredList.clear();
        String filterPattern = query.toLowerCase().trim();

        for (Invoice inv : invoiceListFull) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;

            if (!filterPattern.isEmpty()) {
                String idString = String.valueOf(inv.getId());
                matchesSearch = idString.contains(filterPattern);
            }

            if (!status.equals("Tất cả")) {
                String invStatus = inv.getStatus() != null ? inv.getStatus() : "";
                matchesStatus = invStatus.equalsIgnoreCase(status);
            }

            if (matchesSearch && matchesStatus) {
                currentFilteredList.add(inv);
            }
        }
        currentPage = 0;
        loadCurrentPage();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice inv = list.get(position);
        DecimalFormat formatter = new DecimalFormat("#,###,###,##0");

        holder.tvId.setText("Mã HD: #" + inv.getId());
        holder.tvCustomer.setText("ID Khách hàng: " + inv.getCustomerId());
        holder.tvEmployee.setText("ID Nhân viên: " + inv.getUserId());
        holder.tvTotal.setText("Tổng: " + formatter.format(inv.getTotalAmount()) + " VNĐ");
        holder.tvStatus.setText(inv.getStatus());

        // --- RESET TRẠNG THÁI TRƯỚC KHI XỬ LÝ (QUAN TRỌNG CHO RECYCLERVIEW) ---
        holder.viewAlertDot.clearAnimation();

        // 1. Xử lý trạng thái CHỜ XÁC NHẬN
        if ("Chờ xác nhận".equalsIgnoreCase(inv.getStatus())) {
            holder.viewAlertDot.setVisibility(View.VISIBLE);

            // Chạy hiệu ứng Pulse Pro
            Animation pulse = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.pulse_warning);
            holder.viewAlertDot.startAnimation(pulse);

            // Chỉnh màu cam đậm và tăng size chữ lên 13sp
            holder.tvStatus.setTextColor(Color.parseColor("#E65100"));
            holder.tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);

        } else {
            // 2. Xử lý các trạng thái khác
            holder.viewAlertDot.setVisibility(View.GONE);
            holder.tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); // Trả về size mặc định

            if ("Giao hàng thất bại".equalsIgnoreCase(inv.getStatus())) {
                holder.tvStatus.setTextColor(Color.RED);
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Xanh lá
            }
        }

        holder.btnViewDetail.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), InvoiceDetailActivity.class);
            intent.putExtra("INVOICE_DATA", inv);
            intent.putExtra("INVOICE_ID", inv.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvCustomer, tvEmployee, tvTotal, tvStatus, btnViewDetail;
        View viewAlertDot;

        public ViewHolder(View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tvInvoiceId);
            tvCustomer = itemView.findViewById(R.id.tvInvoiceCustomer);
            tvEmployee = itemView.findViewById(R.id.tvInvoiceEmployee);
            tvTotal = itemView.findViewById(R.id.tvInvoiceTotal);
            tvStatus = itemView.findViewById(R.id.tvInvoiceStatus);
            btnViewDetail = itemView.findViewById(R.id.btnViewDetail);
            viewAlertDot = itemView.findViewById(R.id.viewAlertDot);
        }
    }
}