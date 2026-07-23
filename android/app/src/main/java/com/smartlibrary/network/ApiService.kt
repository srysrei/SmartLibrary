package com.smartlibrary.network

import com.smartlibrary.network.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface covering every SmartLibrary API endpoint. All calls are suspend
 * functions returning [Response] so callers can inspect status codes and error bodies.
 * The Authorization header is added automatically by [AuthInterceptor].
 */
interface ApiService {

    // ---------- Auth ----------

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun me(): Response<UserResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<MessageResponse>

    @PUT("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<MessageResponse>

    // ---------- Users ----------

    @GET("users/me")
    suspend fun getProfile(): Response<UserResponse>

    @PUT("users/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<UserResponse>

    /**
     * Uploads the caller's own profile image (multipart, part name `file`). The server stores the
     * file and returns the updated user, whose `imageUrl` is a host-relative path.
     */
    @Multipart
    @POST("users/me/image")
    suspend fun uploadUserImage(@Part file: MultipartBody.Part): Response<UserResponse>

    @GET("users")
    suspend fun listUsers(): Response<List<UserResponse>>

    @PUT("users/{id}/role")
    suspend fun updateUserRole(
        @Path("id") id: Long,
        @Body body: UpdateRoleRequest,
    ): Response<UserResponse>

    // ---------- Books ----------

    @GET("books")
    suspend fun getBooks(
        @Query("search") search: String? = null,
        @Query("categoryId") categoryId: Long? = null,
    ): Response<List<BookResponse>>

    @GET("books/{id}")
    suspend fun getBook(@Path("id") id: Long): Response<BookResponse>

    /**
     * Uploads a cover image for an existing book (multipart, part name `file`). The server stores
     * the file and returns the updated book, whose `imageUrl` is a host-relative path.
     */
    @Multipart
    @POST("books/{id}/image")
    suspend fun uploadBookImage(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part,
    ): Response<BookResponse>

    @POST("books")
    suspend fun createBook(@Body body: BookRequest): Response<BookResponse>

    @PUT("books/{id}")
    suspend fun updateBook(@Path("id") id: Long, @Body body: BookRequest): Response<BookResponse>

    @DELETE("books/{id}")
    suspend fun deleteBook(@Path("id") id: Long): Response<MessageResponse>

    // ---------- Categories ----------

    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryResponse>>

    @POST("categories")
    suspend fun createCategory(@Body body: CategoryRequest): Response<CategoryResponse>

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: Long, @Body body: CategoryRequest): Response<CategoryResponse>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<MessageResponse>

    // ---------- Borrows ----------

    @POST("borrows")
    suspend fun createBorrow(@Body body: BorrowRequest): Response<BorrowResponse>

    @GET("borrows/my")
    suspend fun getMyBorrows(@Query("status") status: String? = null): Response<List<BorrowResponse>>

    @GET("borrows/{id}")
    suspend fun getBorrow(@Path("id") id: Long): Response<BorrowResponse>

    @POST("borrows/{id}/return")
    suspend fun returnBooks(@Path("id") id: Long, @Body body: ReturnRequest): Response<BorrowResponse>

    @GET("borrows")
    suspend fun getAllBorrows(@Query("status") status: String? = null): Response<List<BorrowResponse>>

    @POST("borrows/{id}/approve")
    suspend fun approveBorrow(@Path("id") id: Long): Response<BorrowResponse>

    @POST("borrows/{id}/reject")
    suspend fun rejectBorrow(@Path("id") id: Long): Response<BorrowResponse>

    @POST("borrows/{id}/reverse")
    suspend fun reverseBorrow(@Path("id") id: Long): Response<BorrowResponse>

    // ---------- Dashboard ----------

    @GET("admin/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    // ---------- Settings: OTP sender mailbox (admin only) ----------

    @GET("settings/mail-sender")
    suspend fun getMailSender(): Response<MailSenderResponse>

    @PUT("settings/mail-sender")
    suspend fun updateMailSender(@Body body: UpdateMailSenderRequest): Response<MailSenderResponse>

    @POST("settings/mail-sender/test")
    suspend fun sendTestEmail(@Body body: TestEmailRequest): Response<MessageResponse>
}
