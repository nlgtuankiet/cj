package com.rainyseason.cj

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.rainyseason.cj.common.notNull
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationTest {

    private lateinit var device: UiDevice
    private val BASIC_SAMPLE_PACKAGE = "com.rainyseason.cj"
    private val LAUNCH_TIMEOUT = 5000L

    @Before
    fun startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        val launcherPackage: String = device.launcherPackageName
        assertThat(launcherPackage, notNullValue())
        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            LAUNCH_TIMEOUT
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(
            BASIC_SAMPLE_PACKAGE
        ).notNull().apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        device.wait(
            Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)),
            LAUNCH_TIMEOUT
        )
    }

    @Test
    fun createWidgetTest() {
        createWidget()
        device.openNotification()
    }

    private fun createWidget() {
        val buttonId = "com.rainyseason.cj:id/manage_widget_preview_container"
        device.wait(Until.findObject(By.res(buttonId)), 3000)
        val widgetShortcutButton: UiObject = device.findObject(
            UiSelector().resourceId(buttonId)
        )
        widgetShortcutButton.click()

        device.wait(Until.findObject(By.textStartsWith("add")), 3000)
        val addToHomeScreen = device.findObject(
            UiSelector()
                .className("""android.widget.Button""".toRegex().pattern)
                .textStartsWith("add")
        )
        addToHomeScreen.click()

        val savePattern = """(?i)\bauto saved\b""".toRegex().pattern
        device.wait(Until.findObject(By.text(savePattern)), 3000)
        device.findObject(UiSelector().textMatches(savePattern)).click()
    }
}
