package com.stilllynnthecloset.sdcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ReceivedBroadcast {
    abstract val topic: String

    @Serializable
    data class AttributeReceivedBroadcast constructor(
        @SerialName("Topic")
        override val topic: String,
        @SerialName("Attributes")
        val attributes: Attributes,
        @SerialName("MainboardID")
        val mainboardId: MainboardId,
        @SerialName("TimeStamp")
        val timestamp: Long,
    ) : ReceivedBroadcast() {
        @Serializable
        data class Attributes constructor(
            @SerialName("Name")
            val name: String,
            @SerialName("MachineName")
            val machineName: String,
            @SerialName("BrandName")
            val brandName: String,
            @SerialName("ProtocolVersion")
            val protocolVersion: String,
            @SerialName("FirmwareVersion")
            val firmwareVersion: String,
            @SerialName("Resolution")
            val resolution: String,
            @SerialName("XYZsize")
            val xyzSize: String,
            @SerialName("MainboardIP")
            val mainboardIp: String,
            @SerialName("MainboardID")
            val mainboardId: MainboardId,
            @SerialName("SDCPStatus")
            val sdcpStatus: Int,
            @SerialName("MaximumCloudSDCPSercicesAllowed")
            val maximumCloudSDCPSercicesAllowed: Int,
            @SerialName("NumberOfCloudSDCPServicesConnected")
            val numberOfCloudSDCPServicesConnected: Int,
            @SerialName("NumberOfVideoStreamConnected")
            val numberOfVideoStreamConnected: Int,
            @SerialName("MaximumVideoStreamAllowed")
            val maximumVideoStreamAllowed: Int,
            @SerialName("NetworkStatus")
            val networkStatus: String,
            @SerialName("UsbDiskStatus")
            val usbDiskStatus: Int,
            @SerialName("Capabilities")
            val capabilities: List<String>,
            @SerialName("SupportFileType")
            val supportFileType: List<String>,
            @SerialName("DevicesStatus")
            val devicesStatus: DevicesStatus,
            @SerialName("ReleaseFilmMax")
            val releaseFilmMax: Long,
            @SerialName("CameraStatus")
            val cameraStatus: Int,
            @SerialName("RemainingMemory")
            val remainingMemory: Long,
            @SerialName("TLPNoCapPos")
            val tlpNoCapPos: Int,
            @SerialName("TLPStartCapPos")
            val tlpStartCapPos: Int,
            @SerialName("TLPInterLayers")
            val tlpInterLayers: Int,
        ) {
            @Serializable
            data class DevicesStatus constructor(
                @SerialName("TempSensorStatusOfUVLED")
                val tempSensorStatusOfUVLED: Int,
                @SerialName("LCDStatus")
                val lcdStatus: Int,
                @SerialName("SgStatus")
                val sgStatus: Int,
                @SerialName("ZMotorStatus")
                val zMotorStatus: Int,
                @SerialName("RotateMotorStatus")
                val rotateMotorStatus: Int,
                @SerialName("RelaseFilmState")
                val releaseFilmState: Int,
            )
        }
    }

    @Serializable
    data class StatusReceivedBroadcast constructor(
        @SerialName("Topic")
        override val topic: String,
        @SerialName("MainboardID")
        val mainboardId: MainboardId,
        @SerialName("TimeStamp")
        val timestamp: Long,
        @SerialName("Status")
        val status: StatusBroadcastBody
    ) : ReceivedBroadcast() {
        @Serializable(with = MachineStatus.MachineStatusSerializer::class)
        enum class MachineStatus {
            MACHINE_STATUS_IDLE, // (0),  // Idle
            MACHINE_STATUS_PRINTING, // (1), // Executing print task
            MACHINE_STATUS_FILE_TRANSFERRING, // (2), // File transfer in progress
            MACHINE_STATUS_EXPOSURE_TESTING, // (3), // Exposure test in progress
            MACHINE_STATUS_DEVICES_TESTING, // (4), //Device self-check in progress
            ;
            data object MachineStatusSerializer : EnumOrdinalSerializer<MachineStatus>(entries)
        }

        @Serializable(with = ErrorCode.ErrorCodeSerializer::class)
        enum class ErrorCode {
            PRINT_ERROR_NONE, // (0),  // Normal
            PRINT_ERROR_CHECK, // (1),  // File MD5 Check Failed
            PRINT_ERROR_FILEIO, // (2),  // File Read Failed
            PRINT_ERROR_INVLAID_RESOLUTION, // (3),  // Resolution Mismatch
            PRINT_ERROR_UNKNOWN_FORMAT, // (4),  // Format Mismatch
            PRINT_ERROR_UNKNOWN_MODEL, // (5),  // Machine Model Mismatch
            ;
            data object ErrorCodeSerializer : EnumOrdinalSerializer<ErrorCode>(entries)
        }

        @Serializable
        data class StatusBroadcastBody constructor(
            @SerialName("CurrentStatus")
            val currentStatus: List<MachineStatus>, // Enum
            @SerialName("ReleaseFilm")
            val releaseFilm: Long,
            @SerialName("TempOfUVLED")
            val tempOfUvLed: Double,
            @SerialName("TimeLapseStatus")
            val timelapseStatus: Int, // Enum
            @SerialName("PrintInfo")
            val printInfo: PrintInfo,
        ) {
            @Serializable
            data class PrintInfo constructor(
                @SerialName("Status")
                val status: PrintStatus,
                @SerialName("CurrentLayer")
                val currentLayer: Long,
                @SerialName("TotalLayer")
                val totalLayer: Long,
                @SerialName("CurrentTicks")
                val currentTicks: Long,
                @SerialName("TotalTicks")
                val totalTicks: Long,
                @SerialName("ErrorNumber")
                val errorNumber: ErrorCode,
                @SerialName("Filename")
                val filename: String,
                @SerialName("TaskId")
                val taskId: String,
            )

            @Serializable(with = PrintStatus.PrintStatusSerializer::class)
            enum class PrintStatus {
                PRINT_STATUS_IDLE, // 0  // Idle
                PRINT_STATUS_HOMING, // 1  // Resetting
                PRINT_STATUS_DROPPING, // 2  // Descending
                PRINT_STATUS_EXPOSING, // 3  // Exposing
                PRINT_STATUS_LIFTING, // 4  // Lifting
                PRINT_STATUS_PAUSING, // 5  // Executing Pause Action
                PRINT_STATUS_PAUSED, // 6  // Suspended
                PRINT_STATUS_STOPPING, // 7  // Executing Stop Action
                PRINT_STATUS_STOPPED, // 8  // Stopped
                PRINT_STATUS_COMPLETE, // 9  // Print Completed
                PRINT_STATUS_FILE_CHECKING, // 10 // File Checking in Progress
                ;
                data object PrintStatusSerializer : EnumOrdinalSerializer<PrintStatus>(entries)
            }
        }
    }

    sealed class ResponseReceivedBroadcast : ReceivedBroadcast() {
        abstract val id: MachineId
        abstract val data: ResponseData

        @Serializable
        data class VideoResponseReceivedBroadcast constructor(
            @SerialName("Topic")
            override val topic: String,
            @SerialName("Id")
            override val id: MachineId,
            @SerialName("Data")
            override val data: ResponseData.VideoResponseData,
        ): ResponseReceivedBroadcast()

        @Serializable
        sealed class ResponseData {
            abstract val cmd: Int
            abstract val dataBody: ResponseDataBody
            abstract val requestId: String
            abstract val mainboardId: MainboardId
            abstract val timestamp: Long

            @Serializable
            data class VideoResponseData constructor(
                @SerialName("Cmd")
                override val cmd: Int,
                @SerialName("Data")
                override val dataBody: ResponseDataBody.VideoResponseDataBody,
                @SerialName("RequestID")
                override val requestId: String,
                @SerialName("MainboardID")
                override val mainboardId: MainboardId,
                @SerialName("TimeStamp")
                override val timestamp: Long,
            ) : ResponseData()

            @Serializable
            sealed class ResponseDataBody {
                abstract val ack: Int

                @Serializable
                data class VideoResponseDataBody constructor(
                    @SerialName("Ack")
                    override val ack: Int,
                    @SerialName("VideoUrl")
                    val videoUrl: String? = null, // Null if stream cannot be opened
                ) : ResponseDataBody()
            }
        }
    }
}
