# 创建访客支持选门（door_ids）— iOS + Android 设计

**日期：** 2026-06-12  **状态：** 已确认 → 执行
**顺序：** iOS 先做并验证（作为 UI 基准），Android 复刻。两端各自仓库分别提交。

## 背景
- 后端已支持创建访客携带 `door_ids` 并做归属校验（server commit 03536c5 + B-2：选了不属于该 place 的门返回 400）。来源：server 仓库 `docs/CODE-REVIEW-2026-06-10.md` 的 M-2、`docs/mobile-followups-2026-06-12.md`。
- 两端模型字段**均已存在**，只是没接 UI：
  - Android：`CreateGuestRequest.doorIds`（`AdminModels.kt:493`，`@SerialName("door_ids")`，缺省 `emptyList()`），调用点 `AdminGuestManagementScreen.kt` 的 `CreateGuestSheet` 构造请求时未传。
  - iOS：`CreateGuestRequest.doorIds: [String]`（`Guest.swift:111`，CodingKey `door_ids`），`AdminGuestManagementView.swift:410` 硬编码 `doorIds: []`。

## 需求
1. 创建访客表单加「可访问门」多选控件，门列表取当前 place 的 doors（复用现有 place doors 接口，无新增网络层）。
2. 提交时把所选门 ID 作为 `door_ids` 传给 createGuest。
3. 错误处理：后端越权门返回 400，前端必须把后端错误信息展示给用户，**不静默吞**；表单保持打开。
4. 兼容：不选门 → `door_ids` 为空数组，后端行为与现状一致。

## 共同设计（两端一致的形态）
- **数据源**：`GET /app/places/{placeId}/doors` → `AccessibleDoor` 列表（两端均已有该接口封装）。
- **UI 形态**：表单内一行「可访问门」只读字段，显示已选数量/名称摘要；点击弹出**专用多选 sheet**（带搜索的勾选列表，Set 绑定）。不选 = 空。
- **错误展示**：400 的后端 message 必须显示在**打开着的创建表单之上/之内**，而不是被 sheet 挡住的列表页。

## iOS（先做，作为基准）
仓库：`/Users/siky/code/ios-MistyisletPass`，文件 `MistyisletPass/Views/Admin/AdminGuestManagementView.swift`（`CreateGuestView`）。
- 新增 `@State`：`availableDoors: [AccessibleDoor]`、`selectedDoorIds: Set<String>`、`doorsLoading`、**局部** `errorMessage: String?`。
- `.task { availableDoors = try await APIService.shared.fetchPlaceDoors(placeId: viewModel.placeId) }`（`APIService.swift:180`）。
- 新增「Accessible doors」`Section`：摘要行 → 弹新建的 `DoorMultiSelectSheet`（searchable 勾选列表），参考既有 `AddDoorToGroupSheet`（`AdminCRUDViews.swift:840`）。
- `submit()`：`doorIds: Array(selectedDoorIds)` 替换硬编码 `[]`。
- **顺带修复**：现 `catch` 把错误写到 `viewModel.errorMessage`，渲染在 sheet 背后的列表页（`AdminGuestManagementView.swift:147`），用户在表单里看不到 → 改为写**局部** `errorMessage`，在表单内显示，sheet 不关闭。
- 文案走 `settings.L(...)` 本地化，补对应 key。

## Android（复刻 iOS）
仓库：`/Users/siky/code/android-MistyisletPass`。
- **ViewModel**（`AdminGuestManagementScreen.kt:88`）：注入 `PlaceRepository`；`placeId` 解析后加载 `availableDoors: StateFlow<List<AccessibleDoor>>`。
- **修复静默吞错**：现 `createGuest()` 不检查 `ApiResult`（`:117-123`）。改为：`Success` → 发成功信号（关 sheet）+ `loadData()`；`Error` → 发布 `createError =` 后端 `message`（`ApiResult.kt:24-32` 已解析 400 body）；`Exception` → 发布 `throwable.message`。
- **无状态层**（`AdminGuestManagementContent`，供 parity harness/preview 复用）：新增**带默认值**的参数：`availableDoors`、`createError`、`onClearCreateError`、成功 token（用于关 sheet），保证既有调用点不破。
- **`CreateGuestSheet`**：加 `selectedDoorIds: Set<String>` 状态 + 「可访问门」`MistyReadonlyField` → 弹新建的 `MistyMultiSelectPickerSheet`（`MistyPickerSheet` 的 Set 多选变体，`MistyIosComponents.kt:323` 旁）；构造请求时 `doorIds = selectedDoorIds.toList()`；`createError` 在表单内联显示，sheet 出错不关。
- 文案进 `strings.xml`（含现有的多语言资源目录同步）。

## 测试 / 验证
- **iOS**：encodable 单测断言 `CreateGuestRequest` 选门时 JSON 含 `door_ids`、不选时为 `[]`；`xcodebuild build`（或既有 CI 命令）绿。
- **Android**：ViewModel 单测覆盖 createGuest 错误路径（fake repo 返回 `ApiResult.Error(400, msg)` → `createError` 暴露 msg；Success → 关 sheet 信号）；`./gradlew :app:assembleDebug` 绿。
- **验收**（对后端）：选门创建的 guest 返回 `door_ids` 非空；不选门为空；选越权门时表单显示后端错误文案。

## 不做（YAGNI）
- 不做门的分组/层级选择，只做平铺搜索勾选列表。
- 不做编辑既有 guest 的门、不做按门的 TTL。
- 不动 share-access / visitor-pass 等其它流程的门选择。
