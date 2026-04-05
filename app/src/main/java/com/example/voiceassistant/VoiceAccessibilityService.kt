package com.example.voiceassistant

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class VoiceAccessibilityService : AccessibilityService() {
    private var originalClipboard: ClipData? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 可以在这里处理焦点变化事件
    }

    override fun onInterrupt() {
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "inject_text") {
            val text = intent.getStringExtra("text")
            text?.let { injectText(it) }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun injectText(text: String) {
        // 保存原始剪贴板内容
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        originalClipboard = clipboard.primaryClip

        try {
            // 将文本放入剪贴板
            val clip = ClipData.newPlainText("voice_input", text)
            clipboard.setPrimaryClip(clip)
        } finally {
            // 恢复原始剪贴板内容
            if (originalClipboard != null) {
                clipboard.setPrimaryClip(originalClipboard!!)
            }
        }
    }

    private fun findFocusedEditText(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findFocusedEditText(rootNode)
    }

    private fun findFocusedEditText(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && node.className == "android.widget.EditText") {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findFocusedEditText(child)
            if (result != null) {
                return result
            }
        }
        return null
    }
}