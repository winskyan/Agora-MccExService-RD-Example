# MccExService RD Example - Android

## 🚀 运行 Demo

### 前置条件

- Android Studio Arctic Fox 或更高版本
- Android SDK 21+
- NDK 23.1.7779620 或更高版本

### 步骤

1. **下载代码**

   ```bash
   git clone <repository_url>
   cd Agora-MccExService-RD-Example
   ```

2. **打开 Android Studio**
    - 启动 Android Studio
    - 选择 "Open an existing project"
    - 选择项目根目录

3. **配置项目**

   在 `local.properties` 中添加以下配置：

   ```properties
   APP_CERTIFICATE=your_app_certificate_here
   APP_ID=your_agora_app_id_here
   YSD_APP_ID=your_ysd_app_id_here
   YSD_APP_KEY=your_ysd_app_key_here
   ```

   **注意**：运行时请检查并修改 `app/src/main/java/io/agora/mccex_demo/utils/MccExKeys.kt` 中的默认值，填入有效的 `ysdToken` 和 `ysdUserId`：

   ```kotlin
   object MccExKeys {
       var ysdAppId: String = BuildConfig.YSD_APP_ID
       var ysdAppKey: String = BuildConfig.YSD_APP_KEY
       var ysdToken:String="your_token_here"
       var ysdUserId:String="your_user_id_here"
   }
   ```

4. **运行项目**
    - 连接 Android 设备或启动模拟器
    - 点击 Run 按钮或按 `Shift + F10`
