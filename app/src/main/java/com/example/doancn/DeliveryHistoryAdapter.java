package com.example.doancn;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Invoice;

import java.util.List;

public class DeliveryHistoryAdapter extends RecyclerView.Adapter<DeliveryHistoryAdapter.ViewHolder> {

    private Context context;
    private List<Invoice> list;

    public DeliveryHistoryAdapter(Context context, List<Invoice> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_delivery_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice inv = list.get(position);

        holder.tvOrderId.setText("Đơn hàng: #" + inv.getId());
        holder.tvCusId.setText("Mã khách hàng: " + inv.getCustomerId());
        holder.tvStaffId.setText("Mã nhân viên giao: " + inv.getShipperId());

        // --- XỬ LÝ HIỂN THỊ TRẠNG THÁI KÈM LÝ DO ---
        String statusText = inv.getStatus() != null ? inv.getStatus() : "N/A";

        // Nếu giao thất bại, chúng ta sẽ cộng thêm lý do vào sau trong ngoặc đơn
        if ("Giao hàng thất bại".equals(inv.getStatus())) {
            if (inv.getReason() != null && !inv.getReason().isEmpty()) {
                statusText += " (" + inv.getReason() + ")";
            }
            holder.tvStatus.setTextColor(android.graphics.Color.RED);
        }
        else if ("Giao hàng thành công".equals(inv.getStatus())) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        }
        else {
            holder.tvStatus.setTextColor(android.graphics.Color.GRAY);
        }

        holder.tvStatus.setText("Trạng thái: " + statusText);

        // --- LOAD ẢNH MINH CHỨNG ---
        String imageUrl = RetrofitClient.BASE_URL + "uploads/" + inv.getImageProof();
        if (inv.getImageProof() != null && !inv.getImageProof().isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image) // Ảnh tạm khi đang load
                    .into(holder.ivProof);
            holder.ivProof.setVisibility(View.VISIBLE);
        } else {
            holder.ivProof.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvCusId, tvStaffId, tvStatus;
        ImageView ivProof;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvHistoryOrderId);
            tvCusId = itemView.findViewById(R.id.tvHistoryCusId);
            tvStaffId = itemView.findViewById(R.id.tvHistoryStaffId);
            tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
            ivProof = itemView.findViewById(R.id.ivHistoryProof);
        }
    }
}