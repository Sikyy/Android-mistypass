# 创建访客选门（door_ids）实现计划 — iOS 先行 + Android 复刻

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 两端创建访客表单支持多选门并把 `door_ids` 传给后端；后端 400（越权门）错误在表单内可见，不被静默吞掉。

**Architecture:** 复用各端既有 place-doors 接口取门列表；表单加「可访问门」只读行 → 专用多选 sheet（搜索 + 勾选，Set 绑定）。iOS 错误改为表单内固定横幅（修复现在写到 sheet 背后列表页的 bug）；Android 把 `createGuest` 的 `ApiResult` 映射为 `CreateGuestState`（Idle/Success/Error），成功才关 sheet，错误显示在 `MistyFormSheet` 新增的固定错误横幅里。

**Tech Stack:** iOS：SwiftUI + XCTest（XcodeGen 工程 — 不新建文件，全部改既有文件，避免重新生成工程）。Android：Compose + Hilt + kotlinx.serialization + JUnit4 + MockWebServer。

**Spec:** `docs/superpowers/specs/2026-06-12-guest-door-ids-design.md`

**仓库与分支（不用 worktree，直接在两个 checkout 上做）：**
- iOS `/Users/siky/code/ios-MistyisletPass`：从当前 HEAD（`codex/ios-staging-scheme-2026-05-25`，干净）新建分支 `codex/guest-door-ids-2026-06-12`。
- Android `/Users/siky/code/android-MistyisletPass`：留在当前分支 `codex/android-ios-fidelity-2026-05-27`（spec 已提交在此）。

**已核实的关键事实（执行者不必重查）：**
- iOS `CreateGuestRequest`（`MistyisletPass/Models/Guest.swift:98-126`）已有 `doorIds: [String]`，CodingKey `door_ids`；`AdminGuestManagementView.swift:410` 硬编码 `doorIds: []`。
- iOS `APIService.fetchPlaceDoors(placeId:) async throws -> [AccessibleDoor]`（`Services/APIService.swift:180`）；`AccessibleDoor`（`Models/Door.swift:33`）有 `id/name/status/groupName`。
- iOS 本地化：`MistyisletPass/Resources/{en,zh-Hans,id}.lproj/Localizable.strings` 三文件行号对齐，`"guests.register"` 都在第 622 行；`"admin.search_doors"`、`"common.done"`、`"common.cancel"` 已存在。
- iOS 测试 target `MistyisletPassTests` 存在，模式参考 `AdminModelDecodingTests.swift`；模拟器可用 `iPhone 17 Pro`（iOS 26.4）。
- Android `CreateGuestRequest.doorIds`（`AdminModels.kt:493`，`@SerialName("door_ids")`，默认 `emptyList()`）；kotlinx 默认 `encodeDefaults=false` ⇒ 不选门时 `door_ids` 整个键省略 = 与现状字节级一致（现状从不发该键）。
- Android `PlaceRepository.listPlaceDoors(placeId): ApiResult<List<AccessibleDoor>>`（`data/repository/PlaceRepository.kt:28`，`@Singleton @Inject` 可直接进 Hilt VM）。
- Android `ApiResult.Error.message` 已解析 400 body 的 `message/error` 字段（`core/network/ApiResult.kt:24-32`）。
- Android `MistyFormSheet`（`ui/components/MistyIosComponents.kt:199-265`）：固定顶栏（Cancel|标题|Confirm），内容槽 `LazyListScope.() -> Unit`；`MistyPickerSheet` 在同文件 `:323` 起；`MistySearchField(value,onValueChange,placeholder,modifier)` 在 `:645`。
- Android `AdminDemoData.accessibleDoors` 存在（`ui/admin/AdminDemoData.kt:49`）；`GuestVisit` 仅 `id` 必填。
- Android `AdminGuestManagementContent` 仅两个调用点：包装器（`AdminGuestManagementScreen.kt:162`）与 `app/src/debug/.../ParityPreviewActivity.kt:394` —— 新参数带默认值即不破。
- Android 单测无 mockk/mockito；惯用法 = 抽纯函数测（`AdminDemoFallbackTest`）+ MockWebServer 测请求体（`AdminApiEndpointTest`，其中已有 share-access 的 `door_ids` 先例，`enqueueJson` 是该文件私有扩展）。
- Android 字符串：`values/`、`values-in/`、`values-zh-rCN/` 三份，`guest_access_duration` 都在第 377 行；`visitors_hours`=`%1$d hours` 已存在；**无** `done` 字符串（要新增）。

---

## Part 1 — iOS（先做，全部绿了再做 Part 2）

### Task 1: 分支 + 本地化 key

**Files:**
- Modify: `MistyisletPass/Resources/en.lproj/Localizable.strings:622`
- Modify: `MistyisletPass/Resources/zh-Hans.lproj/Localizable.strings:622`
- Modify: `MistyisletPass/Resources/id.lproj/Localizable.strings:622`

- [ ] **Step 1: 新建分支**

```bash
cd /Users/siky/code/ios-MistyisletPass
git status --short   # 必须为空；若有未提交改动先停下来问用户
git checkout -b codex/guest-door-ids-2026-06-12
```

- [ ] **Step 2: 三个 Localizable.strings 各自在 `"guests.register" = ...;` 行后追加 4 个 key**（保持三文件行号继续对齐，按下面语言各自的值）

en.lproj：
```text
"guests.accessible_doors" = "Accessible Doors";
"guests.select_doors" = "Select Doors";
"guests.doors_default" = "Default access";
"guests.doors_selected" = "%d selected";
```

zh-Hans.lproj：
```text
"guests.accessible_doors" = "可访问门";
"guests.select_doors" = "选择门";
"guests.doors_default" = "默认权限";
"guests.doors_selected" = "已选 %d 扇门";
```

id.lproj：
```text
"guests.accessible_doors" = "Pintu yang Dapat Diakses";
"guests.select_doors" = "Pilih Pintu";
"guests.doors_default" = "Akses bawaan";
"guests.doors_selected" = "%d dipilih";
```

- [ ] **Step 3: 验证三文件 key 数一致**

```bash
for f in MistyisletPass/Resources/*.lproj/Localizable.strings; do echo "$f: $(grep -c 'guests\.' $f)"; done
```
Expected: 三个文件数字相同。

### Task 2: CreateGuestRequest 编码回归测试

**Files:**
- Modify: `MistyisletPassTests/AdminModelDecodingTests.swift`（类结尾 `}` 前追加）

说明：`doorIds` 字段已存在，所以这是**回归测试**（首跑即绿），锁住 `door_ids` 键名与空数组行为，防后续改字段名/可选性时回归。

- [ ] **Step 1: 追加测试代码**

```swift
    // MARK: - CreateGuestRequest encoding

    private func makeCreateGuestRequest(doorIds: [String]) -> CreateGuestRequest {
        CreateGuestRequest(
            name: "Guest",
            email: nil,
            phone: "0812000111",
            company: nil,
            purpose: nil,
            hostName: "Host",
            hostEmail: nil,
            hostPhone: nil,
            idDocumentType: nil,
            idDocumentNumber: nil,
            expectedAt: nil,
            notifyHost: true,
            doorIds: doorIds,
            accessTtlHours: 24
        )
    }

    func testCreateGuestRequestEncodesDoorIds() throws {
        let data = try JSONEncoder().encode(makeCreateGuestRequest(doorIds: ["door-1", "door-2"]))
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        XCTAssertEqual(json["door_ids"] as? [String], ["door-1", "door-2"])
    }

    func testCreateGuestRequestEncodesEmptyDoorIdsAsEmptyArray() throws {
        let data = try JSONEncoder().encode(makeCreateGuestRequest(doorIds: []))
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        XCTAssertEqual(json["door_ids"] as? [String], [])
    }
```

- [ ] **Step 2: 跑这组测试**

```bash
cd /Users/siky/code/ios-MistyisletPass
xcodebuild test -project MistyisletPass.xcodeproj -scheme MistyisletPass \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -only-testing:MistyisletPassTests/AdminModelDecodingTests 2>&1 | tail -15
```
Expected: `** TEST SUCCEEDED **`

- [ ] **Step 3: Commit**

```bash
git add MistyisletPassTests/AdminModelDecodingTests.swift MistyisletPass/Resources
git commit -m "test: lock CreateGuestRequest door_ids encoding; add door-picker strings"
```

### Task 3: CreateGuestView — 门多选 + 表单内错误横幅

**Files:**
- Modify: `MistyisletPass/Views/Admin/AdminGuestManagementView.swift`（`CreateGuestView` 全部改动 + 文件尾新增 `DoorMultiSelectSheet` struct；不新建文件，避免 XcodeGen 重新生成）

- [ ] **Step 1: 给 `CreateGuestView` 加状态**（`@State private var isSubmitting = false`（现 317 行）之后插入）

```swift
    @State private var availableDoors: [AccessibleDoor] = []
    @State private var doorsLoading = true
    @State private var selectedDoorIds: Set<String> = []
    @State private var showDoorPicker = false
    @State private var errorMessage: String?
```

- [ ] **Step 2: body 包一层 VStack 放固定错误横幅，并挂 doors 的加载与 sheet**

现结构是 `NavigationStack { Form {...}.navigationTitle(...)...toolbar {...} }`。改为（`Form { ... }` 的全部既有 Section 内容原样保留）：

```swift
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if let errorMessage {
                    Label(errorMessage, systemImage: "exclamationmark.triangle")
                        .font(.caption)
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(.red.opacity(0.1))
                }
                Form {
                    // …既有各 Section 原样 + Step 3 的新 Section…
                }
            }
            .navigationTitle(settings.L("guests.new_guest"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                // …既有两个 ToolbarItem 原样…
            }
            .task { await loadDoors() }
            .sheet(isPresented: $showDoorPicker) {
                DoorMultiSelectSheet(
                    doors: availableDoors,
                    isLoading: doorsLoading,
                    selectedDoorIds: $selectedDoorIds
                )
            }
        }
    }
```

横幅在 Form 外、导航栏下方固定 —— 不管表单滚到哪都可见（Register 在固定 toolbar 上，错误必须同样固定才不会错过）。

- [ ] **Step 3: Schedule Section 之后（`.pickerStyle(.segmented)` 所在 Section 的收尾 `}` 后）加「可访问门」Section**

```swift
                Section(settings.L("guests.accessible_doors")) {
                    Button {
                        showDoorPicker = true
                    } label: {
                        HStack {
                            Text(settings.L("guests.select_doors"))
                                .foregroundStyle(.primary)
                            Spacer()
                            Text(selectedDoorIds.isEmpty
                                ? settings.L("guests.doors_default")
                                : String(format: settings.L("guests.doors_selected"), selectedDoorIds.count))
                                .foregroundStyle(.secondary)
                        }
                    }
                }
```

- [ ] **Step 4: `submit()` 改三处**

1. 开头 `isSubmitting = true` 后加 `errorMessage = nil`；
2. `doorIds: [],`（现 410 行）改为 `doorIds: selectedDoorIds.sorted(),`；
3. `catch` 里 `viewModel.errorMessage = error.localizedDescription` 改为 `errorMessage = error.localizedDescription`（局部，表单内可见；sheet 保持打开）。

- [ ] **Step 5: `CreateGuestView` 内加 `loadDoors()`（`submit()` 后面）**

```swift
    private func loadDoors() async {
        do {
            availableDoors = try await APIService.shared.fetchPlaceDoors(placeId: viewModel.placeId)
        } catch {
            // 门列表拉不到不阻塞建客；选择器显示空列表即可
        }
        #if DEBUG
        if availableDoors.isEmpty { availableDoors = PreviewData.accessibleDoors }
        #endif
        doorsLoading = false
    }
```

（DEBUG 回退与 `AddDoorToGroupSheet` 同款，`AdminCRUDViews.swift:899-901`。）

- [ ] **Step 6: 文件末尾新增 `DoorMultiSelectSheet`**

```swift
// MARK: - Door Multi-Select

struct DoorMultiSelectSheet: View {
    let doors: [AccessibleDoor]
    let isLoading: Bool
    @Binding var selectedDoorIds: Set<String>
    @State private var settings = SettingsService.shared
    @State private var searchText = ""
    @Environment(\.dismiss) private var dismiss

    private var filteredDoors: [AccessibleDoor] {
        if searchText.isEmpty { return doors }
        return doors.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }

    var body: some View {
        NavigationStack {
            List {
                if isLoading {
                    HStack { Spacer(); ProgressView(); Spacer() }
                } else {
                    ForEach(filteredDoors) { door in
                        Button {
                            if selectedDoorIds.contains(door.id) {
                                selectedDoorIds.remove(door.id)
                            } else {
                                selectedDoorIds.insert(door.id)
                            }
                        } label: {
                            HStack {
                                Image(systemName: "door.left.hand.closed")
                                    .foregroundStyle(door.status == "online" ? .green : .gray)
                                Text(door.name)
                                    .font(.subheadline)
                                    .foregroundStyle(.primary)
                                Spacer()
                                if selectedDoorIds.contains(door.id) {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(.brandPrimary)
                                }
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .searchable(text: $searchText, prompt: settings.L("admin.search_doors"))
            .navigationTitle(settings.L("guests.select_doors"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(settings.L("common.done")) { dismiss() }
                }
            }
        }
    }
}
```

（行样式抄 `AddDoorToGroupSheet`，把尾随 `plus.circle` 换成勾选态 `checkmark`。）

- [ ] **Step 7: 编译 + 全量该测试类**

```bash
cd /Users/siky/code/ios-MistyisletPass
xcodebuild build -project MistyisletPass.xcodeproj -scheme MistyisletPass \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' 2>&1 | tail -5
```
Expected: `** BUILD SUCCEEDED **`

```bash
xcodebuild test -project MistyisletPass.xcodeproj -scheme MistyisletPass \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -only-testing:MistyisletPassTests/AdminModelDecodingTests 2>&1 | tail -15
```
Expected: `** TEST SUCCEEDED **`

- [ ] **Step 8: Commit**

```bash
git add MistyisletPass/Views/Admin/AdminGuestManagementView.swift
git commit -m "feat: door multi-select in create-guest form; surface backend errors in-form"
```

---

## Part 2 — Android（iOS 绿了再开始）

### Task 4: 字符串资源（三语言）

**Files:**
- Modify: `app/src/main/res/values/strings.xml:377`（`guest_access_duration` 行后）
- Modify: `app/src/main/res/values-zh-rCN/strings.xml:377`
- Modify: `app/src/main/res/values-in/strings.xml:377`

- [ ] **Step 1: 三个文件各自在 `guest_access_duration` 行后追加**

values/：
```xml
    <string name="guest_section_doors">Accessible doors</string>
    <string name="guest_select_doors">Select doors</string>
    <string name="guest_doors_default">Default access</string>
    <string name="guest_doors_selected">%1$d selected</string>
    <string name="done">Done</string>
```

values-zh-rCN/：
```xml
    <string name="guest_section_doors">可访问门</string>
    <string name="guest_select_doors">选择门</string>
    <string name="guest_doors_default">默认权限</string>
    <string name="guest_doors_selected">已选 %1$d 扇门</string>
    <string name="done">完成</string>
```

values-in/：
```xml
    <string name="guest_section_doors">Pintu yang dapat diakses</string>
    <string name="guest_select_doors">Pilih pintu</string>
    <string name="guest_doors_default">Akses bawaan</string>
    <string name="guest_doors_selected">%1$d dipilih</string>
    <string name="done">Selesai</string>
```

- [ ] **Step 2: 快速校验**

```bash
cd /Users/siky/code/android-MistyisletPass
for f in app/src/main/res/values*/strings.xml; do echo "$f: $(grep -c 'guest_doors\|guest_select_doors\|guest_section_doors\|name="done"' $f)"; done
```
Expected: 三个文件都是 5。

### Task 5: `MistyFormSheet` 错误横幅 + `MistyMultiSelectPickerSheet` 组件

**Files:**
- Modify: `app/src/main/java/com/mistyislet/app/ui/components/MistyIosComponents.kt`

- [ ] **Step 1: `MistyFormSheet` 加 `errorMessage` 参数**（`:206` `confirmEnabled: Boolean = true,` 之后）

```kotlin
    errorMessage: String? = null,
```

- [ ] **Step 2: 头部 Box（`:255` 的 `}`）与 `LazyColumn`（`:257`）之间插入固定横幅**

```kotlin
            errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
```

确认按钮固定在顶栏，错误横幅也固定在顶栏下 —— 列表滚到哪都看得见（与 iOS 的 VStack 横幅同构）。

- [ ] **Step 3: `MistyPickerSheet` 函数结束后新增多选变体**（先读 `MistyPickerSheet` 收尾的 415-440 行确认行结构与分隔线写法，行 UI 与其保持一致）

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MistyMultiSelectPickerSheet(
    title: String,
    doneLabel: String,
    items: List<T>,
    itemLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onToggle: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    searchPlaceholder: String? = null,
    itemDetail: (T) -> String? = { null },
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val visibleItems = if (query.isBlank()) items else items.filter { itemLabel(it).contains(query, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 88.dp),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Text(doneLabel)
                }
            }
            searchPlaceholder?.let { placeholder ->
                MistySearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = placeholder,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    MistyGroupedSection {
                        visibleItems.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .clickable { onToggle(entry) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = itemLabel(entry),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 22.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    itemDetail(entry)?.takeIf { it.isNotBlank() }?.let { detail ->
                                        Text(
                                            text = detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (isSelected(entry)) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            if (index < visibleItems.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
```

如 `MistyPickerSheet` 415-440 行的分隔线/行尾写法与上面不同，以文件内既有写法为准微调（保持同文件一致）。缺失 import（如 `HorizontalDivider`）按编译器提示补。

- [ ] **Step 4: 编译确认**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res app/src/main/java/com/mistyislet/app/ui/components/MistyIosComponents.kt
git commit -m "feat(ui): MistyMultiSelectPickerSheet + pinned error banner in MistyFormSheet"
```

### Task 6: `CreateGuestState` 纯函数 — TDD

**Files:**
- Create: `app/src/test/java/com/mistyislet/app/ui/admin/CreateGuestStateTest.kt`
- Modify: `app/src/main/java/com/mistyislet/app/ui/admin/AdminGuestManagementScreen.kt`（VM 类前加 sealed interface + 映射函数）

- [ ] **Step 1: 写失败测试**

```kotlin
package com.mistyislet.app.ui.admin

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.domain.model.GuestVisit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class CreateGuestStateTest {

    @Test
    fun `success maps to Success`() {
        assertEquals(
            CreateGuestState.Success,
            createGuestStateOf(ApiResult.Success(GuestVisit(id = "guest-1"))),
        )
    }

    @Test
    fun `error maps to Error carrying backend message`() {
        assertEquals(
            CreateGuestState.Error("door door-9 does not belong to place place-1"),
            createGuestStateOf(ApiResult.Error(400, "door door-9 does not belong to place place-1")),
        )
    }

    @Test
    fun `exception maps to Error with throwable message`() {
        assertEquals(
            CreateGuestState.Error("no network"),
            createGuestStateOf(ApiResult.Exception(IOException("no network"))),
        )
    }

    @Test
    fun `exception without message maps to fallback text`() {
        assertEquals(
            CreateGuestState.Error("Unknown error"),
            createGuestStateOf(ApiResult.Exception(IOException())),
        )
    }
}
```

- [ ] **Step 2: 跑测试确认失败（未定义符号，编译失败即为「红」）**

```bash
./gradlew :app:testDebugUnitTest --tests "com.mistyislet.app.ui.admin.CreateGuestStateTest" 2>&1 | tail -10
```
Expected: FAILED（unresolved reference `CreateGuestState` / `createGuestStateOf`）

- [ ] **Step 3: 实现**（`AdminGuestManagementScreen.kt` 中 `@HiltViewModel` 注解前插入）

```kotlin
sealed interface CreateGuestState {
    data object Idle : CreateGuestState
    data object Success : CreateGuestState
    data class Error(val message: String) : CreateGuestState
}

internal fun createGuestStateOf(result: ApiResult<GuestVisit>): CreateGuestState = when (result) {
    is ApiResult.Success -> CreateGuestState.Success
    is ApiResult.Error -> CreateGuestState.Error(result.message)
    is ApiResult.Exception -> CreateGuestState.Error(result.throwable.message ?: "Unknown error")
}
```

- [ ] **Step 4: 跑测试确认通过**

```bash
./gradlew :app:testDebugUnitTest --tests "com.mistyislet.app.ui.admin.CreateGuestStateTest" 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/mistyislet/app/ui/admin/CreateGuestStateTest.kt app/src/main/java/com/mistyislet/app/ui/admin/AdminGuestManagementScreen.kt
git commit -m "feat: CreateGuestState mapping for guest-creation results (TDD)"
```

### Task 7: createGuest 请求体回归测试（MockWebServer）

**Files:**
- Modify: `app/src/test/java/com/mistyislet/app/data/api/AdminApiEndpointTest.kt`（仿照既有 `share access sends door_ids in request body` 先例；`CreateGuestRequest` 需要 import）

说明：模型字段已存在，首跑即绿——锁住键名 + 「不选门时键省略（兼容现状）」两个行为。

- [ ] **Step 1: 追加两个测试**

```kotlin
    @Test
    fun `create guest sends door_ids in request body`() = runTest {
        server.enqueueJson("""{"id":"guest-1","name":"Tamu","status":"expected"}""")

        api.createGuest(
            "place-1",
            CreateGuestRequest(name = "Tamu", doorIds = listOf("door-1", "door-2")),
        )

        val recorded = server.takeRequest()
        assertEquals("/api/v1/app/places/place-1/guests", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue("body should contain door_ids, was: $body", body.contains("\"door_ids\""))
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString(CreateGuestRequest.serializer(), body)
        assertEquals(listOf("door-1", "door-2"), parsed.doorIds)
    }

    @Test
    fun `create guest omits door_ids when none selected`() = runTest {
        server.enqueueJson("""{"id":"guest-2","name":"Tamu","status":"expected"}""")

        api.createGuest("place-1", CreateGuestRequest(name = "Tamu"))

        val body = server.takeRequest().body.readUtf8()
        // encodeDefaults=false：默认空列表整键省略 = 与改动前的请求体一致（兼容）
        assertEquals(false, body.contains("door_ids"))
    }
```

- [ ] **Step 2: 跑测试**

```bash
./gradlew :app:testDebugUnitTest --tests "com.mistyislet.app.data.api.AdminApiEndpointTest" 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`。若 `omits` 用例失败（说明 app 的 Retrofit Json 配了 `encodeDefaults=true`，请求体会是 `"door_ids":[]`，同样兼容）：把断言改为 `assertTrue(body.contains("\"door_ids\":[]"))` 并把注释改成对应说明。

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/mistyislet/app/data/api/AdminApiEndpointTest.kt
git commit -m "test: lock createGuest door_ids request-body contract"
```

### Task 8: VM + Content + CreateGuestSheet 接线

**Files:**
- Modify: `app/src/main/java/com/mistyislet/app/ui/admin/AdminGuestManagementScreen.kt`

- [ ] **Step 1: 补 import**

```kotlin
import androidx.compose.runtime.LaunchedEffect
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.ui.components.MistyMultiSelectPickerSheet
```

- [ ] **Step 2: VM 构造器加 `PlaceRepository`，新增两个 StateFlow + doors 加载 + createGuest 改造**

构造器（`:88`）：
```kotlin
class AdminGuestManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val placeRepository: PlaceRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
```

状态（`_error` 声明后）：
```kotlin
    private val _availableDoors = MutableStateFlow<List<AccessibleDoor>>(emptyList())
    val availableDoors: StateFlow<List<AccessibleDoor>> = _availableDoors
    private val _createGuestState = MutableStateFlow<CreateGuestState>(CreateGuestState.Idle)
    val createGuestState: StateFlow<CreateGuestState> = _createGuestState
```

`init` 块 `loadData()` 后加 `loadDoors()`，并新增私有方法（demo 回退与本文件 `loadData()` 既有口径一致）：
```kotlin
    private suspend fun loadDoors() {
        val pid = placeId ?: return
        _availableDoors.value = when (val result = placeRepository.listPlaceDoors(pid)) {
            is ApiResult.Success -> result.data.ifEmpty { AdminDemoData.accessibleDoors }
            else -> AdminDemoData.accessibleDoors
        }
    }
```

`createGuest`（`:117-123`）整体替换 —— 这是「静默吞 400」的修复点：
```kotlin
    fun createGuest(request: CreateGuestRequest) {
        val pid = placeId ?: return
        viewModelScope.launch {
            val result = adminRepository.createGuest(pid, request)
            _createGuestState.value = createGuestStateOf(result)
            if (result is ApiResult.Success) loadData()
        }
    }

    fun consumeCreateGuestState() {
        _createGuestState.value = CreateGuestState.Idle
    }
```

- [ ] **Step 3: 包装器（`:154-172`）收集并下传新状态**

```kotlin
    val availableDoors by viewModel.availableDoors.collectAsStateWithLifecycle()
    val createGuestState by viewModel.createGuestState.collectAsStateWithLifecycle()

    AdminGuestManagementContent(
        guests = guests,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        availableDoors = availableDoors,
        createGuestState = createGuestState,
        onConsumeCreateGuestState = viewModel::consumeCreateGuestState,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onCreateGuest = viewModel::createGuest,
        onUpdateStatus = viewModel::updateStatus,
        onDeleteGuest = viewModel::deleteGuest,
    )
```

- [ ] **Step 4: `AdminGuestManagementContent` 签名加默认参数（parity 调用点 `ParityPreviewActivity.kt:394` 不动也能编译）**

参数表追加：
```kotlin
    availableDoors: List<AccessibleDoor> = emptyList(),
    createGuestState: CreateGuestState = CreateGuestState.Idle,
    onConsumeCreateGuestState: () -> Unit = {},
```

`showCreateSheet` 声明后加成功联动：
```kotlin
    LaunchedEffect(createGuestState) {
        if (createGuestState is CreateGuestState.Success) {
            showCreateSheet = false
            onConsumeCreateGuestState()
        }
    }
```

`if (showCreateSheet) { CreateGuestSheet(...) }`（`:279-287`）替换为（**注意：onSave 不再立刻关 sheet** —— 成功靠上面的 LaunchedEffect 关，失败留在表单上看错误）：
```kotlin
    if (showCreateSheet) {
        CreateGuestSheet(
            availableDoors = availableDoors,
            errorMessage = (createGuestState as? CreateGuestState.Error)?.message,
            onSave = onCreateGuest,
            onCancel = {
                onConsumeCreateGuestState()
                showCreateSheet = false
            },
        )
    }
```

- [ ] **Step 5: `CreateGuestSheet` 加参数与门选择**

签名（`:451-454`）：
```kotlin
private fun CreateGuestSheet(
    availableDoors: List<AccessibleDoor>,
    errorMessage: String?,
    onSave: (CreateGuestRequest) -> Unit,
    onCancel: () -> Unit,
) {
```

状态区（`showDatePicker` 后）：
```kotlin
    var selectedDoorIds by remember { mutableStateOf(setOf<String>()) }
    var showDoorPicker by remember { mutableStateOf(false) }
```

`MistyFormSheet(...)` 调用加 `errorMessage = errorMessage,`（`confirmEnabled` 旁）。

`CreateGuestRequest(...)` 构造（`:492-506`）`notifyHost = notifyHost,` 后加：
```kotlin
                    doorIds = selectedDoorIds.sorted(),
```

Schedule section 的 `item {}` 之后加新 section：
```kotlin
        item {
            MistyGroupedSection(title = stringResource(R.string.guest_section_doors)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyReadonlyField(
                        value = if (selectedDoorIds.isEmpty()) {
                            stringResource(R.string.guest_doors_default)
                        } else {
                            stringResource(R.string.guest_doors_selected, selectedDoorIds.size)
                        },
                        label = stringResource(R.string.guest_select_doors),
                        onClick = { showDoorPicker = true },
                    )
                }
            }
        }
```

文件尾部 `showIdTypePicker`/`showDatePicker` 的弹层旁加：
```kotlin
    if (showDoorPicker) {
        MistyMultiSelectPickerSheet(
            title = stringResource(R.string.guest_select_doors),
            doneLabel = stringResource(R.string.done),
            items = availableDoors,
            itemLabel = { it.name },
            itemDetail = { it.groupName },
            isSelected = { it.id in selectedDoorIds },
            onToggle = { door ->
                selectedDoorIds = if (door.id in selectedDoorIds) selectedDoorIds - door.id else selectedDoorIds + door.id
            },
            onDismiss = { showDoorPicker = false },
            searchPlaceholder = stringResource(R.string.guest_select_doors),
        )
    }
```

- [ ] **Step 6: 全量验证**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -5 && ./gradlew :app:assembleDebug 2>&1 | tail -5
```
Expected: 两个都 `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/admin/AdminGuestManagementScreen.kt
git commit -m "feat: door multi-select in create-guest form; stop swallowing createGuest errors"
```

---

## 验收（两端共用，需要可用后端；没有就标注为待真机验收）

1. 选 1-2 扇门创建 guest → 后端返回的 guest `door_ids` 非空且等于所选。
2. 不选门创建 → `door_ids` 为空（与改动前行为一致）。
3. 构造越权门（其它 place 的 door id）→ 表单顶部显示后端 400 的 message，表单不关闭。
4. iOS `xcodebuild build` 绿 + `AdminModelDecodingTests` 绿；Android `:app:testDebugUnitTest` 与 `:app:assembleDebug` 绿。
