package com.example.engine.streaming

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StreamStats(
    val isLive: Boolean = false,
    val bitrateKbps: Int = 0,
    val fps: Float = 0f,
    val droppedFrames: Long = 0,
    val durationSeconds: Long = 0,
    val connectionState: ConnectionState = ConnectionState.IDLE,
    val serverHost: String = "",
    val errorMessage: String? = null
)

enum class ConnectionState {
    IDLE, CONNECTING, CONNECTED, RECONNECTING, FAILED, DISCONNECTED
}

interface StreamingEngine {
    val statsFlow: StateFlow<StreamStats>
    suspend fun startStream(
        serverUrl: String,
        streamKey: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int
    ): Result<Unit>
    suspend fun stopStream()
}

class FakeStreamingEngine : StreamingEngine {
    private val _statsFlow = MutableStateFlow(StreamStats())
    override val statsFlow: StateFlow<StreamStats> = _statsFlow.asStateFlow()

    private var streamJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    override suspend fun startStream(
        serverUrl: String,
        streamKey: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int
    ): Result<Unit> {
        val validation = validateStreamConfig(serverUrl, streamKey)
        if (validation.isFailure) {
            _statsFlow.value = StreamStats(
                connectionState = ConnectionState.FAILED,
                errorMessage = validation.exceptionOrNull()?.message ?: "Invalid configuration"
            )
            return validation
        }

        val host = runCatching {
            val stripped = serverUrl.removePrefix("rtmp://").removePrefix("rtmps://")
            stripped.substringBefore("/")
        }.getOrDefault("Unknown Server")

        _statsFlow.value = StreamStats(
            isLive = false,
            connectionState = ConnectionState.CONNECTING,
            serverHost = host
        )

        delay(900) // Simulated connection handshake

        _statsFlow.value = StreamStats(
            isLive = true,
            bitrateKbps = bitrateKbps,
            fps = fps.toFloat(),
            droppedFrames = 0,
            durationSeconds = 0,
            connectionState = ConnectionState.CONNECTED,
            serverHost = host
        )

        streamJob?.cancel()
        streamJob = scope.launch {
            var elapsed = 0L
            var dropped = 0L
            while (isActive) {
                delay(1000)
                elapsed++
                if (elapsed % 7L == 0L && Math.random() < 0.2) {
                    dropped += (1..3).random()
                }
                val fluctuation = ((-80..80).random())
                _statsFlow.value = _statsFlow.value.copy(
                    durationSeconds = elapsed,
                    bitrateKbps = (bitrateKbps + fluctuation).coerceAtLeast(100),
                    droppedFrames = dropped
                )
            }
        }
        return Result.success(Unit)
    }

    override suspend fun stopStream() {
        streamJob?.cancel()
        streamJob = null
        _statsFlow.value = StreamStats(
            isLive = false,
            bitrateKbps = 0,
            fps = 0f,
            droppedFrames = _statsFlow.value.droppedFrames,
            durationSeconds = _statsFlow.value.durationSeconds,
            connectionState = ConnectionState.DISCONNECTED,
            serverHost = _statsFlow.value.serverHost
        )
    }

    companion object {
        fun validateStreamConfig(url: String, key: String): Result<Unit> {
            val trimmedUrl = url.trim()
            if (trimmedUrl.isBlank()) {
                return Result.failure(IllegalArgumentException("Stream server URL cannot be empty."))
            }
            if (!trimmedUrl.startsWith("rtmp://") && !trimmedUrl.startsWith("rtmps://")) {
                return Result.failure(IllegalArgumentException("Server URL must begin with rtmp:// or rtmps://"))
            }
            if (key.trim().isBlank()) {
                return Result.failure(IllegalArgumentException("Stream key cannot be empty."))
            }
            return Result.success(Unit)
        }
    }
}
