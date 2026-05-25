# MistyPass Android — 真机调试环境搭建

## 前置条件

| 依赖 | 版本 | 安装 |
|------|------|------|
| Android Studio | Hedgehog+ | https://developer.android.com/studio |
| JDK | 17 | `brew install openjdk@17` |
| Go | 1.22+ | `brew install go` |
| PostgreSQL | 15+ | `brew install postgresql@15 && brew services start postgresql@15` |
| ADB | - | 随 Android Studio 安装，或 `brew install android-platform-tools` |

## 1. 启动 PostgreSQL

```bash
# 确认 PostgreSQL 运行中
brew services list | grep postgresql

# 创建数据库（首次）
createdb mistypass
```

## 2. 启动后端 API

```bash
cd /Users/siky/code/mistypass/api

# 构建
go build -o /tmp/mistypass-api ./cmd/api

# 启动（最小必要环境变量）
DATABASE_URL="postgres://siky@localhost:5432/mistypass?sslmode=disable" \
DATABASE_AUTO_MIGRATE=true \
ENABLE_DEMO_USERS=true \
JWT_SECRET="mistypass-dev-secret-2026" \
/tmp/mistypass-api
```

后端默认监听 **:8080**。启动成功会看到：
```
{"level":"INFO","msg":"postgres state store enabled"}
{"level":"INFO","msg":"mistypass api listening","addr":":8080"}
```

### 后端环境变量速查

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PORT` | `8080` | API 监听端口 |
| `DATABASE_URL` | (空) | PostgreSQL 连接串，**不设则无持久化** |
| `DATABASE_AUTO_MIGRATE` | `true` | 自动执行数据库迁移 |
| `ENABLE_DEMO_USERS` | `false` | **必须设为 true**，否则无法登录 |
| `JWT_SECRET` | (自动生成) | **开发环境建议固定**，否则重启后 token 失效 |
| `REDIS_ADDR` | (空) | 可选，用于会话存储 |

## 3. ADB 端口转发

手机的 `localhost` 指手机自己，需要把手机的 8080 端口转发到 Mac 的 8080：

```bash
# USB 连接手机后
adb devices              # 确认设备已连接
adb reverse tcp:8080 tcp:8080  # 端口转发
```

> 每次断开 USB 重连后都需要重新执行 `adb reverse`。

## 4. 构建安装 APK

### 可选：Firebase / FCM

没有 `app/google-services.json` 时，构建仍会通过，登录后 FCM token 注册会被跳过。要测试真实推送：

1. 在 Firebase Console 创建 Android app，package name 使用 `com.mistyislet.app`。
2. 下载 `google-services.json`，放到 `app/google-services.json`。
3. 重新构建 staging/debug APK。
4. 后端 Mac mini 需要同时启用 `FCM_ENABLED=true` 并配置 Firebase service account。

```bash
cd /Users/siky/code/android-MistyisletPass

# 构建 debug APK
./gradlew assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug 版本使用 `LOCAL` 环境（`http://localhost:8080/api/v1/`），通过 `adb reverse` 转发到 Mac。

## 5. 登录测试

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `siky` | `65552588` | tenant_admin | 主测试账号，有 Admin 权限 |
| `tenant.admin@sudirman.co` | `admin123` | tenant_admin | Jakarta 租户管理员 |
| `superadmin@mistypass.local` | `admin123` | super_admin | 超级管理员 |
| `resident.jakarta@mistypass.local` | `admin123` | resident | 普通住户（无 Admin 权限） |

## 6. Admin 模块数据说明

| 模块 | 是否有 Demo 数据 | 说明 |
|------|:---:|------|
| Users | ✅ | 9 个 demo 用户 |
| Events | ✅ | 内存种子数据（访问事件） |
| Incidents | ✅ | 从 Events 中的拒绝/告警事件派生 |
| Activity | ✅ | 从 Events 中的成功事件派生 |
| Schedules | ✅ | 3 个时间表（Office Hours、24/7 Security、Weekend Maintenance） |
| Zones | ❌ | 后端无种子数据 |
| Cards | ✅ | 2 张实体卡（CARD-1001 reserved、CARD-1002 available） |
| Digital Credentials | ✅ | 设备注册后自动生成；后端含 3 个 demo 凭证 |
| Teams | ✅ | 3 个团队（Engineering、Operations、Factory Security） |
| Reports | ❌ | 静态页面，无后端数据 |

## 故障排查

### App 连不上后端
```bash
# 检查后端是否在运行
lsof -iTCP:8080 -sTCP:LISTEN -P

# 检查端口转发
adb reverse --list

# 重新转发
adb reverse tcp:8080 tcp:8080
```

### 登录返回 Unauthorized
- 确认后端启动时带了 `ENABLE_DEMO_USERS=true`
- 检查日志：`tail -f /tmp/mistypass.log`

### Admin 页面返回 Forbidden (403)
- 当前账号角色不够。Admin 需要 `super_admin`、`tenant_admin` 或 `building_admin`
- 检查角色：`psql -d mistypass -c "SELECT email, role FROM mistypass_auth_users;"`

### 断线重连后 App 请求失败
```bash
adb reverse tcp:8080 tcp:8080  # 重新端口转发
```

## 一键启动脚本

可以在项目根目录创建 `dev-start.sh`：

```bash
#!/bin/bash
set -e

echo "=== Starting PostgreSQL ==="
brew services start postgresql@15 2>/dev/null || true

echo "=== Building backend ==="
cd /Users/siky/code/mistypass/api
go build -o /tmp/mistypass-api ./cmd/api

echo "=== Starting backend ==="
DATABASE_URL="postgres://siky@localhost:5432/mistypass?sslmode=disable" \
DATABASE_AUTO_MIGRATE=true \
ENABLE_DEMO_USERS=true \
JWT_SECRET="mistypass-dev-secret-2026" \
/tmp/mistypass-api &

sleep 2
echo "=== Setting up ADB ==="
adb reverse tcp:8080 tcp:8080

echo "=== Building Android APK ==="
cd /Users/siky/code/android-MistyisletPass
./gradlew assembleDebug

echo "=== Installing APK ==="
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "=== Done! Login with siky / 65552588 ==="
```
