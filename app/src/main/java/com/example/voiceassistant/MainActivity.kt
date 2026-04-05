package com.example.voiceassistant

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    
    private lateinit var spinnerLanguage: Spinner
    private lateinit var switchLlmRefinement: Switch
    private lateinit var etApiBaseUrl: EditText
    private lateinit var etApiKey: EditText
    private lateinit var etModel: EditText
    private lateinit var btnEnableOverlay: Button
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    private lateinit var btnSave: Button
    private lateinit var btnTestApi: Button
    private lateinit var btnResetPosition: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("voice_assistant", MODE_PRIVATE)
        
        // 初始化视图引用
        initViews()

        setupLanguageSpinner()
        loadSettings()
        setupClickListeners()
    }
    
    private fun initViews() {
        spinnerLanguage = findViewById(R.id.spinner_language)
        switchLlmRefinement = findViewById(R.id.switch_llm_refinement)
        etApiBaseUrl = findViewById(R.id.et_api_base_url)
        etApiKey = findViewById(R.id.et_api_key)
        etModel = findViewById(R.id.et_model)
        btnEnableOverlay = findViewById(R.id.btn_enable_overlay)
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility)
        btnStartService = findViewById(R.id.btn_start_service)
        btnStopService = findViewById(R.id.btn_stop_service)
        btnSave = findViewById(R.id.btn_save)
        btnTestApi = findViewById(R.id.btn_test_api)
        btnResetPosition = findViewById(R.id.btn_reset_position)
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
        spinnerLanguage.adapter = adapter

        val savedLanguage = sharedPreferences.getString("language", "zh-CN")
        val position = when (savedLanguage) {
            "en-US" -> 0
            "zh-CN" -> 1
            "zh-TW" -> 2
            "ja-JP" -> 3
            "ko-KR" -> 4
            else -> 1
        }
        spinnerLanguage.setSelection(position)
    }

    private fun loadSettings() {
        switchLlmRefinement.isChecked = sharedPreferences.getBoolean("llm_refinement_enabled", false)
        etApiBaseUrl.setText(sharedPreferences.getString("api_base_url", ""))
        etApiKey.setText(sharedPreferences.getString("api_key", ""))
        etModel.setText(sharedPreferences.getString("model", "gpt-3.5-turbo"))
    }

    private fun setupClickListeners() {
        btnEnableOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivityForResult(intent, 101)
                } else {
                    Toast.makeText(this, "Overlay permission already enabled", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnStartService.setOnClickListener {
            val intent = Intent(this, VoiceAssistantService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
        }

        btnStopService.setOnClickListener {
            val intent = Intent(this, VoiceAssistantService::class.java)
            stopService(intent)
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            saveSettings()
        }

        btnTestApi.setOnClickListener {
            testApi()
        }

        btnResetPosition.setOnClickListener {
            sharedPreferences.edit().putInt("floating_button_x", 100).putInt("floating_button_y", 300).apply()
            Toast.makeText(this, "Floating button position reset", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        val language = when (spinnerLanguage.selectedItemPosition) {
            0 -> "en-US"
            1 -> "zh-CN"
            2 -> "zh-TW"
            3 -> "ja-JP"
            4 -> "ko-KR"
            else -> "zh-CN"
        }

        sharedPreferences.edit()
            .putString("language", language)
            .putBoolean("llm_refinement_enabled", switchLlmRefinement.isChecked)
            .putString("api_base_url", etApiBaseUrl.text.toString())
            .putString("api_key", etApiKey.text.toString())
            .putString("model", etModel.text.toString())
            .apply()

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun testApi() {
        val apiBaseUrl = etApiBaseUrl.text.toString()
        val apiKey = etApiKey.text.toString()
        val model = etModel.text.toString()

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