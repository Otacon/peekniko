package peekniko.ui.networkcalls

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import peekniko.Peekniko
import peekniko.internal.notifications.AndroidNotifications
import peekniko.internal.support.viewById
import peekniko.library.R
import peekniko.ui.details.NetworkCallDetailFragment

public class PeeknikoActivity : AppCompatActivity() {
    private val root: ViewGroup by viewById(R.id.main_layout)
    private val twoPanes: Boolean get() = resources.getBoolean(R.bool.peekniko_two_panes)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peekniko)
        supportFragmentManager.restoreLayout(savedInstanceState)
        applySystemBarInsets()
        onBackPressedDispatcher.addCallback(this) {
            when {
                twoPanes -> finish()
                supportFragmentManager.backStackEntryCount <= 0 -> finish()
                else -> supportFragmentManager.popBackStack()
            }
        }
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= 33) {
            requestNotificationPermissionApi33AndAbove()
        }
    }

    internal fun onCallSelected(callId: String) {
        supportFragmentManager.showDestination(callId)
    }

    private fun FragmentManager.showDestination(callId: String?) {
        if (twoPanes) {
            showTabletDetails(callId)
        } else {
            showPhoneDetails(callId)
        }
    }

    private fun FragmentManager.restoreLayout(savedInstanceState: Bundle?) {
        val hasLayoutChanged = savedInstanceState != null && savedInstanceState.getBoolean(STATE_TWO_PANES) != twoPanes
        if (hasLayoutChanged) {
            removeAllDetailFragments()
        }
        if (savedInstanceState == null) {
            beginTransaction()
                .add(R.id.network_calls_host, NetworkCallListFragment())
                .commitNow()
        }
        if (savedInstanceState == null || hasLayoutChanged) {
            showDestination(savedInstanceState?.getString("selected_call_id"))
        }
    }

    private fun FragmentManager.removeAllDetailFragments() {
        popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val detailFragments = fragments.filterIsInstance<NetworkCallDetailFragment>()
        if (detailFragments.isNotEmpty()) {
            beginTransaction()
                .apply { detailFragments.forEach { remove(it) } }
                .commitNow()
        }
    }

    private fun FragmentManager.showTabletDetails(callId: String?) {
        val current = findFragmentById(R.id.network_detail_host) as? NetworkCallDetailFragment
        if (current != null && current.transactionId == callId) return
        beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.network_detail_host, NetworkCallDetailFragment.newInstance(callId))
            .commit()
    }

    private fun FragmentManager.showPhoneDetails(callId: String?) {
        if (callId == null) return
        beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.anim.slide_in_from_end, R.anim.slide_out_to_start,
                R.anim.slide_in_from_start, R.anim.slide_out_to_end,
            )
            .replace(R.id.network_calls_host, NetworkCallDetailFragment.newInstance(callId))
            .addToBackStack("details")
            .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val detailHost = if (twoPanes) R.id.network_detail_host else R.id.network_calls_host
        val detail = supportFragmentManager.findFragmentById(detailHost) as? NetworkCallDetailFragment
        outState.putString("selected_call_id", detail?.transactionId)
        outState.putBoolean(STATE_TWO_PANES, twoPanes)
    }

    private fun applySystemBarInsets() {
        val initialPaddingLeft = root.paddingLeft
        val initialPaddingTop = root.paddingTop
        val initialPaddingRight = root.paddingRight
        val initialPaddingBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(
                initialPaddingLeft + insets.left,
                initialPaddingTop + insets.top,
                initialPaddingRight + insets.right,
                initialPaddingBottom + insets.bottom,
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }

    @RequiresApi(33)
    private fun requestNotificationPermissionApi33AndAbove() {
        try {
            if (applicationInfo.targetSdkVersion <= 32) {
                // Android controls the permission dialog for legacy-target apps.
                // Creating the first channel here keeps that prompt in this activity.
                (Peekniko.notifications as? AndroidNotifications)?.registerChannel()
            } else if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST,
                )
            }
        } catch (exception: RuntimeException) {
            Log.e("PeeknikoActivity", "Unable to request notification permission", exception)
        }
    }

    private companion object {
        const val STATE_TWO_PANES = "two_panes"
        const val NOTIFICATION_PERMISSION_REQUEST = 7101
    }
}
