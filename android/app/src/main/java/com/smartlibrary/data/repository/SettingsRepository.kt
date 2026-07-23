package com.smartlibrary.data.repository

import android.content.Context
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.RetrofitClient
import com.smartlibrary.network.dto.MailSenderResponse
import com.smartlibrary.network.dto.MessageResponse
import com.smartlibrary.network.dto.TestEmailRequest
import com.smartlibrary.network.dto.UpdateMailSenderRequest
import com.smartlibrary.network.safeApiCall

/**
 * Admin-only management of the OTP-sender mailbox (table OTPSENDER): the address and
 * password the API sends password-reset codes from. All endpoints require the ADMIN role.
 */
class SettingsRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun getMailSender(): ApiResult<MailSenderResponse> =
        safeApiCall { api.getMailSender() }

    suspend fun updateMailSender(email: String, password: String): ApiResult<MailSenderResponse> =
        safeApiCall { api.updateMailSender(UpdateMailSenderRequest(email, password)) }

    suspend fun sendTestEmail(to: String): ApiResult<MessageResponse> =
        safeApiCall { api.sendTestEmail(TestEmailRequest(to)) }
}
