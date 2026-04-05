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
            // 检测当前输入法是否为CJK
            val isCJK = isCurrentInputMethodCJK()
            if (isCJK) {
                // 临时切换到ASCII键盘
                switchToAsciiKeyboard()
            }

            // 将文本放入剪贴板
            val clip = ClipData.newPlainText("voice_input", text)
            clipboard.setPrimaryClip(clip)

            // 执行粘贴操作
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_PASTE)
        } finally {
            // 恢复原始剪贴板内容
            if (originalClipboard != null) {
                clipboard.setPrimaryClip(originalClipboard!!)
            }
        }
    }

    private fun isCurrentInputMethodCJK(): Boolean {
        // 这里可以实现检测当前输入法是否为CJK的逻辑
        // 简化实现，实际项目中可能需要更复杂的检测
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val currentInputMethod = inputMethodManager.currentInputMethodSubtype
        return currentInputMethod?.language?.startsWith("zh") == true ||
               currentInputMethod?.language?.startsWith("ja") == true ||
               currentInputMethod?.language?.startsWith("ko") == true
    }

    private fun switchToAsciiKeyboard() {
        // 这里可以实现切换到ASCII键盘的逻辑
        // 简化实现，实际项目中可能需要更复杂的处理
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        // 尝试切换到美式英语键盘
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