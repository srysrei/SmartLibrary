package com.smartlibrary.network.dto

/**
 * Data-transfer objects mirroring the SmartLibrary Spring Boot API contract exactly.
 * Field names match the JSON keys produced/consumed by the server.
 */

// ---------- Auth / User ----------

data class LoginRequest(
    // The server field is usernameOrEmail — it accepts either a username or an email address.
    val usernameOrEmail: String,
    val password: String,
)

data class RegisterRequest(
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
)

data class ForgotPasswordRequest(
    // Server field is usernameOrEmail (accepts either); it emails the OTP to the account's address.
    val usernameOrEmail: String,
)

data class ForgotPasswordResponse(
    val message: String?,
    val otp: String?,
)

data class ResetPasswordRequest(
    val usernameOrEmail: String,
    val otp: String,
    val newPassword: String,
)

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
)

data class UpdateProfileRequest(
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String,
)

data class UpdateRoleRequest(
    val role: String,
)

data class UserResponse(
    val id: Long,
    val username: String?,
    val fullName: String,
    val email: String,
    val phone: String?,
    val role: String,
    val imageUrl: String?,
)

data class AuthResponse(
    val token: String,
    val user: UserResponse,
)

// ---------- Settings: OTP sender mailbox (table OTPSENDER, admin only) ----------

data class MailSenderResponse(
    val configured: Boolean,
    val email: String?,
)

data class UpdateMailSenderRequest(
    val email: String,
    val password: String,
)

data class TestEmailRequest(
    val to: String,
)

// ---------- Books ----------

data class BookResponse(
    val id: Long,
    val title: String,
    val author: String,
    val categoryId: Long,
    val categoryName: String?,
    val description: String?,
    val qty: Int,
    val availableQty: Int,
    val imageUrl: String?,
)

data class BookRequest(
    val title: String,
    val author: String,
    val categoryId: Long,
    val description: String,
    val qty: Int,
    val imageUrl: String?,
)

// ---------- Categories ----------

data class CategoryResponse(
    val id: Long,
    val name: String,
    val bookCount: Long = 0,
)

data class CategoryRequest(
    val name: String,
)

// ---------- Borrows ----------

data class BorrowRequest(
    val bookIds: List<Long>,
    val borrowDate: String,   // yyyy-MM-dd
    val borrowDay: Int,
    val description: String?,
)

data class ReturnRequest(
    val borrowBookIds: List<Long>,
)

data class BorrowBookResponse(
    val id: Long,
    val bookId: Long,
    val title: String?,
    val author: String?,
    val imageUrl: String?,
    val returned: Boolean,
    val returnDate: String?,
)

data class BorrowResponse(
    val id: Long,
    val userId: Long,
    val userName: String?,
    val borrowDate: String?,
    val borrowDay: Int,
    val description: String?,
    val firstReturn: String?,
    val lastReturn: String?,
    val status: String,
    val approvedBy: Long?,
    val items: List<BorrowBookResponse> = emptyList(),
)

// ---------- Dashboard / misc ----------

data class DashboardResponse(
    val totalBooks: Long,
    val totalUsers: Long,
    val pendingRequests: Long,
    val activeBorrows: Long,
)

data class MessageResponse(
    val message: String?,
)
