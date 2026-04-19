package com.example.doancn.api;

import com.example.doancn.InvoiceRequest;
import com.example.doancn.model.Customer;
import com.example.doancn.model.Invoice;
import com.example.doancn.model.Product;
import com.example.doancn.model.User;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // === QUẢN LÝ TÀI KHOẢN & NHÂN VIÊN ===
    @POST("api/auth/login")
    Call<User> login(@Body User user);

    @POST("api/users/add")
    Call<User> addEmployee(@Body User newUser);

    @Multipart
    @POST("api/users/upload-avatar")
    Call<ResponseBody> uploadAvatar(@Part MultipartBody.Part file);

    @GET("api/users/all")
    Call<List<User>> getAllEmployees();

    @DELETE("api/users/delete/{id}")
    Call<ResponseBody> deleteEmployee(@Path("id") int id);

    @Multipart
    @PUT("api/users/update-with-image/{id}")
    Call<ResponseBody> updateEmployeeWithImage(
            @Path("id") int id,
            @Part("user") RequestBody userJson,
            @Part MultipartBody.Part image
    );

    @GET("api/users/shippers")
    Call<List<User>> getAllShippers();


    // === QUẢN LÝ SẢN PHẨM ===
    @GET("api/products/all")
    Call<List<Product>> getAllProducts();

    @Multipart
    @POST("api/products/add")
    Call<Product> addProduct(
            @Part("proName") RequestBody name,
            @Part("proCode") RequestBody code,
            @Part("category") RequestBody category,
            @Part("stock") RequestBody stock,
            @Part("price") RequestBody price,
            @Part MultipartBody.Part image
    );

    @Multipart
    @PUT("api/products/update/{id}")
    Call<Product> updateProduct(
            @Path("id") int id,
            @Part("proName") RequestBody name,
            @Part("proCode") RequestBody code,
            @Part("category") RequestBody category,
            @Part("stock") RequestBody stock,
            @Part("price") RequestBody price,
            @Part MultipartBody.Part image
    );

    @DELETE("api/products/delete/{id}")
    Call<Void> deleteProduct(@Path("id") int id);


    // === QUẢN LÝ KHÁCH HÀNG ===
    @GET("api/customers/all")
    Call<List<Customer>> getAllCustomers();

    @POST("api/customers/add")
    Call<Customer> addCustomer(@Body Customer customer);

    @DELETE("api/customers/delete/{id}")
    Call<Void> deleteCustomer(@Path("id") int id);

    @PUT("api/customers/update/{id}")
    Call<Void> updateCustomer(@Path("id") int id, @Body Customer customer);

    @GET("api/customers/{id}")
    Call<Customer> getCustomerById(@Path("id") int customerId);


    // === QUẢN LÝ HÓA ĐƠN (INVOICE) ===
    @POST("api/invoices/create")
    Call<Void> createInvoice(@Body InvoiceRequest request);

    /**
     * Lấy danh sách hóa đơn.
     * Nếu userId != null: Lấy hóa đơn cá nhân.
     * Nếu userId == null: Lấy tất cả hóa đơn (Cho Admin).
     */
    @GET("api/invoices/all")
    Call<List<Invoice>> getInvoices(@Query("userId") Integer userId);

    @GET("api/invoices/{id}/items")
    Call<List<Product>> getInvoiceItems(@Path("id") int invoiceId);

    @PUT("api/invoices/{id}/assign-shipper")
    Call<String> assignShipper(
            @Path("id") int invoiceId,
            @Query("shipperId") int shipperId
    );

    @GET("api/invoices/customer/{cusId}")
    Call<List<Invoice>> getInvoicesByCustomer(@Path("cusId") int cusId);


    // === CHỨC NĂNG DÀNH CHO SHIPPER ===
    @GET("api/invoices/shipper/{shipperId}/pending")
    Call<List<Invoice>> getPendingOrdersForShipper(@Path("shipperId") int shipperId);

    @GET("api/invoices/shipper/{shipperId}/history")
    Call<List<Invoice>> getHistoryForShipper(@Path("shipperId") int shipperId);

    @Multipart
    @POST("api/invoices/confirm/{id}")
    Call<String> confirmDelivery(
            @Path("id") int id,
            @Part MultipartBody.Part image,
            @Part("status") RequestBody status,
            @Part("reason") RequestBody reason
    );


    // === KIỂM TRA ĐƠN CHỜ (ĐỂ BẬT ĐÈN NHÁY MENU) ===
    @GET("api/invoices/check-pending/personal/{userId}")
    Call<Boolean> hasPendingPersonal(@Path("userId") int userId);

    @GET("api/invoices/check-pending/all")
    Call<Boolean> hasPendingAll();
    // Thêm API này vào ApiService.java
    @GET("api/payments/check/{paymentCode}")
    Call<Boolean> checkPaymentStatus(@Path("paymentCode") String paymentCode);
    @GET("api/payments/check-status/{paymentCode}")
    Call<Boolean> checkStatus(@Path("paymentCode") String paymentCode);
}