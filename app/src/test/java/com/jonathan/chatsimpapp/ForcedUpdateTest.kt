package com.jonathan.chatsimpapp

import android.app.Application
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.jonathan.chatsimpapp.update.ForcedUpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

/** La actualización obligatoria con FakeAppUpdateManager (sin Play Store). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ForcedUpdateTest {

    private fun setUp(minVersion: Long = 0, configure: (FakeAppUpdateManager) -> Unit = {}): Triple<ComponentActivity, FakeAppUpdateManager, ForcedUpdateManager> {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create()
        val activity = controller.get()
        val fake = FakeAppUpdateManager(activity).also(configure)
        val forced = ForcedUpdateManager(activity, { minVersion }, {}, installedVersionCode = 2, manager = fake)
        controller.start()
        return Triple(activity, fake, forced)
    }

    @Test
    fun launchesImmediateFlowWhenUpdateIsAvailable() {
        val (_, fake, forced) = setUp { it.setUpdateAvailable(3, AppUpdateType.IMMEDIATE) }
        forced.onCreate()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(fake.isImmediateFlowVisible)
        assertFalse(forced.blocked.value)
    }

    @Test
    fun doesNothingWithoutUpdate() {
        val (_, fake, forced) = setUp { it.setUpdateNotAvailable() }
        forced.onCreate()
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(fake.isImmediateFlowVisible)
        assertFalse(forced.blocked.value)
    }

    @Test
    fun blocksWhenBelowRemoteConfigMinimum() {
        val (_, _, forced) = setUp(minVersion = 5) { it.setUpdateNotAvailable() }
        forced.onCreate()
        assertTrue(forced.blocked.value)
    }
}
