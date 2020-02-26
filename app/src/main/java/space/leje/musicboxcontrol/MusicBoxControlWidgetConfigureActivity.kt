package space.leje.musicboxcontrol

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.rafakob.nsdhelper.NsdHelper
import com.rafakob.nsdhelper.NsdListener
import com.rafakob.nsdhelper.NsdService
import com.rafakob.nsdhelper.NsdType

/**
 * The configuration screen for the [MusicBoxControlWidget] AppWidget.
 */
class MusicBoxControlWidgetConfigureActivity : AppCompatActivity(), NsdListener {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var nsdHelper: NsdHelper
    private lateinit var foundHttpServicesList: ArrayAdapter<HttpService>

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private fun startApp(selectedHttpService: HttpService) {
        nsdHelper.stopDiscovery()

        saveBaseUrl(this, appWidgetId,
                "http://${selectedHttpService.hostIp}:${selectedHttpService.port}")

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        MusicBoxControlWidget.updateAppWidget(this, appWidgetManager, appWidgetId)

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(Activity.RESULT_CANCELED)

        setContentView(R.layout.music_box_control_widget_configure)

        // Find the widget id from the intent.
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Connect to background Service
        // bindService(
        //         Intent(this, MopidyControlService::class.java), this, Context.BIND_AUTO_CREATE)

        foundHttpServicesList = ArrayAdapter(this, android.R.layout.simple_list_item_1)

        val listView = findViewById(R.id.listView) as ListView
        listView.adapter = foundHttpServicesList
        listView.setOnItemClickListener { parent, view, position, id ->
            startApp(parent.adapter.getItem(position) as HttpService) }

        nsdHelper = NsdHelper(this, this)
        nsdHelper.isLogEnabled = true
        nsdHelper.setDiscoveryTimeout(60)

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout) as SwipeRefreshLayout

        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent)
        swipeRefreshLayout.setOnRefreshListener {
            if (nsdHelper.isDiscoveryRunning) {
                nsdHelper.stopDiscovery()
            }
            foundHttpServicesList.clear()
            nsdHelper.startDiscovery(NsdType.HTTP)
        }

        if (MopidyControlService.isNetworkConnected(this)) {
            nsdHelper.startDiscovery(NsdType.HTTP)
            swipeRefreshLayout.isRefreshing = true
        }
        else {
            Toast.makeText(this,
                    getString(R.string.network_unavailable),
                    Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.widget_configure_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.stopMenuItem) {
            if (nsdHelper.isDiscoveryRunning) {
                nsdHelper.stopDiscovery()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNsdServiceResolved(resolvedService: NsdService) {
        runOnUiThread {
            foundHttpServicesList.add(HttpService(
                    resolvedService.name,
                    resolvedService.hostIp,
                    resolvedService.port
            ))
        }
    }

    override fun onNsdServiceLost(lostService: NsdService) {
    }

    override fun onNsdError(errorMessage: String, errorCode: Int, errorSource: String) {
    }

    override fun onNsdDiscoveryFinished() {
        swipeRefreshLayout.isRefreshing = false
    }

    override fun onNsdServiceFound(foundService: NsdService) {
    }

    override fun onNsdRegistered(registeredService: NsdService) {
    }

    companion object {
        private val PREFS_NAME = "space.leje.musicboxcontrol"
        private val PREF_BASE_URL_PREFIX = "baseUrl_"

        internal fun loadBaseUrl(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getString(PREF_BASE_URL_PREFIX + appWidgetId, null)
        }

        internal fun saveBaseUrl(context: Context, appWidgetId: Int, url: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
                    .edit()
            prefs.putString(PREF_BASE_URL_PREFIX + appWidgetId, url)
            prefs.apply()
        }

        internal fun removeBaseUrl(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
                    .edit()
            prefs.remove(PREF_BASE_URL_PREFIX + appWidgetId)
            prefs.apply()
        }
    }
}

