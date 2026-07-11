# SourceHub — 数字资料售卖分享平台

一个完整的 Android 数字资料售卖 App，支持 PDF/Word 等文件售卖、自动化收款、用户认证和反爬安全功能。

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-purple?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose-Material%203-blue?logo=jetpackcompose" alt="Compose">
  <img src="https://img.shields.io/badge/minSdk-24-green?logo=android" alt="minSdk">
  <img src="https://img.shields.io/badge/targetSdk-37-green?logo=android" alt="targetSdk">
</p>

---

## 功能概览

| 模块 | 功能 |
|------|------|
| 🔐 用户认证 | 登录/注册/忘记密码、JWT Token 管理、自动刷新 |
| 🏠 首页 | Banner 轮播、6 大分类、热门推荐、新品上架 |
| 🔍 搜索 | 300ms 防抖搜索、搜索历史 |
| 🛒 购物车 | CRUD、数量调整、全选、实时计价 |
| 💰 结算支付 | 订单确认、优惠码、微信/支付宝/信用卡模拟支付 |
| 📦 订单管理 | 状态筛选、订单详情、已支付直接下载 |
| 📥 文件下载 | WorkManager 后台下载、AES-256-GCM 加密存储 |
| 👤 个人中心 | 资料编辑、安全状态检测、离线文件管理 |

## 安全特性

- **Root 检测** — su 二进制 / Magisk / SuperSU / 系统属性检查
- **模拟器检测** — Fingerprint / Model / Hardware / 特征文件
- **反调试** — Debug.isDebuggerConnected() 周期监测
- **SSL Pinning** — OkHttp CertificatePinner
- **请求签名** — HMAC-SHA256(method + path + timestamp + body)
- **防截屏** — 支付页和预览页 FLAG_SECURE
- **文件加密** — AES/GCM/NoPadding，Android Keystore 密钥管理
- **R8 混淆** — Release 构建代码混淆 + 日志移除

## 技术架构

```
Presentation (Compose UI + ViewModels + StateFlow)
    ↕
Domain (Models + Repository Interfaces)
    ↕
Data (Repository Impls + Mock API + DataStore + FileStorage)
```

- **架构模式**: MVVM + Clean Architecture（单模块，包隔离）
- **依赖注入**: 手动 DI（AppContainer），无框架开销
- **状态管理**: StateFlow + collectAsStateWithLifecycle()
- **导航**: Navigation Compose（类型安全路由，5 个底部 Tab）
- **Mock 架构**: API 接口与 Retrofit 签名一致，切换真实后端只需替换实现

## 快速开始

### 环境要求

- Android Studio (latest)
- JDK 21
- Android SDK 37

### 构建

```bash
./gradlew assembleDebug
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`

### 测试账号

| 邮箱 | 密码 |
|------|------|
| `test@sourcehub.com` | `password123` |

优惠码：`SAVE10`（9 折）

## 项目结构

```
app/src/main/java/com/example/sourcehub/
├── SourcehubApplication.kt      # Application 入口
├── MainActivity.kt              # 单 Activity
├── di/AppContainer.kt           # 手动 DI 容器
├── navigation/                  # 路由定义 + NavHost
├── domain/model/                # 11 个领域模型
├── domain/repository/           # 6 个仓库接口
├── data/remote/api/             # API 接口（预留 Retrofit）
├── data/remote/mock/            # Mock 实现
├── data/repository/             # 仓库实现
├── data/local/                  # Mock 数据 + DataStore
├── data/filestorage/            # 文件加密存储
├── presentation/                # 约 20 个页面
│   ├── auth/                    # 登录/注册
│   ├── home/                    # 首页
│   ├── product/                 # 商品列表/详情/预览
│   ├── search/                  # 搜索
│   ├── cart/                    # 购物车
│   ├── checkout/                # 结算
│   ├── payment/                 # 支付
│   ├── orders/                  # 订单
│   ├── download/                # 下载管理
│   ├── profile/                 # 个人中心
│   └── settings/                # 设置
├── security/                    # 7 个安全模块
└── worker/                      # 后台下载 Worker
```

## 从 Mock 迁移到生产

1. 用 Retrofit 实现 `data/remote/api/` 下的接口
2. 替换 `data/remote/mock/` 为 Retrofit 代理
3. 对接微信/支付宝支付 SDK（`PaymentApi` 接口已预定义）
4. 可引入 Room 做本地缓存（实体结构已预定义）

## License

MIT
