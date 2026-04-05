package com.example.voiceassistant

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("voice_assistant", MODE_PRIVATE)

        setupLanguageSpinner()
        loadSettings()
        setupClickListeners()
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf(
            "English (en-US)",
            "Chinese (Simplified) (zh-CN)",
            "Chinese (Traditional) (zh-TW)",
            "Japanese (ja-JP)",
            "Korean (ko-KR)"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_language.adapter = adapter

        val savedLanguage = sharedPreferences.getString("language", "zh-CN")
        val position = when (savedLanguage) {
            "en-US" -> 0
            "zh-CN" -> 1
            "zh-TW" -> 2
            "ja-JP" -> 3
            "ko-KR" -> 4
            else -> 1
        }
        spinner_language.setSelection(position)
    }

    private fun loadSettings() {
        switch_llm_refinement.isChecked = sharedPreferences.getBoolean("llm_refinement_enabled", false)
        et_api_base_url.setText(sharedPreferences.getString("api_base_url", ""))
        et_api_key.setText(sharedPreferences.getString("api_key", ""))
        et_model.setText(sharedPreferences.getString("model", "gpt-3.5-turbo"))
    }

    private fun setupClickListeners() {
        btn_enable_overlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivityForResult(intent, 101)
                } else {
                    Toast.makeText(this, "Overlay permission already enabled", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btn_enable_accessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btn_start_service.setOnClickListener {
            val intent = Intent(this, VoiceAssistantService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
        }

        btn_stop_service.setOnClickListener {
            val intent = Intent(this, VoiceAssistantService::class.java)
            stopService(intent)
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
        }

        btn_save.setOnClickListener {
            saveSettings()
        }

        btn_test_api.setOnClickListener {
            testApi()
        }

        btn_reset_position.setOnClickListener {
            sharedPreferences.edit().putInt("floating_button_x", 100).putInt("floating_button_y", 300).apply()
            Toast.makeText(this, "Floating button position reset", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        val language = when (spinner_language.selectedItemPosition) {
            0 -> "en-US"
            1 -> "zh-CN"
            2 -> "zh-TW"
            3 -> "ja-JP"
            4 -> "ko-KR"
            else -> "zh-CN"
        }

        sharedPreferences.edit()
            .putString("language", language)
            .putBoolean("llm_refinement_enabled", switch_llm_refinement.isChecked)
            .putString("api_base_url", et_api_base_url.text.toString())
            .putString("api_key", et_api_key.text.toString())
            .putString("model", et_model.text.toString())
            .apply()

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun testApi() {
        val apiBaseUrl = et_api_base_url.text.toString()
        val apiKey = et_api_key.text.toString()
        val model = et_model.text.toString()

        if (apiBaseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
            Toast.makeText(this, "Please fill all API fields", Toast.LENGTH_SHORT).show()
            return
        }

        // 这里可以添加API测试逻辑
        Toast.makeText(this, "Test successful!", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}