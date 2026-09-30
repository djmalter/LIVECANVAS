package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.streaming.ConnectionState
import com.example.engine.streaming.FakeStreamingEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LiveCanvas Studio", appName)
  }

  @Test
  fun `stream config validation rejects invalid urls`() {
    val resultInvalid = FakeStreamingEngine.validateStreamConfig("http://invalid", "key123")
    assertTrue(resultInvalid.isFailure)

    val resultValid = FakeStreamingEngine.validateStreamConfig("rtmp://live.twitch.tv/app", "key123")
    assertTrue(resultValid.isSuccess)
  }

  @Test
  fun `fake streaming engine lifecycle`() = runBlocking {
    val engine = FakeStreamingEngine()
    assertEquals(ConnectionState.IDLE, engine.statsFlow.value.connectionState)

    val startRes = engine.startStream("rtmp://live.test.com/live", "secret_key", 1920, 1080, 30, 4500)
    assertTrue(startRes.isSuccess)
    assertEquals(ConnectionState.CONNECTED, engine.statsFlow.value.connectionState)
    assertTrue(engine.statsFlow.value.isLive)

    engine.stopStream()
    assertEquals(ConnectionState.DISCONNECTED, engine.statsFlow.value.connectionState)
  }
}
