package com.nexa.finance

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class StartupPerformanceTest {

    @Test
    fun coldStartMedianIsRecorded() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val packageName = context.packageName
        val launcherIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        assertNotNull("Launcher activity is not available.", launcherIntent)

        val samples = buildList {
            repeat(5) {
                device.executeShellCommand("am force-stop " + packageName)
                val intent = Intent(launcherIntent).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }

                val startNanos = SystemClock.elapsedRealtimeNanos()
                context.startActivity(intent)

                val ready = device.wait(
                    Until.hasObject(By.pkg(packageName)),
                    TimeUnit.SECONDS.toMillis(10),
                )
                val elapsedMillis =
                    TimeUnit.NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - startNanos)

                assertTrue("NEXA failed to reach its package window within 10s.", ready)
                add(elapsedMillis)

                device.executeShellCommand("am force-stop " + packageName)
            }
        }

        val ordered = samples.sorted()
        val median = ordered[ordered.lastIndex / 2]
        val average = samples.average()

        println(
            "NEXA_STARTUP_COLD_MS samples=" + samples +
                " median_ms=" + median +
                " average_ms=" + String.format(Locale.US, "%.1f", average)
        )
    }
}
