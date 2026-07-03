package com.arslandaim.playtube

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import androidx.work.Configuration
import com.arslandaim.playtube.BuildConfig
import com.arslandaim.playtube.data.network.YouTubeDownloader
import androidx.hilt.work.HiltWorkerFactory
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.arslandaim.playtube.domain.repository.DownloadRepository
import com.arslandaim.playtube.utils.ConnectivityObserver
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import javax.inject.Inject

@HiltAndroidApp
class PlayTubeApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var downloadRepository: DownloadRepository
    @Inject lateinit var connectivityObserver: ConnectivityObserver

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: Context): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        // Initialize NewPipe Extractor
        NewPipe.init(YouTubeDownloader(okHttpClient))

        createNotificationChannel()
        observeConnectivity()
    }

    private fun observeConnectivity() {
        connectivityObserver.observe()
            .onEach { status ->
                when (status) {
                    ConnectivityObserver.Status.Available -> {
                        downloadRepository.resumeAllPausedDownloads()
                    }
                    ConnectivityObserver.Status.Lost, ConnectivityObserver.Status.Unavailable -> {
                        downloadRepository.pauseAllActiveDownloads()
                    }
                    else -> {}
                }
            }
            .launchIn(applicationScope)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Downloads"
            val descriptionText = "Shows download progress"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("download_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
