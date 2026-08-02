package com.smartlibrary.notify

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 */
object ReminderScheduler {


        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        )
    }

    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
        )
    }

    fun cancel(context: Context) {
        }
    }
    }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            )
        }
    }