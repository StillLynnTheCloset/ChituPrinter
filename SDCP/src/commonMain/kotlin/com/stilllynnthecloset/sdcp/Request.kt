package com.stilllynnthecloset.sdcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Request constructor(
    @SerialName("Id")
    val id: MachineId,
    @SerialName("Data")
    val data: RequestData,
    @SerialName("Topic")
    val topic: String,
) {
    @Serializable
    data class RequestData constructor(
        @SerialName("Cmd")
        val cmd: Int,
        @SerialName("Data")
        val data: RequestDataBody,
        @SerialName("RequestID")
        val requestId: String,
        @SerialName("MainboardID")
        val mainboardId: MainboardId,
        @SerialName("TimeStamp")
        val timestamp: Long,
        @SerialName("From")
        val from: Int,
    ) {
        @Serializable
        sealed class RequestDataBody {
            @Serializable
            data object StatusBody : RequestDataBody()

            @Serializable
            data object AttributesBody : RequestDataBody()

            @Serializable
            data class EnableVideoBody constructor(
                @SerialName("Enable")
                val enable: Int, // 0: Disable, 1: Enable
            ) : RequestDataBody()

            @Serializable
            data class StartPrintingBody constructor(
                @SerialName("Filename")
                val filename: String,
                @SerialName("StartLayer")
                val startLayer: Int,
            ) : RequestDataBody()

            @Serializable
            data object PausePrintingBody : RequestDataBody()

            @Serializable
            data object StopPrintingBody : RequestDataBody()

            @Serializable
            data object ContinuePrintingBody : RequestDataBody()
        }
    }
}
