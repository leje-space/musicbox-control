package space.leje.musicboxcontrol

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class MopidyControlService : Service() {
    private val mopidyApis: HashMap<String, MopidyApi> = HashMap()

    private fun mopidyRun(baseUrl: String) {
        mopidyApis[baseUrl] = MopidyApi.create(baseUrl)
    }

    private fun mopidySend(baseUrl: String,
                           method: String,
                           params: Map<String, Any>? = null,
                           onSuccess: (JSONRPCResult) -> Unit = {}) {
        val mopidyApi = mopidyApis[baseUrl]
        if (mopidyApi != null) {
            mopidyApi.send(JSONRPCBody(method, params))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                result -> onSuccess(result)
                            },
                            {
                                error -> // @TODO
                            }
                    )
        }
        else {
            mopidyRun(baseUrl)
            mopidySend(baseUrl, method, params, onSuccess)
        }
    }

    private fun mopidyHandle(baseUrl: String, action: String) {
        when (action) {
            ACTION_PLAY -> mopidySend(baseUrl, "core.playback.play")
            ACTION_PAUSE -> mopidySend(baseUrl, "core.playback.pause")
            ACTION_STOP -> mopidySend(baseUrl, "core.playback.stop")
            in listOf(ACTION_VOLUME_UP, ACTION_VOLUME_DOWN) -> {
                mopidySend(baseUrl, "core.playback.get_volume", onSuccess = {
                    var volume = (it.result as Double).toInt()
                    when (action) {
                        ACTION_VOLUME_UP -> {
                            volume += VOLUME_INCREMENT
                            if (volume > VOLUME_MAX) {
                                volume = VOLUME_MAX
                            }
                        }
                        ACTION_VOLUME_DOWN -> {
                            volume -= VOLUME_INCREMENT
                            if (volume < VOLUME_MIN) {
                                volume = VOLUME_MIN
                            }
                        }
                    }
                    mopidySend(baseUrl, "core.playback.set_volume",
                            mapOf("volume" to volume))
                })
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_MOPIDY_HANDLE) {
            val baseUrl = intent.getStringExtra(EXTRA_BASE_URL)
            val action = intent.getStringExtra(EXTRA_ACTION)

            mopidyHandle(baseUrl, action)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        /*
        val notification = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .build()
        startForeground(NOTIFICATION_ID, notification)
        */
    }

    override fun onDestroy() {
    }

    companion object {
        internal val ACTION_PLAY = "space.leje.musicboxcontrol.action.play"
        internal val ACTION_PAUSE = "space.leje.musicboxcontrol.action.pause"
        internal val ACTION_STOP = "space.leje.musicboxcontrol.action.stop"
        internal val ACTION_VOLUME_UP = "space.leje.musicboxcontrol.action.volume_up"
        internal val ACTION_VOLUME_DOWN = "space.leje.musicboxcontrol.action.volume_down"

        private val VOLUME_INCREMENT = 10
        private val VOLUME_MIN = 0
        private val VOLUME_MAX = 100

        internal val ACTION_MOPIDY_HANDLE = "space.leje.musicboxcontrol.action.mopidyHandle"

        internal val EXTRA_ACTION = "space.leje.musicboxcontrol.extra.action"
        internal val EXTRA_BASE_URL = "space.leje.musicboxcontrol.extra.baseUrl"

        private val NOTIFICATION_ID = 1

        internal fun isNetworkConnected(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo?.isConnected ?: false
        }
    }
}
