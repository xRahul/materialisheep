package io.github.sheepdestroyer.materialisheep

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        val log = intent.getStringExtra(EXTRA_LOG)
        findViewById<TextView>(R.id.text_log).text = log
    }

    companion object {
        const val EXTRA_LOG = "EXTRA_LOG"
    }
}
