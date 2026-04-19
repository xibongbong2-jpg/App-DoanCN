package com.example.doancn; // Đổi lại package cho đúng với của ông

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doancn.R;
import com.example.doancn.model.User; // Class model Shipper/User của ông
import java.util.List;

public class ShipperAdapter extends RecyclerView.Adapter<ShipperAdapter.ViewHolder> {
    private List<User> list;
    private OnShipperClickListener listener;

    // Interface để bắt sự kiện khi ông bấm chọn 1 Shipper
    public interface OnShipperClickListener {
        void onShipperClick(User shipper);
    }

    public ShipperAdapter(List<User> list, OnShipperClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shipper, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User shipper = list.get(position);

        // ĐÂY CHÍNH LÀ CHỖ HIỂN THỊ ĐỊNH DẠNG MÀ ÔNG MUỐN
        String displayText = shipper.getFull_name() + " (Mã nhân viên: " + shipper.getId() + ")";
        holder.tvShipperInfo.setText(displayText);

        // Bắt sự kiện click vào dòng Shipper
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShipperClick(shipper);
            }
        });
    }

    public void updateData(List<User> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvShipperInfo;

        public ViewHolder(View itemView) {
            super(itemView);
            tvShipperInfo = itemView.findViewById(R.id.tvShipperInfo);
        }
    }
}