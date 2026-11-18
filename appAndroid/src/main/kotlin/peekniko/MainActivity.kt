package peekniko

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import peekniko.storage.sqldelight.sqlDelightStorage

class MainActivity : AppCompatActivity() {
    private val holder: SampleViewModelHolder by viewModels()
    private val viewModel: SampleViewModel get() = holder.sampleViewModel

    private lateinit var status: TextView
    private lateinit var localServer: SwitchCompat
    private lateinit var runOkHttp: Button
    private lateinit var runKtor: Button

    private val onLocalServerToggled = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        viewModel.onLocalServerToggled(isChecked)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Must run before the view model is created: it builds HTTP clients that capture into Peekniko.
        Peekniko.configure {
            sqlDelightStorage()
        }
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        status = findViewById(R.id.status)
        localServer = findViewById(R.id.local_server)
        runOkHttp = findViewById(R.id.run_okhttp)
        runKtor = findViewById(R.id.run_ktor)
        findViewById<Button>(R.id.open_peekniko).setOnClickListener { viewModel.onOpenPeeknikoClick() }
        runOkHttp.setOnClickListener { viewModel.onRunOkHttp() }
        runKtor.setOnClickListener { viewModel.onRunKtor() }
        localServer.setOnCheckedChangeListener(onLocalServerToggled)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            SampleEvent.OpenPeekniko -> Peekniko.open(this@MainActivity)
                        }
                    }
                }
            }
        }
    }

    private fun render(state: SampleUiState) {
        status.text = state.status

        localServer.visibility = if (state.showLocalServerToggle) View.VISIBLE else View.GONE
        localServer.isEnabled = !state.isLoading
        // Update the switch without reporting it back as a user toggle.
        localServer.setOnCheckedChangeListener(null)
        localServer.isChecked = state.isLocalServerEnabled
        localServer.setOnCheckedChangeListener(onLocalServerToggled)

        runOkHttp.visibility = if (state.showOkHttpButton) View.VISIBLE else View.GONE
        runOkHttp.isEnabled = !state.isLoading
        runKtor.isEnabled = !state.isLoading
    }
}
