package com.rainyseason.cj

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.rainyseason.cj.common.notNull
import com.rainyseason.cj.ticker.CoinTickerSettingActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.util.LinkedList
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class NotificationTest {

    private lateinit var device: UiDevice
    private val packageName = "com.rainyseason.cj"
    private val timeout = 10000L

    private fun UiDevice.waitAndLog(what: String, selector: SearchCondition<Boolean>) {
        Timber.d("wait for $what")
        val startTime = SystemClock.uptimeMillis()
        val result: Boolean? = wait(selector, timeout)
        assert(result == true) {
            "$what not found"
        }
        val endTime = SystemClock.uptimeMillis()
        Timber.d("success wait for in ${endTime - startTime} ms")
    }

    private fun String.asTextStartPattern(): Pattern {
        return """^$this.*$""".toRegex(RegexOption.IGNORE_CASE).toPattern()
    }

    private fun String.asRegexPattern(): Pattern {
        return toRegex(RegexOption.IGNORE_CASE).toPattern()
    }

    @Before
    fun startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.pressHome()

        val launcher: String = device.launcherPackageName
        device.waitAndLog(launcher, Until.hasObject(By.pkg(launcher).depth(0)))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(packageName).notNull()
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.waitAndLog(packageName, Until.hasObject(By.pkg(packageName).depth(0)))
        // SystemClock.sleep(30000) // warn up
    }

    @Test
    fun createWidgetTest() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation
            .addMonitor(CoinTickerSettingActivity::class.java.name, null, false)

        val buttonId = "com.rainyseason.cj:id/manage_widget_preview_container"
        device.waitAndLog("create widget shortcut", Until.hasObject(By.res(buttonId)))
        device.findObject(UiSelector().resourceId(buttonId)).click()

        device.waitAndLog("add button", Until.hasObject(By.text("add".asTextStartPattern())))
        device.findObject(
            UiSelector()
                .className("""android.widget.Button""".toRegex().pattern)
                .textStartsWith("add")
        ).click()

        instrumentation.waitForMonitor(monitor)

        device.waitAndLog(
            "save button",
            Until.hasObject(By.text("(auto )?saved?".asRegexPattern()))
        )
        device.findObject(By.text("(auto )?saved?".asRegexPattern())).click()

        device.openNotification()

        device.waitAndLog(
            "notification",
            Until.hasObject(By.text("(bitcoin|btc).*?".asRegexPattern()))
        )

        // how to improve this?
        val container = device.findObject(By.text("(bitcoin|btc).*?".asRegexPattern()))
            .parent.parent.parent.parent

        Timber.d("Parent is ${container.className} ${container.resourceName}")
        val allChild = container.allChildren()
        allChild.forEach {
            Timber.d("child: ${it.className} ${it.resourceName}")
        }
        container.allChildren().first { it.resourceName == "android:id/expand_button" }
            .click()
    }
}

fun UiObject2.allChildren(): List<UiObject2> {
    val queue = LinkedList<UiObject2>()
    val result = mutableListOf<UiObject2>()
    queue.offer(this)
    while (!queue.isEmpty()) {
        var count = queue.size
        while (count > 0) {
            val element = queue.poll()
            element?.children.orEmpty().forEach { child ->
                result.add(child)
                if (child.childCount > 0) {
                    queue.offer(child)
                } else {
                    result.add(child)
                }
            }
            count--
        }
    }
    return result
}
