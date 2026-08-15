package com.example.wood_restaurant.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberExternalActions(): ExternalActions = remember { IosExternalActions() }

private class IosExternalActions : ExternalActions {

    override val appIdentifier: String
        get() = NSBundle.mainBundle.bundleIdentifier ?: ""

    override fun openUrl(url: String, fallbackUrl: String?) {
        val primary = NSURL.URLWithString(url)
        if (primary == null) {
            openFallback(fallbackUrl)
            return
        }
        // 앱 미설치 등으로 못 열면 completion에 false가 온다. 그때 폴백을 시도한다.
        UIApplication.sharedApplication.openURL(
            url = primary,
            options = emptyMap<Any?, Any>(),
            completionHandler = { success -> if (!success) openFallback(fallbackUrl) },
        )
    }

    override fun dial(phoneNumber: String) {
        val cleaned = phoneNumber.filter { it.isDigit() || it == '+' || it == '-' }
        openUrl("tel:$cleaned")
    }

    override fun share(text: String) {
        val presenter = topViewController() ?: return
        val controller = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        // iPad에서는 팝오버 anchor가 없으면 크래시난다.
        controller.popoverPresentationController?.sourceView = presenter.view
        presenter.presentViewController(controller, animated = true, completion = null)
    }

    override fun openAppSettings() {
        openUrl(UIApplicationOpenSettingsURLString)
    }

    private fun openFallback(fallbackUrl: String?) {
        val url = fallbackUrl?.let { NSURL.URLWithString(it) } ?: return
        UIApplication.sharedApplication.openURL(
            url = url,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }

    /** 현재 화면 맨 위에 떠 있는 뷰컨트롤러. 공유 시트를 여기서 present 한다. */
    private fun topViewController(): UIViewController? {
        val app = UIApplication.sharedApplication
        val window = app.keyWindow
            ?: app.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
            ?: app.windows.firstOrNull() as? UIWindow
        var top = window?.rootViewController ?: return null
        while (true) {
            top = top.presentedViewController ?: return top
        }
    }
}
