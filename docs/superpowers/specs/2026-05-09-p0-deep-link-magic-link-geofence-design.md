# P0 功能补齐设计：Deep Linking + Magic Link/SSO + 地理围栏

> 日期：2026-05-09
> 范围：Android 端 4 个 P0 缺口（对标 iOS）
> 实施顺序：Deep Linking → Magic Link + SSO → 地理围栏

---

## 1. Deep Linking

### 1.1 路由表

| Scheme | 路径 | 目标 Route | 参数 |
|--------|------|-----------|------|
| `mistyislet://` | `unlock/{doorId}` | `Routes.DOORS` | doorId: String |
| `mistyislet://` | `pass` | `Routes.PASS` | — |
| `mistyislet://` | `dashboard` | `Routes.DASHBOARD` | — |
| `mistyislet://` | `profile` | `Routes.PROFILE` | — |
| `mistyislet://` | `magic-link?token={token}` | `Routes.LOGIN` | token: String |
| `https://app.mistyislet.com` | `/visitor/{token}` | `Routes.VISITORS` | visitorToken: String |

### 1.2 AndroidManifest.xml 变更

在 MainActivity 的 `<activity>` 内新增两组 intent-filter：

```xml
<!-- Custom scheme -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="mistyislet" />
</intent-filter>

<!-- App Links (HTTPS) -->
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="app.mistyislet.com" />
</intent-filter>
```

### 1.3 Navigation deep link 注册

在 AppNavigation.kt 中为每个路由添加 `deepLinks` 参数：

```kotlin
composable(
    Routes.DOORS,
    deepLinks = listOf(navDeepLink { uriPattern = "mistyislet://unlock/{doorId}" })
) { ... }
```

Navigation Compose 会自动匹配 URI 并传入 `NavBackStackEntry.arguments`。

### 1.4 Magic Link token 特殊处理

Magic link 的 deep link（`mistyislet://magic-link?token=xxx`）比较特殊：用户可能未登录。

处理策略：
- MainActivity.onCreate / onNewIntent 中检查 intent
- 如果是 magic-link intent：提取 token → 存入 `SavedStateHandle` 或临时变量
- 如果当前在 LoginScreen → LoginViewModel 自动调用 `verifyMagicLink(token)`
- 如果已登录 → 忽略（token 已过期场景无害）

新增文件：`core/deeplink/DeepLinkHandler.kt`
- `fun extractMagicLinkToken(intent: Intent): String?`
- `fun extractDeepLinkRoute(intent: Intent): String?`

### 1.5 不新增依赖

Navigation Compose 原生支持 `navDeepLink`，无需额外库。

---

## 2. Magic Link + SSO 登录

### 2.1 登录状态机

```
AuthStep.EmailInput
    ↓ submitEmail() → 提取域名 → 调用 org-lookup
AuthStep.OrgLookupLoading
    ↓
AuthStep.PasswordInput   (authType == "password" 或无组织)
    → login()            → 密码登录
    → requestMagicLink() → AuthStep.MagicLinkSent
AuthStep.SSORedirect     (authType == "sso")
    → 打开 CustomTabs(ssoUrl)
    → SSO 回调处理
AuthStep.MagicLinkSent
    → deep link 回调 → verifyMagicLink(token) → 登录成功
```

### 2.2 新增 API 方法

AuthApi.kt 新增：

```kotlin
@GET("app/auth/org-lookup")
suspend fun orgLookup(@Query("domain") domain: String): OrgAuthConfig

@POST("app/auth/magic-link")
suspend fun requestMagicLink(@Body request: MagicLinkRequest): MagicLinkResponse

@POST("app/auth/magic-link/verify")
suspend fun verifyMagicLink(@Body request: VerifyMagicLinkRequest): LoginResponse
```

### 2.3 新增数据模型

ApiModels.kt 新增：

```kotlin
@Serializable
data class OrgAuthConfig(
    @SerialName("auth_type") val authType: String,      // "password" | "sso" | "saml"
    @SerialName("sso_url") val ssoUrl: String? = null,
    @SerialName("org_name") val orgName: String? = null,
)

@Serializable
data class MagicLinkRequest(val email: String)

@Serializable
data class MagicLinkResponse(val status: String)  // "sent"

@Serializable
data class VerifyMagicLinkRequest(val token: String)
```

### 2.4 AuthRepository 扩展

新增 3 个方法：

```kotlin
suspend fun lookupOrg(domain: String): ApiResult<OrgAuthConfig>
suspend fun requestMagicLink(email: String): ApiResult<MagicLinkResponse>
suspend fun verifyMagicLink(token: String): ApiResult<LoginResponse>
```

`verifyMagicLink` 成功后与 `login()` 一样存储 token。

### 2.5 LoginViewModel 重构

LoginUiState 改为：

```kotlin
enum class AuthStep {
    EmailInput,
    OrgLookupLoading,
    PasswordInput,
    MagicLinkSent,
    SSORedirect,
}

data class LoginUiState(
    val authStep: AuthStep = AuthStep.EmailInput,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val orgAuthConfig: OrgAuthConfig? = null,
    val magicLinkSent: Boolean = false,
    val forgotPasswordSent: Boolean = false,
    val forgotPasswordError: String? = null,
)
```

新增方法：
- `submitEmail()` — 提取域名 → `authRepository.lookupOrg()` → 按 authType 切换步骤
- `requestMagicLink()` — 发送 magic link 邮件
- `verifyMagicLink(token: String)` — 验证 token → 登录
- `goBack()` — 从任何步骤回到 EmailInput

### 2.6 LoginScreen UI 改造

按 `authStep` 渲染不同 Composable：

- **EmailInput**: 邮箱输入框 + "继续" 按钮（原来直接显示邮箱+密码，改为分步）
- **OrgLookupLoading**: CircularProgressIndicator + "查询组织配置…"
- **PasswordInput**: 邮箱（只读显示）+ 密码框 + "登录" 按钮 + "发送登录链接" 文本按钮 + "忘记密码" 链接
- **MagicLinkSent**: 邮件图标 + "登录链接已发送到 {email}" + "重新发送" 按钮 + "返回" 按钮
- **SSORedirect**: "正在跳转到 {orgName} 登录…" + 自动打开 CustomTabs

### 2.7 SSO 回调

CustomTabs 打开 ssoUrl 后，SSO 提供方完成认证后会回调到 `https://app.mistyislet.com/sso/callback?token=xxx`。

通过 App Links intent-filter 捕获 → MainActivity.onNewIntent → 提取 token → 走 `verifyMagicLink` 相同的 token 存储路径（后端两种 token 格式相同，都是 JWT）。

### 2.8 新增依赖

`build.gradle.kts`:
```kotlin
implementation("androidx.browser:browser:1.8.0")  // CustomTabs for SSO
```

---

## 3. 地理围栏

### 3.1 架构

```
ProfileScreen toggle ON
    ↓ 权限请求 (FINE → BACKGROUND)
GeofenceManager.syncGeofences(doors)
    ↓
GeofencingClient.addGeofences()  (50m 半径, ENTER 触发)
    ↓ 用户进入围栏
GeofenceBroadcastReceiver.onReceive()
    ↓
NotificationManager → "你已到达 {doorName} 附近"
    ↓ 用户点击通知
PendingIntent → mistyislet://unlock/{doorId}
```

### 3.2 GeofenceManager

新增 `core/geofence/GeofenceManager.kt`：

```kotlin
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = LocationServices.getGeofencingClient(context)
    private val activeGeofenceIds = mutableSetOf<String>()

    fun syncGeofences(doors: List<AccessibleDoor>) { ... }
    fun clearAll() { ... }
    fun isPermissionGranted(): Boolean { ... }
}
```

关键逻辑：
- `syncGeofences`: 过滤有经纬度的门 → 对比 `activeGeofenceIds` → 增删差异
- 每个 Geofence: requestId = doorId, radius = 50f, ENTER 触发, 无过期
- PendingIntent 指向 GeofenceBroadcastReceiver
- 最多 100 个围栏（Android 限制）

### 3.3 GeofenceBroadcastReceiver

新增 `core/geofence/GeofenceBroadcastReceiver.kt`：

- 解析 `GeofencingEvent.fromIntent(intent)`
- ENTER 事件 → 从 triggeringGeofences 拿 requestId (doorId)
- 查 Room `CachedDoor` 拿门名称
- 发送本地通知：
  - Channel: `CHANNEL_ACCESS`（复用现有推送通道）
  - 点击 intent: `mistyislet://unlock/{doorId}`（走 deep link）

### 3.4 权限流程

Android 11+ 要求分步请求位置权限：

1. 请求 `ACCESS_FINE_LOCATION` → 用户授权
2. 请求 `ACCESS_BACKGROUND_LOCATION` → 系统弹出单独对话框，解释"始终允许"
3. 如果任一被拒 → 显示 Snackbar "需要位置权限才能使用自动感应开锁"

权限请求在 ProfileScreen 中用 `rememberLauncherForActivityResult` 处理。

### 3.5 生命周期

- 登录后 + 围栏开关开启 → `syncGeofences()`
- 门禁列表刷新后 → `syncGeofences()`
- 围栏开关关闭 → `clearAll()`
- 退出登录 → `clearAll()`

### 3.6 AndroidManifest 变更

```xml
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<receiver
    android:name=".core.geofence.GeofenceBroadcastReceiver"
    android:exported="false" />
```

### 3.7 新增依赖

`build.gradle.kts`:
```kotlin
implementation("com.google.android.gms:play-services-location:21.3.0")
```

---

## 4. 文件变更汇总

### 新增文件 (3)

| 文件 | 行数估计 | 用途 |
|------|----------|------|
| `core/deeplink/DeepLinkHandler.kt` | ~40 | Intent 解析、magic-link token 提取 |
| `core/geofence/GeofenceManager.kt` | ~120 | 围栏注册/同步/清除 |
| `core/geofence/GeofenceBroadcastReceiver.kt` | ~70 | 围栏事件→本地通知 |

### 修改文件 (11)

| 文件 | 改动说明 |
|------|----------|
| `AndroidManifest.xml` | +2 intent-filter, +1 permission, +1 receiver |
| `build.gradle.kts` | +2 依赖 (browser, play-services-location) |
| `data/api/AuthApi.kt` | +3 方法 (orgLookup, requestMagicLink, verifyMagicLink) |
| `domain/model/ApiModels.kt` | +4 数据类 (OrgAuthConfig, MagicLinkRequest/Response, VerifyMagicLinkRequest) |
| `data/repository/AuthRepository.kt` | +3 方法 |
| `ui/login/LoginViewModel.kt` | 重构: AuthStep 枚举 + 状态机 + 4 新方法 |
| `ui/login/LoginScreen.kt` | 分步 UI: 5 个 Composable 段 |
| `ui/navigation/AppNavigation.kt` | 所有路由增加 deepLinks 参数 |
| `MainActivity.kt` | +onNewIntent, deep link 处理, magic-link token 传递 |
| `data/repository/DoorRepository.kt` | 刷新后调用 geofenceManager.syncGeofences() |
| `ui/profile/ProfileViewModel.kt` | 围栏开关实际调用 GeofenceManager |

---

## 5. 不在范围内

- SSO SAML 元数据解析（后端处理，客户端只需打开 URL）
- App Links 服务端 `assetlinks.json`（需后端部署，本次不涉及）
- 围栏相关的 UI 地图展示（P2 优化项）
- Lock Screen Widget 扩展（P3）
