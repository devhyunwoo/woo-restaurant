package com.example.wood_restaurant.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberExternalActions(): ExternalActions {
    val context = LocalContext.current
    return remember(context) { AndroidExternalActions(context) }
}

private class AndroidExternalActions(
    private val context: Context,
) : ExternalActions {

    override val appIdentifier: String get() = context.packageName

    override fun openUrl(url: String, fallbackUrl: String?) {
        if (launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))) return
        if (fallbackUrl != null) launch(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
    }

    override fun dial(phoneNumber: String) {
        // 숫자·+·- 만 남긴다. "02-123-4567" 그대로 넣어도 다이얼러가 처리하지만 안전하게.
        val cleaned = phoneNumber.filter { it.isDigit() || it == '+' || it == '-' }
        launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
    }

    override fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        launch(Intent.createChooser(send, null))
    }

    override fun openAppSettings() {
        launch(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
        )
    }

    /** 받을 앱이 없으면 예외 대신 false. Activity 컨텍스트가 아니면 NEW_TASK 플래그가 필요하다. */
    private fun launch(intent: Intent): Boolean = try {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
