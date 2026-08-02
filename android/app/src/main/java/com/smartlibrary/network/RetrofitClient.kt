package com.smartlibrary.network

import android.content.Context
import android.content.Intent
import com.smartlibrary.BuildConfig
import com.smartlibrary.LoginActivity
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a singleton [ApiService]. Requests automatically carry the Bearer token from
 * [SessionManager] when one is present.
 */
object RetrofitClient {

    @Volatile
    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: build(context.applicationContext).also { apiService = it }
        }
    }

    private fun build(context: Context): ApiService {
        val appContext = context.applicationContext
        val session = SessionManager(appContext)

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            session.token?.let { token ->
                if (token.isNotEmpty()) {
                    builder.header("Authorization", "Bearer $token")
                }
            }
            builder.header("Accept", "application/json")
            chain.proceed(builder.build())
        }

        // A 401 on a request that carried a token means that token was rejected
        // (expired/invalid) — as opposed to a 401 from /login with wrong credentials,
        // which never carries a token. Only the former should force a logout.
        val sessionExpiryInterceptor = Interceptor { chain ->
            val hadToken = !session.token.isNullOrEmpty()
            val response = chain.proceed(chain.request())
            if (response.code == 401 && hadToken) {
                session.clear()
                appContext.startActivity(
                    Intent(appContext, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, true)
                )
            }
            response
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(sessionExpiryInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
