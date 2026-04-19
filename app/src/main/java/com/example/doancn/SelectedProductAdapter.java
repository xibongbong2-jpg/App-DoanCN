package com.example.doancn;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doancn.model.Product;

import java.util.List;

public class SelectedProductAdapter extends RecyclerView.Adapter<SelectedProductAdapter.ViewHolder> {
    private List<Product> list;
    private OnQuantityChangeListener listener;

    public interface OnQuantityChangeListener {
        void onQuantityChanged();
    }

    public SelectedProductAdapter(List<Product> list, OnQuantityChangeListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = list.get(position);

        holder.name.setText(p.getPro_name());
        holder.code.setText("Mã: " + p.getPro_code());

        // Lấy tồn kho (Giả sử tên biến trong Model là pro_stock hoặc stock)
        // Nếu model của bạn chưa có biến này, hãy thêm vào Product.java
        int stock = p.getStock();
        int orderQty = p.getQuantity(); // Số lượng đang chọn mua

        if (stock <= 0) {
            // --- TRẠNG THÁI HẾT HÀNG ---
            holder.tvOutOfStock.setVisibility(View.VISIBLE);
            holder.layoutQuantityControl.setVisibility(View.GONE); // Ẩn cụm tăng giảm
            holder.itemView.setAlpha(0.5f);
            holder.price.setText("Hết hàng");
        } else {
            // --- TRẠNG THÁI CÒN HÀNG ---
            holder.tvOutOfStock.setVisibility(View.GONE);
            holder.layoutQuantityControl.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);

            // Hiển thị số lượng mua
            holder.tvQuantity.setText(String.valueOf(orderQty));

            // Tính tiền: Giá x Số lượng mua
            double totalPriceForItem = p.getPrice() * orderQty;
            holder.price.setText(String.format("%,.0f", totalPriceForItem) + "đ");
        }

        // Nút Tăng (+)
        holder.btnPlus.setOnClickListener(v -> {
            // CHỐT CHẶN: Không cho tăng quá số lượng tồn kho
            if (p.getQuantity() < p.getStock()) {
                p.setQuantity(p.getQuantity() + 1);
                notifyItemChanged(position);
                if (listener != null) listener.onQuantityChanged();
            } else {
                Toast.makeText(v.getContext(), "Đã đạt giới hạn tồn kho (" + p.getStock() + ")", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút Giảm (-)
        holder.btnMinus.setOnClickListener(v -> {
            if (p.getQuantity() > 1) {
                p.setQuantity(p.getQuantity() - 1);
                notifyItemChanged(position);
            } else {
                // Nếu đang là 1 mà bấm trừ thì xóa luôn khỏi danh sách chọn
                list.remove(position);
                notifyDataSetChanged(); // Dùng cái này để cập nhật lại toàn bộ vị trí (position)
            }
            if (listener != null) listener.onQuantityChanged();
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, code, price, tvQuantity, tvOutOfStock;
        ImageButton btnPlus, btnMinus;
        LinearLayout layoutQuantityControl; // Khai báo thêm để ẩn hiện cả cụm

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvSelectedName);
            code = itemView.findViewById(R.id.tvSelectedCode);
            price = itemView.findViewById(R.id.tvSelectedPrice);
            tvQuantity = itemView.findViewById(R.id.tvSelectedQuantity);
            btnPlus = itemView.findViewById(R.id.btnPlusQuantity);
            btnMinus = itemView.findViewById(R.id.btnMinusQuantity);
            tvOutOfStock = itemView.findViewById(R.id.tvOutOfStockLabel);

            // Ánh xạ layout chứa 2 nút + - và số lượng
            layoutQuantityControl = itemView.findViewById(R.id.layoutQuantityControl);
        }
    }
}