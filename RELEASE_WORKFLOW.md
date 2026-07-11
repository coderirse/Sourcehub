# SourceHub 发布工作流

> 喂 AI 用：本文档记录了此项目的完整发布流程，包括构建命令、git 操作、GitHub Release 发布等。

---

## 1. 项目概况

- **项目名称**：SourceHub
- **用途**：数字资料售卖分享 Android App — PDF/Word 文件售卖、自动化收款、用户认证、反爬安全
- **语言/框架**：Kotlin + Jetpack Compose + Material 3
- **最低 SDK**：24 (Android 7.0)
- **GitHub**：`coderirse/Sourcehub`

---

## 2. 环境配置

### 构建环境

- **JDK**：Gradle 自动管理的 JDK，路径 `C:/Users/shang/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2`
- **Gradle**：项目自带的 `gradlew.bat`（Windows）
- **Android SDK**：`C:/Users/shang/AppData/Local/Android/Sdk`

### JAVA_HOME

```bash
export JAVA_HOME="C:/Users/shang/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2"
export PATH="$JAVA_HOME/bin:$PATH"
```

### 代理配置（如需要）

如果 GitHub 推送需要走代理：

```bash
# git
git -c http.proxy=http://127.0.0.1:7897 -c https.proxy=http://127.0.0.1:7897 push

# gh CLI
HTTPS_PROXY=http://127.0.0.1:7897 HTTP_PROXY=http://127.0.0.1:7897 gh release create ...
```

---

## 3. 版本号规则

| 文件 | 字段 | 说明 |
|------|------|------|
| `app/build.gradle.kts` | `versionCode` | 整数，每次 +1 |
| `app/build.gradle.kts` | `versionName` | 字符串，如 `"1.0.1"` |

| 版本 | versionCode | 说明 |
|------|------------|------|
| v1.0.0 | 1 | 初始版本，完整 MVP |

---

## 4. 完整发布流程

### 4.1 改版本号

编辑 `app/build.gradle.kts`：

```kotlin
versionCode = 2       // +1
versionName = "1.0.1" // 对应版本名
```

### 4.2 构建 APK

```bash
cd "d:/Android-project/Sourcehub"
JAVA_HOME="C:/Users/shang/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2" \
  ./gradlew assembleDebug --no-daemon
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`

### 4.3 验证构建

```bash
# 确认 BUILD SUCCESSFUL
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

### 4.4 Git 提交

```bash
git add <改动的文件...>
# 例如：
# git add app/build.gradle.kts
# git add app/src/main/java/com/example/sourcehub/...

git commit -m "修复描述 (v1.0.x)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

提交信息格式：简短描述 + 版本号标签，以 `Co-Authored-By:` 结尾。

### 4.5 推送

```bash
git push origin master

# 如果需要代理：
# git -c http.proxy=http://127.0.0.1:7897 \
#     -c https.proxy=http://127.0.0.1:7897 \
#     -c http.postBuffer=524288000 \
#     push origin master
```

### 4.6 创建 GitHub Release

```bash
# 复制 APK 为带版本号的文件名
cp app/build/outputs/apk/debug/app-debug.apk SourceHub-v1.0.x-debug.apk

# 创建 release
gh release create v1.0.x \
  SourceHub-v1.0.x-debug.apk \
  --title "v1.0.x — 简短标题" \
  --notes "详细 release notes..."

# 清理
rm SourceHub-v1.0.x-debug.apk
```

---

## 5. 代码架构速查

```
app/src/main/java/com/example/sourcehub/
├── SourcehubApplication.kt       # Application，DI 容器初始化，安全检测
├── MainActivity.kt               # 单 Activity，Compose 入口
├── di/
│   └── AppContainer.kt           # 手动 DI，所有依赖在此注册
├── navigation/
│   ├── Screen.kt                 # 类型安全路由 sealed class
│   └── NavGraph.kt               # NavHost 定义，全部页面路由
├── domain/
│   ├── model/                    # 11 个领域模型
│   └── repository/               # 6 个仓库接口
├── data/
│   ├── remote/api/               # API 接口定义（预留 Retrofit）
│   ├── remote/mock/              # Mock 实现（模拟 200-800ms 延迟）
│   ├── remote/dto/               # 数据传输对象
│   ├── repository/               # 仓库实现（桥接 Mock API + 本地存储）
│   ├── local/mock/               # MockDataProvider（20 商品/6 分类/3 Banner）
│   ├── local/prefs/              # DataStore 偏好管理
│   └── filestorage/              # 文件存储 + 下载状态管理
├── presentation/
│   ├── auth/                     # 登录/注册/忘记密码
│   ├── home/                     # 首页
│   ├── product/list/             # 商品列表
│   ├── product/detail/           # 商品详情
│   ├── product/preview/          # PDF 预览（FLAG_SECURE）
│   ├── search/                   # 搜索
│   ├── cart/                     # 购物车
│   ├── checkout/                 # 结算
│   ├── payment/                  # 支付 + 结果页
│   ├── orders/list/              # 订单列表
│   ├── orders/detail/            # 订单详情
│   ├── download/                 # 下载管理 + 离线文件
│   ├── profile/                  # 个人中心 + 编辑资料
│   ├── settings/                 # 设置 + 安全 + 关于
│   └── common/                   # 通用组件 + 状态类
├── security/                     # 7 个安全模块
└── worker/                       # WorkManager 后台下载
```

### 关键设计点

- **手动 DI**：`AppContainer` 在 `SourcehubApplication.onCreate()` 中初始化，所有 ViewModel 通过 `SourcehubApplication.instance.appContainer` 获取依赖。不使用 Hilt。
- **Mock API 架构**：所有 API 接口定义在 `data/remote/api/`，Mock 实现在 `data/remote/mock/`。切换真实后端时，用 Retrofit 实现相同接口并替换即可。
- **无 Room 数据库**：MVP 使用内存存储（`MutableStateFlow`）。后续可引入 Room，实体结构已在 `data/local/db/entity/` 预定义（此目录在切换到无 Room 方案时被删除）。
- **支付接口预留**：`PaymentApi` 定义 `createPayment` / `verifyPayment` / `refundPayment` 三个端点，与 Retrofit 兼容。
- **文件安全**：下载文件经 AES-256-GCM 加密存储，密钥由 Android Keystore 管理。预览和支付页面启用 `FLAG_SECURE` 防截屏。

---

## 6. 关键文件清单

| 文件 | 作用 | 修改频率 |
|------|------|---------|
| `app/build.gradle.kts` | 版本号、依赖配置 | 每次发版 |
| `gradle/libs.versions.toml` | 依赖版本目录 | 加新库时 |
| `di/AppContainer.kt` | DI 容器 | 加新仓库/服务时 |
| `navigation/NavGraph.kt` | 全部页面路由 | 加新页面时 |
| `security/TokenManager.kt` | Token 存储与管理 | 少 |
| `security/CryptoManager.kt` | 文件加解密 | 少 |
| `data/local/mock/MockDataProvider.kt` | Mock 数据 | 加测试数据时 |
| `presentation/*/` | 各页面 UI + ViewModel | 功能迭代 |

---

## 7. 调试方法

### 7.1 Logcat 日志

```bash
# 确认设备连接
~/AppData/Local/Android/Sdk/platform-tools/adb devices

# 清旧日志 + 抓 SourcehubApp 标签
~/AppData/Local/Android/Sdk/platform-tools/adb logcat -c
~/AppData/Local/Android/Sdk/platform-tools/adb logcat -s SourcehubApp
```

### 7.2 日志标签

| TAG | 用途 |
|-----|------|
| `SourcehubApp` | 应用启动、安全检测结果 |
| `AntiDebugging` | 调试器检测 |

---

## 8. 测试账号

| 邮箱 | 密码 | 说明 |
|------|------|------|
| `test@sourcehub.com` | `password123` | 默认测试账号 |
| 任意邮箱 | 任意 ≥6 位密码 | 注册新账号（模拟） |

优惠码：`SAVE10`（9 折）

---

## 9. 提交信息参考

```
Initial commit: SourceHub v1.0.0 — 数字资料售卖App
```

格式：`简短描述 (v1.0.x)` + 必要时正文 + `Co-Authored-By:` 结尾。

---

## 10. Release Notes 模板

```markdown
## SourceHub v1.0.x

### 新增
- xxx

### 修复
- xxx

### 变更
- xxx

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```
