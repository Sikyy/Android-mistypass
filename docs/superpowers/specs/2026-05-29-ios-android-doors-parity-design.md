# iOS→Android Doors 高保真复刻 — 方法 & 保真标准

**日期：** 2026-05-29  **状态：** 待用户确认 → 执行
**策略：** 深度优先。先把 **Doors 页**在 Xiaomi 15 上调到真·高保真，锁定「测量 + 材质 + 真机对比」方法与保真标准，验证通过后用同一套配方铺到其它页。iOS 为唯一事实来源。

## 背景（为什么是这个方法）
- 上一次（2026-05-29 另一会话，37 提交，现存 `backup/parity-session-2026-05-29`）被用户判「不太行」回滚。**本次确认的失败原因：真机还原度不够** —— 具体是「尺寸/间距偏」「质感/颜色偏」「整体说不准」。字体（Roboto vs SF Pro）用户**未**勾选，非重点。
- 根因：过度依赖 Paparazzi（JVM 截图，渲染不出真实材质）与子代理自述，**没在真机上逐屏与 iOS 对比**。
- 当前 main **已含**一套较完整 iOS 复刻（teal `#62B7A8`、iOS 调色板、`MistyCard`、分段控件、状态胶囊、收藏、长按解锁）。本次是**把「八成像」打磨成「难分真假」**，非从零做。

## 范围（本期）
- 屏映射：Android `DoorsScreen`（place 的门列表，Orgs→Places→Doors，带返回）↔ iOS `PlaceDetailsView` + `AccessibleDoorCardView`。
- 元素：大标题/返回键、搜索框、分段控件(All/Favorites)、门卡（状态图标·名称·BLE·收藏星+标签·状态胶囊·分组名·长按解锁）、离线横幅、封锁横幅、解锁结果浮层、门详情 sheet。
- 不在本期：其它 tab/页（Wallet/Profile/Places/History…）—— 打样通过后再排期。

## 方法（核心，直接修复「真机还原度不够」）
1. **iOS 基准**：本机 iOS 模拟器渲染 `PlaceDetailsView`（必要时加 DEBUG 启动参数用 `PreviewData` 直渲、免登录）→ `xcrun simctl io booted screenshot` 截基准图，覆盖各状态。
2. **Android 取样**：加一个**仅 DEBUG、不影响线上**的入口，用 mock 数据渲染 `DoorsScreen` 的每种状态 → 编译装到 Xiaomi 15（`adb d766dd19`）→ 截图。
3. **对比闭环**：iOS vs Android 叠加/并排，量出每个元素的 dp 偏差 → 改 → 重装真机复核 → 直到一致。
4. **真机为准**：不以 Paparazzi 或子代理自述代替真机肉眼确认；每轮改动我自己跑构建并看真机截图。

## 度量规则 & 机型像素差异
- **pt→dp 1:1**：iOS 的 pt 值直接 = Android dp 值（两者物理尺寸同源）；字号 pt→sp。
- **颜色**：取 iOS 语义色 / 资产目录真实 hex（如品牌 `#62B7A8`）。
- **机型差异**（用户关切的「不同机型像素差异」）：全程 dp/sp（密度无关）+ `WindowInsets` 适配状态栏/导航条/挖孔。Xiaomi 15 的 dp 视口与宽高比异于 iPhone，**故不逐像素叠死**；目标 = 设计语言 + 每元素 dp 测量 + 动态安全区三者一致，真机肉眼难分。

## 已知待修（起点，真机叠图后再补全）
| 元素 | iOS | 当前 Android | 动作 |
|---|---|---|---|
| 卡片材质 | Liquid Glass（半透+柔影+发丝边） | `MistyCard` 纯白 elevation 0 无边 | 加柔和阴影 + ~0.5–1dp 发丝边 + 轻微材质感，做出「深度」 |
| 长按解锁高度 | 44 pt | `MistyUnlockButtonHeight=36dp` | 36→44dp |
| 卡片纵向内边距 | 16（`.padding()`） | `vertical=12dp` | 12→16dp |
| 状态胶囊字号 | `caption2`≈11 | `labelMedium`≈12 | →11sp |
| 长按进度填充透明度 | 0.30 | 0.28 | →0.30 |
| 长按时长 | 0.5s | `tween(500)` | 已一致 ✓ |
| 品牌色 | `#62B7A8` | `#62B7A8` | 已一致 ✓ |

## 验收标准
- Xiaomi 15 真机 Doors 与 iOS 并排**肉眼难分**；关键元素 dp 测量在 **~1–2dp** 内。
- **由用户看真机截图拍板**「就是这个程度」，方视为打样通过。

## 风险 / 笔记
- Liquid Glass 真模糊在浅色背景上很微弱；重点是**阴影 + 发丝边 + 半透的「深度」**而非强模糊；真机验证（RenderEffect API31+，Xiaomi 15=Android15 支持）。
- 建任何「缺失」屏前先 `grep` iOS 调用点 —— 上次误把 iOS 死代码（`QRPassView`/`SiteSwitcherView`，仅 `struct`+`#Preview`）当成缺失。
- DEBUG 入口与参照脚手架**不进线上**（DEBUG-gated / 不提交）。
- 构建环境：Android SDK `/opt/homebrew/share/android-commandlinetools`（`local.properties`→`sdk.dir=`），JDK17 / Gradle 8.9；本地装机可让 `key.properties` 指向 `~/.android/debug.keystore`。
