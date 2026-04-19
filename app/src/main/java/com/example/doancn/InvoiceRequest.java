package com.example.doancn;

import java.io.Serializable;
import java.util.List;

/**
 * Class này dùng để đóng gói dữ liệu Hóa đơn gửi lên Server qua API
 */
public class InvoiceRequest implements Serializable {
    private int cusID;
    private int user_id;
    private List<Integer> productIds;
    private List<Integer> quantities; // Đưa lên đầu cho dễ quản lý
    private double totalAmount;
    private int usedPoints;
    private String addressDetail;
    private String paymentMethod;
    // Thêm vào InvoiceRequest.java
    private String paymentCode;

    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }

    // Constructor mặc định (Bắt buộc phải có để GSON hoạt động)
    public InvoiceRequest() {}

    // --- Getters và Setters ---

    public int getCusID() {
        return cusID;
    }

    public void setCusID(int cusID) {
        this.cusID = cusID;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public List<Integer> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Integer> productIds) {
        this.productIds = productIds;
    }

    public List<Integer> getQuantities() {
        return quantities;
    }

    public void setQuantities(List<Integer> quantities) {
        this.quantities = quantities;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getUsedPoints() {
        return usedPoints;
    }

    public void setUsedPoints(int usedPoints) {
        this.usedPoints = usedPoints;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}