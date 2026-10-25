package com.stilllynnthecloset.sdcp

import com.stilllynnthecloset.sdcp.ReceivedBroadcast.AttributeReceivedBroadcast
import com.stilllynnthecloset.sdcp.ReceivedBroadcast.ResponseReceivedBroadcast
import com.stilllynnthecloset.sdcp.ReceivedBroadcast.ResponseReceivedBroadcast.VideoResponseReceivedBroadcast
import com.stilllynnthecloset.sdcp.ReceivedBroadcast.StatusReceivedBroadcast
import com.stilllynnthecloset.sdcp.Request.RequestData
import com.stilllynnthecloset.sdcp.Request.RequestData.RequestDataBody
import com.stilllynnthecloset.sdcp.Request.RequestData.RequestDataBody.AttributesBody
import com.stilllynnthecloset.sdcp.Request.RequestData.RequestDataBody.ContinuePrintingBody
import com.stilllynnthecloset.sdcp.Request.RequestData.RequestDataBody.EnableVideoBody
import com.stilllynnthecloset.sdcp.Request.RequestData.RequestDataBody.PausePrintingBody
import com.stilllynnthecloset.sdcp.Request.RequestData.RequestDataBody.StartPrintingBody
import com.stilllynnthecloset.sdcp.Request.RequestData.RequestDataBody.StatusBody
import com.stilllynnthecloset.sdcp.Request.RequestData.RequestDataBody.StopPrintingBody
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.core.toByteArray
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

class SdcpClient {
    companion object {
        private const val CMD_REFRESH_STATUS = 0
        private const val CMD_REFRESH_ATTRIBUTE = 1
        private const val CMD_START_PRINTING = 128
        private const val CMD_PAUSE_PRINTING = 129
        private const val CMD_STOP_PRINTING = 130
        private const val CMD_CONTINUE_PRINTING = 131
        private const val CMD_STOP_FEEDING_MATERIAL = 132
        private const val CMD_SKIP_PREHEATING = 133
        private const val CMD_CHANGE_PRINTER_NAME = 192
        private const val CMD_RETRIEVE_FILE_LIST = 258
        private const val CMD_DELETE_FILES = 259
        private const val CMD_RETRIEVE_TASKS = 320
        private const val CMD_RETRIEVE_TASK_DETAILS = 321
        private const val CMD_ENABLE_VIDEO = 386
        private const val CMD_ENABLE_TIMELAPSE = 387
    }

    private var selectorManager: SelectorManager? = null
    private var serverSocket: BoundDatagramSocket? = null

    private val sessions = mutableMapOf<MainboardId, ClientWebSocketSession?>()

    public suspend fun initialize() {
        println("Initialize called")
        if (selectorManager != null) {
            throw IllegalStateException("Client has already been initialized")
        }
        if (serverSocket != null) {
            throw IllegalStateException("Client has already been initialized")
        }
        selectorManager = SelectorManager(Dispatchers.IO).also {
            serverSocket = aSocket(it)
                .udp()
                .bind("0.0.0.0", 3000) {
                    broadcast = true
                }
        }
    }

    public fun destroy() {
        serverSocket?.close()
        selectorManager?.close()
        serverSocket = null
        selectorManager = null
    }

    public suspend fun startWebsocketListener(broadcastData: BroadcastResponse, onMessageReceived: (receivedBroadcast: ReceivedBroadcast) -> Unit) {
        val client = HttpClient(CIO) {
            install(WebSockets) {
                pingIntervalMillis = 20_000
            }
        }
        val session = client.webSocketSession(urlString = "ws://${broadcastData.data.mainboardIp}:3030/websocket")
        sessions[broadcastData.data.mainboardId] = session
        // Collect the channel last since it suspends until the closed.
        CoroutineScope(Dispatchers.IO).launch {
            session.incoming.receiveAsFlow().collect {
                val othersMessage = it as? Frame.Text
                if (othersMessage != null) {
                    processWebsocketMessage(othersMessage, onMessageReceived)
                }
            }
        }
    }

    private suspend fun processWebsocketMessage(text: Frame.Text, onMessageReceived: (receivedBroadcast: ReceivedBroadcast) -> Unit) {
        val message = text.readText()

        val element = Json.parseToJsonElement(message)

        val topic = when (element) {
            is JsonObject -> {
                (element["Topic"] as? JsonPrimitive)?.contentOrNull
            }
            else -> null
        }
        val topicType = topic?.split("/")[1]
        val response = when (topicType) {
            "status" -> Json.decodeFromString<StatusReceivedBroadcast>(message)
            "attributes" -> Json.decodeFromString<AttributeReceivedBroadcast>(message)
            "response" -> parseResponse(element)
            "error" -> null
            "notice" -> null
            else -> null.also { println("unknown topic $topic") }
        }

        if (response != null) {
            onMessageReceived(response)
        } else {
            println("Failed to parse message: $message")
        }
    }

    private fun parseResponse(element: JsonElement): ResponseReceivedBroadcast? {
        if (element !is JsonObject) {
            return null
        }

        val command = element.get("Data")?.jsonObject?.get("Cmd")?.jsonPrimitive?.int
        return when (command) {
            CMD_REFRESH_STATUS -> null
            CMD_REFRESH_ATTRIBUTE -> null
            CMD_START_PRINTING -> null
            CMD_PAUSE_PRINTING -> null
            CMD_STOP_PRINTING -> null
            CMD_CONTINUE_PRINTING -> null
            CMD_STOP_FEEDING_MATERIAL -> null
            CMD_SKIP_PREHEATING -> null
            CMD_CHANGE_PRINTER_NAME -> null
            CMD_RETRIEVE_FILE_LIST -> null
            CMD_DELETE_FILES -> null
            CMD_RETRIEVE_TASKS -> null
            CMD_RETRIEVE_TASK_DETAILS -> null
            CMD_ENABLE_VIDEO -> Json.decodeFromJsonElement<VideoResponseReceivedBroadcast>(element)
            CMD_ENABLE_TIMELAPSE -> null
            else -> {
                println("Received response message with unknown command: $command, response: $element")
                null
            }
        }
    }

    public suspend fun sendBroadcast() {
        println("Sending M99999")
        val socket = serverSocket
            ?: throw IllegalStateException("Client must be initialized before broadcasts can be sent")

        val broadcastAddress = InetSocketAddress("192.168.1.255", 3000)
        socket.send(Datagram(ByteReadPacket("M99999".toByteArray()), broadcastAddress))
    }

    public suspend fun startBroadcastListener(onResponse: suspend (address: String, message: BroadcastResponse) -> Unit) {
        val socket = serverSocket
            ?: throw IllegalStateException("Client must be initialized before broadcasts can be observed")

        try {
            while (true) {
                if (!currentCoroutineContext().isActive) {
                    println("Context is no longer active, exiting listener")
                }
                val datagram = socket.receive()
                println("Received broadcast")
                val message = datagram.packet.readText()
                if (message == "M99999") {
                    println("M99999")
                    // Ignore, it's just someone else who is also reaching out to the printer
                    continue
                }
                val broadcast = try {
                    Json.decodeFromString<BroadcastResponse>(message)
                } catch (e: Exception) {
                    println("Error parsing broadcast message: $message")
                    e.printStackTrace()
                    null
                }
                if (broadcast != null) {
                    onResponse(datagram.address.toString(), broadcast)
                } else {
                    println("Could not parse broadcast $message")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun sendRequest(
        cmd: Int,
        data: RequestDataBody,
        machineId: MachineId,
        mainboardId: MainboardId,
    ) {
        val request = Request(
            id = machineId,
            topic = "sdcp/request/${mainboardId}",
            data = RequestData(
                cmd = cmd,
                data = data,
                requestId = "", // What to put here?
                mainboardId = mainboardId,
                timestamp = Clock.System.now().epochSeconds,
                from = 0, // What to put here?
            )
        )
        println("Sending $request")
        sessions[mainboardId]?.outgoing?.send(Frame.Text(Json.encodeToString(request)))
    }

    public suspend fun refreshStatus(machineId: MachineId, mainboardId: MainboardId) {
        sendRequest(
            cmd = CMD_REFRESH_STATUS,
            data = StatusBody,
            machineId = machineId,
            mainboardId = mainboardId,
        )
    }

    public suspend fun refreshAttributes(machineId: MachineId, mainboardId: MainboardId) {
        sendRequest(
            cmd = CMD_REFRESH_ATTRIBUTE,
            data = AttributesBody,
            machineId = machineId,
            mainboardId = mainboardId,
        )
    }

    public suspend fun enableVideoStream(machineId: MachineId, mainboardId: MainboardId, enable: Boolean) {
        sendRequest(
            cmd = CMD_ENABLE_VIDEO,
            data = EnableVideoBody(if (enable) 1 else 0),
            machineId = machineId,
            mainboardId = mainboardId,
        )
    }

    public suspend fun startPrinting(machineId: MachineId, mainboardId: MainboardId, filename: String, startLayer: Int) {
        sendRequest(
            cmd = CMD_START_PRINTING,
            data = StartPrintingBody(filename, startLayer),
            machineId = machineId,
            mainboardId = mainboardId,
        )
    }

    public suspend fun pausePrinting(machineId: MachineId, mainboardId: MainboardId) {
        sendRequest(
            cmd = CMD_PAUSE_PRINTING,
            data = PausePrintingBody,
            machineId = machineId,
            mainboardId = mainboardId,
        )
    }

    public suspend fun stopPrinting(machineId: MachineId, mainboardId: MainboardId) {
        sendRequest(
            cmd = CMD_STOP_PRINTING,
            data = StopPrintingBody,
            machineId = machineId,
            mainboardId = mainboardId,
        )
    }

    public suspend fun continuePrinting(machineId: MachineId, mainboardId: MainboardId) {
        sendRequest(
            cmd = CMD_CONTINUE_PRINTING,
            data = ContinuePrintingBody,
            machineId = machineId,
            mainboardId = mainboardId,
        )
    }
}
