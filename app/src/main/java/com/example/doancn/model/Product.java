package com.example.doancn.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Product implements Serializable {
    @SerializedName("proId")
    private int id;

    @SerializedName("proName")
    private String pro_name;

    @SerializedName("proCode")
    private String pro_code;

    private int stock;
    private double price;

    @SerializedName("proImage")
    private String pro_image;

    private String category;
    @SerializedName("quantity")
    private int quantity; // Biến này để nhận số lượng từ hóa đơn

    public Product() {}

    // --- GETTER ---
    public int getId() { return id; }
    public String getPro_name() { return pro_name; }
    public String getPro_code() { return pro_code; }
    public int getStock() { return stock; }
    public double getPrice() { return price; }
    public String getPro_image() { return pro_image; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }

    public void setId(int id) { this.id = id; }
    public void setPro_name(String pro_name) { this.pro_name = pro_name; }
    public void setPro_code(String pro_code) { this.pro_code = pro_code; }
    public void setStock(int stock) { this.stock = stock; }
    public void setPrice(double price) { this.price = price; }
    public void setPro_image(String pro_image) { this.pro_image = pro_image; }
    public void setCategory(String category) { this.category = category; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}