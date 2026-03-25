package com.streann.insidead.demo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.prerollButton).setOnClickListener {
            startActivity(Intent(this, PrerollActivity::class.java))
        }

        findViewById<Button>(R.id.combinedButton).setOnClickListener {
            startActivity(Intent(this, CombinedActivity::class.java))
        }

        findViewById<Button>(R.id.playerButton).setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        findViewById<Button>(R.id.splitActivityButton).setOnClickListener {
            startActivity(Intent(this, SplitActivity::class.java))
        }
    }
}
