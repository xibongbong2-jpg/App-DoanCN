package com.example.doancn.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Invoice implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("customerId")
    private int customerId;

    @SerializedName("userId")
    private int userId;

    @SerializedName("totalAmount")
    private double totalAmount;

    @SerializedName("usedPoints")
    private int usedPoints;

    @SerializedName("addressDetail")
    private String addressDetail;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private String createdAt; // Nhận dạng String từ JSON cho dễ xử lý
    @SerializedName("shipperId")
    private Integer shipperId;
    @SerializedName("imageProof")
    private String imageProof;
    private String reason;
    public Invoice() {}

    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public int getUsedPoints() { return usedPoints; }
    public void setUsedPoints(int usedPoints) { this.usedPoints = usedPoints; }

    public String getAddressDetail() { return addressDetail; }
    public void setAddressDetail(String addressDetail) { this.addressDetail = addressDetail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Integer getShipperId() {
        return shipperId;
    }

    public void setShipperId(Integer shipperId) {
        this.shipperId = shipperId;
    }
    public String getImageProof() {
        return imageProof;
    }

    public void setImageProof(String imageProof) {
        this.imageProof = imageProof;
    }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}