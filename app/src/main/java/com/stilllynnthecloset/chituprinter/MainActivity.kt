package com.stilllynnthecloset.chituprinter

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.alexvas.rtsp.widget.RtspDataListener
import com.alexvas.rtsp.widget.RtspStatusListener
import com.alexvas.rtsp.widget.RtspSurfaceView
import com.stilllynnthecloset.chituprinter.ui.theme.ChituPrinterTheme
import com.stilllynnthecloset.sdcp.BroadcastResponse
import com.stilllynnthecloset.sdcp.MachineId
import com.stilllynnthecloset.sdcp.MainboardId
import com.stilllynnthecloset.sdcp.ReceivedBroadcast
import com.stilllynnthecloset.sdcp.ReceivedBroadcast.AttributeReceivedBroadcast
import com.stilllynnthecloset.sdcp.ReceivedBroadcast.ResponseReceivedBroadcast.VideoResponseReceivedBroadcast
import com.stilllynnthecloset.sdcp.ReceivedBroadcast.StatusReceivedBroadcast
import com.stilllynnthecloset.sdcp.ReceivedBroadcast.StatusReceivedBroadcast.MachineStatus
import com.stilllynnthecloset.sdcp.ReceivedBroadcast.StatusReceivedBroadcast.StatusBroadcastBody.PrintStatus
import com.stilllynnthecloset.sdcp.SdcpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class MainActivity : ComponentActivity() {
    companion object {
        private const val REFRESH_INTERVAL: Long = 10_000
    }

    val isRefreshing = mutableStateOf(false)

    val devices = mutableStateOf(mapOf<MainboardId, BroadcastResponse>())
    val statuses = mutableStateOf(mapOf<MainboardId, StatusReceivedBroadcast>())
    val attributes = mutableStateOf(mapOf<MainboardId, AttributeReceivedBroadcast>())
    val videos = mutableStateOf(mapOf<MainboardId, VideoResponseReceivedBroadcast>())

    val printers = mutableStateOf(listOf<PrinterViewData>())

    var client: SdcpClient? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChituPrinterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PrintersList(innerPadding, printers.value, isRefreshing.value)
                }
            }

            LaunchedEffect("broadcast") {
                if (!havePermission()) { // TODO: Also do the broadcast stuff after getting permission.
                    getPermission()
                } else {
                    initialize()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client?.destroy()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PrintersList(innerPadding: PaddingValues, printersList: List<PrinterViewData>, isRefreshing: Boolean) {
        Column(Modifier.padding(innerPadding)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing, // TODO: How do we know when we're done refreshing? We just send a broadcast, and then can get any number of responses.
                onRefresh = {
                    CoroutineScope(Dispatchers.IO).launch {
                        this@MainActivity.isRefreshing.value = true
                        client?.sendBroadcast()
                        delay(500)
                        withContext(Dispatchers.Main) {
                            this@MainActivity.isRefreshing.value = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                state = rememberPullToRefreshState(),
                contentAlignment = Alignment.TopStart,
            ) {
                if (printersList.isEmpty()) {
                    Text(text = "No Printers Found")
                } else {
                    LazyColumn {
                        printersList.forEach {
                            item {
                                Printer(it)
                            }
                        }
                    }
//                    printersList.map { it.videoUrl }.firstOrNull()?.let {
//                        RtspView(it.toUri())
//                        vlcView(it.toUri())
//                    }
                }
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect("refresh") {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    println("Refreshing $client")

//                        client?.sendBroadcast() // We don't need to check for new printers every X seconds, just leave that to pull-to-refresh
                    printers.value.forEach { refreshInfo(it.machineId, it.mainboardId) }
                    delay(REFRESH_INTERVAL)
                }
            }
        }
    }

    @Composable
    private fun Printer(printerViewData: PrinterViewData) {
        Column {
            printerViewData.run {
                Text(text = "$name")
                Text(text = "Layer ${currentLayer}/${totalLayers}")
                Text(text = "Film ${releaseFilm}/${releaseFilmTotal}")
                Text(text = "Tick? ${currentTicks}/${totalTicks}")
                Text(text = "File $filename")
                Text(text = "Machine Status $machineStatus")
                Text(text = "Print Status $printStatus")
                Text(text = "Temp $tempOfUvLed")

                Button(
                    onClick = {
                        refreshInfo(printerViewData.machineId, printerViewData.mainboardId)
                    },
                ) {
                    Text("Refresh")
                }
            }
        }
    }

    private fun havePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) == PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == PERMISSION_GRANTED
        } else {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
        }
    }

    private fun getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            this.requestPermissions(arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK), 1)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            this.requestPermissions(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES), 2)
        } else {
            this.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 3)
        }
    }

    // Model stuff

    private fun merge(
        devices: Map<MainboardId, BroadcastResponse>,
        attributes: Map<MainboardId, AttributeReceivedBroadcast>,
        statuses: Map<MainboardId, StatusReceivedBroadcast>,
        videos: Map<MainboardId, VideoResponseReceivedBroadcast>,
    ) : List<PrinterViewData> {
        val mainBoards = (devices.keys + statuses.keys + videos.keys)

        val printers = mainBoards.map { mainboardId ->
            val device = devices[mainboardId]
            val status = statuses[mainboardId]
            val attribute = attributes[mainboardId]
            val video = videos[mainboardId]

            PrinterViewData(
                mainboardId = mainboardId,
                machineId = device?.machineId ?: video?.id ?: MachineId(""),
                name = device?.data?.name,
                machineStatus = status?.status?.currentStatus?.firstOrNull(),
                printStatus = status?.status?.printInfo?.status,
                currentLayer = status?.status?.printInfo?.currentLayer,
                totalLayers = status?.status?.printInfo?.totalLayer,
                currentTicks = status?.status?.printInfo?.currentTicks,
                totalTicks = status?.status?.printInfo?.totalTicks,
                filename = status?.status?.printInfo?.filename,
                releaseFilm = status?.status?.releaseFilm,
                releaseFilmTotal = attribute?.attributes?.releaseFilmMax,
                tempOfUvLed = status?.status?.tempOfUvLed,
                videoUrl = video?.data?.dataBody?.videoUrl,
            ).also { println(it) }
        }
        return printers
    }

    private fun refreshInfo(machineId: MachineId, mainboardId: MainboardId) {
        CoroutineScope(Dispatchers.IO).launch {
            println("Refreshing info $client")
            client?.refreshAttributes(machineId, mainboardId)
            client?.refreshStatus(machineId, mainboardId)
            client?.enableVideoStream(machineId, mainboardId, true)
        }
    }

    private fun onStatusUpdated(status: StatusReceivedBroadcast){
        statuses.value += (status.mainboardId to status)
        printers.value = merge(devices.value, attributes.value, statuses.value, videos.value)
    }

    private fun onAttributesUpdated(attribute: AttributeReceivedBroadcast) {
        attributes.value += (attribute.mainboardId to attribute)
        printers.value = merge(devices.value, attributes.value, statuses.value, videos.value)
    }

    private fun onVideoUpdated(videoResponse: VideoResponseReceivedBroadcast) {
        videos.value += (videoResponse.data.mainboardId to videoResponse)
        printers.value = merge(devices.value, attributes.value, statuses.value, videos.value)
    }

    private fun onDeviceUpdated(broadcastResponse: BroadcastResponse) {
        devices.value += (broadcastResponse.data.mainboardId to broadcastResponse)
        printers.value = merge(devices.value, attributes.value, statuses.value, videos.value)
    }

    private suspend fun initialize() {
        if (client != null) {
            throw IllegalStateException("Initializing from non-null state")
        }
        withContext(Dispatchers.IO) {
            try {
                println("Initializing client")
                val sdcpClient = SdcpClient()
                client = sdcpClient
                sdcpClient.initialize()
                sdcpClient.sendBroadcast()
                sdcpClient.startBroadcastListener(::onBroadcastReceived)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                println("Exited broadcast listener")
                client?.destroy()
                client = null
            }
        }
    }

    private suspend fun onBroadcastReceived(address: String, broadcast: BroadcastResponse) {
        Log.e("asdf", broadcast.toString())
        val hasBeenInitialized = devices.value.contains(broadcast.data.mainboardId)
        onDeviceUpdated(broadcast)
        if (!hasBeenInitialized) {
            client?.startWebsocketListener(broadcast, ::onWebsocketReceived)
            refreshInfo(broadcast.machineId, broadcast.data.mainboardId)
        }
    }

    private fun onWebsocketReceived(receivedBroadcast: ReceivedBroadcast) {
        Log.e("asdf", "Got websocket message: $receivedBroadcast")
        when (receivedBroadcast) {
            is StatusReceivedBroadcast -> onStatusUpdated(receivedBroadcast)
            is AttributeReceivedBroadcast -> onAttributesUpdated(receivedBroadcast)
            is VideoResponseReceivedBroadcast -> onVideoUpdated(receivedBroadcast)
        }
    }
}

data class PrinterViewData constructor(
    val mainboardId: MainboardId,
    val machineId: MachineId,
    val name: String?,
    val machineStatus: MachineStatus?,
    val printStatus: PrintStatus?,
    val currentLayer: Long?,
    val totalLayers: Long?,
    val currentTicks: Long?,
    val totalTicks: Long?,
    val filename: String?,
    val releaseFilm: Long?,
    val releaseFilmTotal: Long?,
    val tempOfUvLed: Double?,
    val videoUrl: String?,
)

@Composable
fun VlcView(uri: Uri) {
    AndroidView(
        modifier = Modifier.aspectRatio(16.0f/9.0f),
        factory = { context ->
            VLCVideoLayout(context).apply {
                val libVlc = LibVLC(context)
                val mediaPlayer = MediaPlayer(libVlc)
                println("Video URI: $uri")

                mediaPlayer.attachViews(this, null, false, false)

                val media: Media = Media(libVlc, uri)
                media.setHWDecoderEnabled(true, false)
                media.addOption(":network-caching=600")

                mediaPlayer.setMedia(media)
                media.release()
                mediaPlayer.play()
/*
                this.setDataListener(object : RtspDataListener {
                    override fun onRtspDataApplicationDataReceived(
                        data: ByteArray,
                        offset: Int,
                        length: Int,
                        timestamp: Long
                    ) {
                        super.onRtspDataApplicationDataReceived(data, offset, length, timestamp)
                        Log.e("asdf", "LYNN: onRtspDataApplicationDataReceived")
                    }

                    override fun onRtspDataAudioSampleReceived(
                        data: ByteArray,
                        offset: Int,
                        length: Int,
                        timestamp: Long
                    ) {
                        super.onRtspDataAudioSampleReceived(data, offset, length, timestamp)
                        Log.e("asdf", "LYNN: onRtspDataAudioSampleReceived")
                    }

                    override fun onRtspDataVideoNalUnitReceived(
                        data: ByteArray,
                        offset: Int,
                        length: Int,
                        timestamp: Long
                    ) {
                        super.onRtspDataVideoNalUnitReceived(data, offset, length, timestamp)
                        Log.e("asdf", "LYNN: onRtspDataVideoNalUnitReceived")
                    }
                })
                this.setStatusListener(object : RtspStatusListener {
                    override fun onRtspFirstFrameRendered() {
                        super.onRtspFirstFrameRendered()
                        Log.e("asdf", "LYNN: onRtspFirstFrameRendered")
                    }

                    override fun onRtspFrameSizeChanged(width: Int, height: Int) {
                        super.onRtspFrameSizeChanged(width, height)
                        Log.e("asdf", "LYNN: onRtspFrameSizeChanged")
                    }

                    override fun onRtspStatusConnected() {
                        super.onRtspStatusConnected()
                        Log.e("asdf", "LYNN: onRtspStatusConnected")
                    }

                    override fun onRtspStatusConnecting() {
                        super.onRtspStatusConnecting()
                        Log.e("asdf", "LYNN: onRtspStatusConnecting")
                    }

                    override fun onRtspStatusDisconnected() {
                        super.onRtspStatusDisconnected()
                        Log.e("asdf", "LYNN: onRtspStatusDisconnected")
                    }

                    override fun onRtspStatusDisconnecting() {
                        super.onRtspStatusDisconnecting()
                        Log.e("asdf", "LYNN: onRtspStatusDisconnecting")
                    }

                    override fun onRtspStatusFailed(message: String?) {
                        super.onRtspStatusFailed(message)
                        Log.e("asdf", "LYNN: onRtspStatusFailed")
                    }

                    override fun onRtspStatusFailedUnauthorized() {
                        super.onRtspStatusFailedUnauthorized()
                        Log.e("asdf", "LYNN: onRtspStatusFailedUnauthorized")
                    }
                })
                this.debug = true
                this.init(uri, socketTimeout = 30_000)
                this.start(
                    requestVideo = true,
                    requestAudio = true,
                    requestApplication = true,
                )
*/
            }
        },
        update = { playerView ->
            println("Video URI updated: $uri")
//            playerView.init(uri)
        }
    )
}

@Composable
fun RtspView(uri: Uri) {
    AndroidView(
        modifier = Modifier.aspectRatio(16.0f/9.0f),
        factory = { context ->
            RtspSurfaceView(context).apply {
                println("Video URI: $uri")
                this.setDataListener(object : RtspDataListener {
                    override fun onRtspDataApplicationDataReceived(
                        data: ByteArray,
                        offset: Int,
                        length: Int,
                        timestamp: Long
                    ) {
                        super.onRtspDataApplicationDataReceived(data, offset, length, timestamp)
                        Log.e("asdf", "LYNN: onRtspDataApplicationDataReceived")
                    }

                    override fun onRtspDataAudioSampleReceived(
                        data: ByteArray,
                        offset: Int,
                        length: Int,
                        timestamp: Long
                    ) {
                        super.onRtspDataAudioSampleReceived(data, offset, length, timestamp)
                        Log.e("asdf", "LYNN: onRtspDataAudioSampleReceived")
                    }

                    override fun onRtspDataVideoNalUnitReceived(
                        data: ByteArray,
                        offset: Int,
                        length: Int,
                        timestamp: Long
                    ) {
                        super.onRtspDataVideoNalUnitReceived(data, offset, length, timestamp)
                        Log.e("asdf", "LYNN: onRtspDataVideoNalUnitReceived")
                    }
                })
                this.setStatusListener(object : RtspStatusListener {
                    override fun onRtspFirstFrameRendered() {
                        super.onRtspFirstFrameRendered()
                        Log.e("asdf", "LYNN: onRtspFirstFrameRendered")
                    }

                    override fun onRtspFrameSizeChanged(width: Int, height: Int) {
                        super.onRtspFrameSizeChanged(width, height)
                        Log.e("asdf", "LYNN: onRtspFrameSizeChanged")
                    }

                    override fun onRtspStatusConnected() {
                        super.onRtspStatusConnected()
                        Log.e("asdf", "LYNN: onRtspStatusConnected")
                    }

                    override fun onRtspStatusConnecting() {
                        super.onRtspStatusConnecting()
                        Log.e("asdf", "LYNN: onRtspStatusConnecting")
                    }

                    override fun onRtspStatusDisconnected() {
                        super.onRtspStatusDisconnected()
                        Log.e("asdf", "LYNN: onRtspStatusDisconnected")
                    }

                    override fun onRtspStatusDisconnecting() {
                        super.onRtspStatusDisconnecting()
                        Log.e("asdf", "LYNN: onRtspStatusDisconnecting")
                    }

                    override fun onRtspStatusFailed(message: String?) {
                        super.onRtspStatusFailed(message)
                        Log.e("asdf", "LYNN: onRtspStatusFailed")
                    }

                    override fun onRtspStatusFailedUnauthorized() {
                        super.onRtspStatusFailedUnauthorized()
                        Log.e("asdf", "LYNN: onRtspStatusFailedUnauthorized")
                    }
                })
                this.debug = true
                this.init(uri, socketTimeout = 30_000)
                this.start(
                    requestVideo = true,
                    requestAudio = true,
                    requestApplication = true,
                )
            }
        },
        update = { playerView ->
            println("Video URI updated: $uri")
//            playerView.init(uri)
        }
    )
}
