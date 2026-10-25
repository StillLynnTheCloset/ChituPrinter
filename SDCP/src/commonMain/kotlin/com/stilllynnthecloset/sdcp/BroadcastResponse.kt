package com.stilllynnthecloset.sdcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BroadcastResponse constructor(
    @SerialName("Id")
    val machineId: MachineId,
    @SerialName("Data")
    val data: BroadcastResponseData,
) {
    @Serializable
    data class BroadcastResponseData constructor(
        @SerialName("Name")
        val name: String,
        @SerialName("MachineName")
        val machineName: String,
        @SerialName("BrandName")
        val brandName: String,
        @SerialName("MainboardIP")
        val mainboardIp: String,
        @SerialName("MainboardID")
        val mainboardId: MainboardId,
        @SerialName("ProtocolVersion")
        val protocolVersion: String,
        @SerialName("FirmwareVersion")
        val firmwareVersion: String,
    )
}
