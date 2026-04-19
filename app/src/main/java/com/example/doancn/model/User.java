package com.example.doancn.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;

public class User implements java.io.Serializable {
    private int id;
    private String username;
    private String password;
    private String full_name;
    private String dob;
    private String role;
    private String department;
    private String user_image;

    public User()  {
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getUser_image() {
        return user_image;
    }

    public void setUser_image(String user_image) {
        this.user_image = user_image;
    }


    // Constructor cho Login
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

}