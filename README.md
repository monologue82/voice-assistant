# Voice Assistant - 语音助手应用

一个带悬浮窗的Android语音助手应用，支持语音识别和LLM文本精炼功能。

## 功能特性

- 🎤 悬浮窗语音按钮
- 🌍 支持多种语言（中文、英文、日文、韩文等）
- 🤖 可选LLM文本精炼功能
- ✂️ 自动剪贴板粘贴
- 🎨 可拖动的悬浮窗位置

## 下载APK

您可以从GitHub Actions下载最新的构建版本：

- [Debug版本（推荐）](https://github.com/monologue82/voice-assistant/actions)
- Release版本

## 使用说明

### 1. 授予权限

首次使用需要授予以下权限：

- **悬浮窗权限**：用于显示悬浮录音按钮
- **录音权限**：用于语音识别
- **无障碍服务**：用于自动粘贴文本

### 2. 配置设置

打开应用后，可以配置以下选项：

- **语言**：选择语音识别语言
- **LLM精炼**：是否启用AI文本精炼
- **API配置**：配置LLM API（如果启用精炼功能）

### 3. 使用语音助手

1. 点击悬浮窗的麦克风按钮开始录音
2. 说话后松开或等待自动停止
3. 识别的文本会自动复制到剪贴板
4. 粘贴到需要的位置即可

## 技术栈

- **语言**：Kotlin
- **最低SDK**：API 33 (Android 13)
- **目标SDK**：API 34 (Android 14)
- **Gradle**：8.9
- **Android Gradle Plugin**：8.7.0

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/voiceassistant/
│   │   ├── MainActivity.kt              # 主界面
│   │   ├── VoiceAssistantService.kt    # 语音助手服务
│   │   └── VoiceAccessibilityService.kt # 无障碍服务
│   ├── res/
│   │   ├── layout/                      # 布局文件
│   │   ├── values/                      # 资源值
│   │   └── drawable/                    # 图片资源
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## 本地构建

```bash
# 克隆仓库
git clone https://github.com/monologue82/voice-assistant.git

# 构建Debug APK
./gradlew assembleDebug

# 构建Release APK
./gradlew assembleRelease
```

## 许可证

MIT License
