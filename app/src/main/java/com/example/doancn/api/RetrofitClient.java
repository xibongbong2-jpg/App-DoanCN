package com.example.doancn.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // 1. Sửa ở đây để đồng bộ
    public static final String BASE_URL = "http://192.168.1.17:8080/";
    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    // 2. Sửa ở đây là quan trọng nhất
                    .baseUrl("http://192.168.1.17:8080/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}