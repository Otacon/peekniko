package peekniko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import peekniko.storage.sqldelight.sqlDelightStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Peekniko.configure {
            sqlDelightStorage()
        }
        AppModule.initialize()
        enableEdgeToEdge()
        setContent {
            SampleApp(onOpenPeekniko = {
                Peekniko.open(this@MainActivity)
            })
        }
    }
}
