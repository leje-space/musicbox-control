package space.leje.musicboxcontrol

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [MusicBoxControlWidgetConfigureActivity]
 */
class MusicBoxControlWidget : AppWidgetProvider() {
    private val appWidgetBaseUrls: HashMap<Int, String> = HashMap()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager,
                          appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            MusicBoxControlWidgetConfigureActivity.removeBaseUrl(context, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        context.startService(Intent(context, MopidyControlService::class.java))
    }

    override fun onDisabled(context: Context) {
        context.stopService(Intent(context, MopidyControlService::class.java))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.startsWith(ACTION_BUTTON_CLICK)) {
            if (MopidyControlService.isNetworkConnected(context)) {
                val action = intent.getStringExtra(MopidyControlService.EXTRA_ACTION)
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID)

                if (appWidgetBaseUrls[appWidgetId] == null) {
                    val baseUrl = MusicBoxControlWidgetConfigureActivity.loadBaseUrl(
                            context, appWidgetId)
                    if (baseUrl != null) {
                        appWidgetBaseUrls[appWidgetId] = baseUrl
                    }
                }

                if (appWidgetBaseUrls[appWidgetId] != null) {
                    val mopidyHandleIntent = Intent(context, MopidyControlService::class.java)
                    mopidyHandleIntent.action = MopidyControlService.ACTION_MOPIDY_HANDLE
                    mopidyHandleIntent.putExtra(MopidyControlService.EXTRA_ACTION, action)
                    mopidyHandleIntent.putExtra(
                            MopidyControlService.EXTRA_BASE_URL, appWidgetBaseUrls[appWidgetId])

                    context.startService(mopidyHandleIntent)
                }
                else {
                    // @TODO
                }
            }
            else {
                Toast.makeText(context,
                        context.getString(R.string.network_unavailable),
                        Toast.LENGTH_SHORT).show()
            }
        }

        super.onReceive(context, intent)
    }

    companion object {
        private val ACTION_BUTTON_CLICK = "space.leje.musicboxcontrol.action.buttonClick"

        private val buttons = mapOf(
                R.id.imageButton to MopidyControlService.ACTION_PLAY,
                R.id.imageButton2 to MopidyControlService.ACTION_PAUSE,
                R.id.imageButton3 to MopidyControlService.ACTION_STOP,
                R.id.imageButton4 to MopidyControlService.ACTION_VOLUME_UP,
                R.id.imageButton5 to MopidyControlService.ACTION_VOLUME_DOWN)

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int) {
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.music_box_control_widget)

            var requestCode = 0

            buttons.forEach {
                val intent = Intent(context, MusicBoxControlWidget::class.java)
                intent.action = ACTION_BUTTON_CLICK + appWidgetId
                intent.putExtra(MopidyControlService.EXTRA_ACTION, it.value)
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

                val pendingIntent = PendingIntent.getBroadcast(
                        context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)

                requestCode++

                views.setOnClickPendingIntent(it.key, pendingIntent)
            }

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

