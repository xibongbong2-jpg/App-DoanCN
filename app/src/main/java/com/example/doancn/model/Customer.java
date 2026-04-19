package com.example.doancn.model;


import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Customer implements Serializable {
    @SerializedName("cusId")
    private int id;
    @SerializedName("cusName")
    private String cusName;
    @SerializedName("cusPhone")
    private String cusPhone;
    @SerializedName("cusAddress")
    private String address;
    @SerializedName("cusCredit")
    private int cusCredit;
    // Constructor và Getters (để Adapter dùng)

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCusName() {
        return cusName;
    }

    public void setCusName(String cusName) {
        this.cusName = cusName;
    }

    public String getCusPhone() {
        return cusPhone;
    }

    public void setCusPhone(String cusPhone) {
        this.cusPhone = cusPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getCusCredit() {
        return cusCredit;
    }

    public void setCusCredit(int cusCredit) {
        this.cusCredit = cusCredit;
    }
}
