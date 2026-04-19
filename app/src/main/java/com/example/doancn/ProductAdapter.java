package com.example.doancn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doancn.api.RetrofitClient;
import com.example.doancn.model.Product;

import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private List<Product> productList; // Danh sách cắt ra để hiển thị trên 1 trang
    private List<Product> productListFull; // Danh sách gốc chứa TẤT CẢ sản phẩm
    private List<Product> currentFilteredList; // Danh sách chứa kết quả sau khi lọc/tìm kiếm

    private Context context;
    private String userRole;
    private OnProductClickListener listener;

    // --- BIẾN PHÂN TRANG ---
    private int currentPage = 0;
    private final int ITEMS_PER_PAGE = 3; // Giới hạn 15 sản phẩm 1 trang

    public ProductAdapter(List<Product> productList, Context context, String userRole, OnProductClickListener listener) {
        this.context = context;
        this.userRole = userRole;
        this.listener = listener;

        this.productListFull = new ArrayList<>(productList);
        this.currentFilteredList = new ArrayList<>(productList);
        this.productList = new ArrayList<>();

        loadCurrentPage(); // Load trang đầu tiên khi khởi tạo
    }

    public void updateData(List<Product> newList) {
        this.productListFull = new ArrayList<>(newList);
        this.currentFilteredList = new ArrayList<>(newList);
        this.currentPage = 0; // Trở về trang 1 khi có data mới
        loadCurrentPage();
    }

    // ================= LOGIC PHÂN TRANG =================
    private void loadCurrentPage() {
        int startItem = currentPage * ITEMS_PER_PAGE;
        int endItem = Math.min(startItem + ITEMS_PER_PAGE, currentFilteredList.size());

        this.productList.clear();
        if (startItem < currentFilteredList.size()) {
            this.productList.addAll(currentFilteredList.subList(startItem, endItem));
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
    // ====================================================

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvProductName.setText(product.getPro_name());
        holder.tvProductCode.setText("Mã: " + product.getPro_code());

        // --- LOGIC KIỂM TRA HẾT HÀNG ---
        int stock = product.getStock();
        if (stock <= 0) {
            holder.tvProductStock.setText("Tình trạng: HẾT HÀNG");
            holder.tvProductStock.setTextColor(Color.RED);
            holder.itemView.setAlpha(0.4f);
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(context, "Sản phẩm này hiện đã hết hàng!", Toast.LENGTH_SHORT).show();
            });
        } else {
            holder.tvProductStock.setText("Số lượng: " + stock);
            holder.tvProductStock.setTextColor(Color.parseColor("#757575"));
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }

        DecimalFormat formatter = new DecimalFormat("#,###,###,##0");
        holder.tvProductPrice.setText("Giá: " + formatter.format(product.getPrice()) + " VNĐ");

        String imgUrl = product.getPro_image();
        if (imgUrl != null && !imgUrl.startsWith("http")) {
            imgUrl = RetrofitClient.BASE_URL + "uploads/" + imgUrl;
        }
        Glide.with(context).load(imgUrl).placeholder(R.drawable.ic_product).into(holder.imgProduct);

        boolean isAdmin = "admin".equalsIgnoreCase(userRole);
        holder.layoutAdminActions.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, UpdateProductActivity.class);
            intent.putExtra("PRODUCT_DATA", product);
            ((Activity) context).startActivityForResult(intent, 999);
        });

        holder.btnDelete.setOnClickListener(v -> {
            deleteProduct(product);
        });
    }

    // Đã sửa lại logic xóa để không bị lỗi index khi phân trang
    private void deleteProduct(Product p) {
        RetrofitClient.getApiService().deleteProduct(p.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    productListFull.remove(p);
                    currentFilteredList.remove(p);
                    loadCurrentPage(); // Tự động load lại trang hiện tại sau khi xóa
                    Toast.makeText(context, "Đã xóa!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    @Override
    public int getItemCount() { return productList != null ? productList.size() : 0; }

    // ================= LOGIC TÌM KIẾM & LỌC =================
    public void filter(String text) {
        currentFilteredList.clear();
        if (text.isEmpty()) {
            currentFilteredList.addAll(productListFull);
        } else {
            String pat = removeAccent(text.toLowerCase().trim());
            for (Product p : productListFull) {
                if (removeAccent(p.getPro_name().toLowerCase()).contains(pat) ||
                        (p.getPro_code() != null && p.getPro_code().toLowerCase().contains(pat))) {
                    currentFilteredList.add(p);
                }
            }
        }
        currentPage = 0; // Trở về trang 1 khi lọc
        loadCurrentPage();
    }

    public void filterByCategory(String cat) {
        currentFilteredList.clear();
        if (cat.equals("TẤT CẢ")) {
            currentFilteredList.addAll(productListFull);
        } else {
            for (Product p : productListFull) {
                if (p.getCategory() != null && p.getCategory().equalsIgnoreCase(cat)) {
                    currentFilteredList.add(p);
                }
            }
        }
        currentPage = 0; // Trở về trang 1 khi lọc
        loadCurrentPage();
    }
    // ========================================================

    private String removeAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvProductCode, tvProductStock, tvProductPrice;
        ImageView imgProduct, btnEdit, btnDelete;
        LinearLayout layoutAdminActions;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductCode = itemView.findViewById(R.id.tvProductCode);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvProductStock = itemView.findViewById(R.id.tvProductStock);
            btnEdit = itemView.findViewById(R.id.btnEditProduct);
            btnDelete = itemView.findViewById(R.id.btnDeleteProduct);
            layoutAdminActions = itemView.findViewById(R.id.layoutAdminActions);
        }
    }
}