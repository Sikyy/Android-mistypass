package com.mistyislet.app.ui.admin

import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.AccessRight
import com.mistyislet.app.domain.model.Alarm
import com.mistyislet.app.domain.model.AlarmCalendarEntry
import com.mistyislet.app.domain.model.AlarmSchedule
import com.mistyislet.app.domain.model.AdminCard
import com.mistyislet.app.domain.model.AdminDigitalCredential
import com.mistyislet.app.domain.model.AdminEvent
import com.mistyislet.app.domain.model.AdminGroup
import com.mistyislet.app.domain.model.AdminIncident
import com.mistyislet.app.domain.model.AdminSchedule
import com.mistyislet.app.domain.model.AdminTeam
import com.mistyislet.app.domain.model.AdminUser
import com.mistyislet.app.domain.model.AdminZone
import com.mistyislet.app.domain.model.AnalyticsSummary
import com.mistyislet.app.domain.model.Camera
import com.mistyislet.app.domain.model.DailyTrendPoint
import com.mistyislet.app.domain.model.FailedAttemptEvent
import com.mistyislet.app.domain.model.GroupDoor
import com.mistyislet.app.domain.model.GroupMember
import com.mistyislet.app.domain.model.GuestVisit
import com.mistyislet.app.domain.model.HeatmapCell
import com.mistyislet.app.domain.model.Holiday
import com.mistyislet.app.domain.model.HolidayRegion
import com.mistyislet.app.domain.model.IncidentEvent
import com.mistyislet.app.domain.model.IncidentOccurrence
import com.mistyislet.app.domain.model.LiveActivityRecord
import com.mistyislet.app.domain.model.MethodCount
import com.mistyislet.app.domain.model.RelatedAdminEvent
import com.mistyislet.app.domain.model.TeamAccessRight
import com.mistyislet.app.domain.model.TeamMember
import com.mistyislet.app.domain.model.TopDoor
import com.mistyislet.app.domain.model.UserLogin
import com.mistyislet.app.domain.model.UserPresenceRecord
import com.mistyislet.app.domain.model.WeeklyUserPoint
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AdminDemoData {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val accessibleDoors = listOf(
        AccessibleDoor(
            id = "door-001",
            name = "Main Entrance",
            buildingId = "b1",
            status = "online",
            gatewayStatus = "online",
            gatewayId = "gw-001",
            gatewayName = "Gateway Lobby",
            groupName = "Lobby",
            canUnlock = true,
            isFavorite = true,
            lastUnlockAt = "2026-05-07T08:12:00Z",
            kind = "door",
        ),
        AccessibleDoor(
            id = "door-002",
            name = "Server Room",
            buildingId = "b2",
            status = "online",
            gatewayStatus = "online",
            gatewayId = "gw-002",
            gatewayName = "Gateway DC",
            groupName = "Data Center",
            canUnlock = true,
            lastUnlockAt = "2026-05-07T07:45:00Z",
            kind = "door",
        ),
        AccessibleDoor(
            id = "door-003",
            name = "Parking Gate",
            buildingId = "b1",
            status = "online",
            gatewayStatus = "offline",
            gatewayId = "gw-003",
            gatewayName = "Gateway Parking",
            groupName = "Parking",
            canUnlock = false,
            lastUnlockAt = "2026-05-06T18:30:00Z",
            kind = "turnstile",
        ),
        AccessibleDoor(
            id = "door-004",
            name = "Meeting Room A",
            buildingId = "b1",
            status = "online",
            gatewayStatus = "online",
            gatewayId = "gw-001",
            gatewayName = "Gateway Lobby",
            groupName = "Lobby",
            canUnlock = true,
            lastUnlockAt = "2026-05-07T09:02:00Z",
            kind = "door",
        ),
        AccessibleDoor(
            id = "door-005",
            name = "Executive Suite",
            buildingId = "b1",
            status = "locked_down",
            gatewayStatus = "online",
            gatewayId = "gw-001",
            gatewayName = "Gateway Lobby",
            groupName = "Office",
            canUnlock = false,
            kind = "door",
        ),
    )

    val cameras = listOf(
        Camera(
            id = "cam-001",
            name = "Lobby Camera 1",
            provider = "Hikvision DS-2CD2143G2-I",
            status = "online",
            host = "192.168.1.101",
            port = 554,
            doorId = "Main Entrance",
        ),
        Camera(
            id = "cam-002",
            name = "Server Room Camera",
            provider = "Dahua IPC-HDW3849H",
            status = "online",
            host = "192.168.1.102",
            port = 554,
            doorId = "Server Room",
        ),
        Camera(
            id = "cam-003",
            name = "Parking Entrance",
            provider = "Hikvision DS-2CD2T47G2-L",
            status = "offline",
            host = "192.168.1.103",
            port = 554,
            doorId = "Parking Gate",
        ),
        Camera(
            id = "cam-004",
            name = "Executive Floor",
            provider = "Axis P3265-V",
            status = "online",
            host = "192.168.1.104",
            port = 554,
            doorId = "Executive Suite",
        ),
    )

    val groups = listOf(
        AdminGroup(
            id = "grp-001",
            name = "Lobby Access",
            description = "Access to lobby and main entrance doors",
            memberCount = 12,
            doorCount = 3,
        ),
        AdminGroup(
            id = "grp-002",
            name = "Server Room",
            description = "Restricted server room access",
            memberCount = 4,
            doorCount = 1,
        ),
        AdminGroup(
            id = "grp-003",
            name = "Executive Floor",
            description = "Executive suite and meeting rooms",
            memberCount = 6,
            doorCount = 2,
        ),
        AdminGroup(
            id = "grp-004",
            name = "Parking",
            description = "Parking gate access",
            memberCount = 18,
            doorCount = 1,
        ),
    )

    val groupMembers = listOf(
        GroupMember("gm-001", "Ahmad Wijaya", "ahmad@example.com", "door_access"),
        GroupMember("gm-002", "Siti Rahayu", "siti@example.com", "door_access"),
        GroupMember("gm-003", "Budi Santoso", "budi@example.com", "group_manager"),
    )

    val groupDoors = listOf(
        GroupDoor("door-001", "Main Entrance", "online"),
        GroupDoor("door-004", "Meeting Room A", "online"),
        GroupDoor("door-005", "Executive Suite", "locked_down"),
    )

    val teams = listOf(
        AdminTeam(
            id = "team-001",
            name = "Facilities",
            description = "Building operations and facility access",
            memberCount = 8,
        ),
        AdminTeam(
            id = "team-002",
            name = "Security Ops",
            description = "Security desk and response team",
            memberCount = 5,
        ),
        AdminTeam(
            id = "team-003",
            name = "Engineering",
            description = "Engineering office access",
            memberCount = 12,
        ),
    )

    val teamMembers = listOf(
        TeamMember("tm-001", "Ahmad Wijaya", "ahmad@example.com", "member"),
        TeamMember("tm-002", "Dewi Lestari", "dewi@example.com", "member"),
        TeamMember("tm-003", "Eko Prasetyo", "eko@example.com", "member"),
    )

    val teamAccessRights = listOf(
        TeamAccessRight(
            id = "ar-001",
            doorName = "Lobby Access",
            scheduleName = "Business Hours",
            accessType = "door_access",
        ),
        TeamAccessRight(
            id = "ar-002",
            doorName = "Sudirman Hub",
            scheduleName = "Always",
            accessType = "place_door_access",
        ),
    )

    val placeUsers = listOf(
        AdminUser(
            id = "u1",
            name = "Ahmad Wijaya",
            email = "ahmad@example.com",
            role = "place_administrator",
            status = "active",
            lastActivity = "2026-05-07T10:00:00Z",
            createdAt = "2025-01-01",
        ),
        AdminUser(
            id = "u2",
            name = "Siti Rahayu",
            email = "siti@example.com",
            role = "group_manager",
            status = "active",
            lastActivity = "2026-05-06T15:00:00Z",
            createdAt = "2025-02-01",
        ),
        AdminUser(
            id = "u3",
            name = "John Chen",
            email = "john@example.com",
            role = "door_access",
            status = "active",
            lastActivity = "2026-05-05T09:00:00Z",
            createdAt = "2025-03-01",
        ),
        AdminUser(
            id = "u4",
            name = "Maria Santos",
            email = "maria@example.com",
            role = "observer",
            status = "active",
            createdAt = "2025-04-01",
        ),
    )

    val userLogins = listOf(
        UserLogin("login-001", "Xiaomi 15", "android", "2026-05-07T12:30:00Z", true),
        UserLogin("login-002", "MacBook Pro", "web", "2026-05-07T10:15:00Z"),
        UserLogin("login-003", "iPhone 17 Pro", "ios", "2026-05-06T18:42:00Z"),
        UserLogin("login-004", "iPad Air", "ipados", "2026-05-05T09:20:00Z"),
    )

    val userAccessRights = listOf(
        AccessRight(doorId = "door-001", doorName = "Main Entrance", kind = "door", status = "active", source = "group", canAccess = true),
        AccessRight(doorId = "door-002", doorName = "Parking Gate", kind = "gate", status = "active", source = "role", canAccess = true),
        AccessRight(doorId = "door-003", doorName = "Meeting Room A", kind = "door", status = "active", source = "group+role", canAccess = true),
    )

    val schedules = listOf(
        AdminSchedule(
            id = "schedule-001",
            name = "Business Hours",
            description = "Weekday access window",
            type = "unlock",
            daysOfWeek = listOf(1, 2, 3, 4, 5),
            startTime = "08:00",
            endTime = "18:00",
            enabled = true,
        ),
        AdminSchedule(
            id = "schedule-002",
            name = "Security Shift",
            description = "Security team overnight coverage",
            type = "first_to_arrive",
            daysOfWeek = listOf(0, 1, 2, 3, 4, 5, 6),
            startTime = "18:00",
            endTime = "07:00",
            enabled = true,
        ),
        AdminSchedule(
            id = "schedule-003",
            name = "Visitor Window",
            description = "Visitor pass validity",
            type = "unlock",
            daysOfWeek = listOf(1, 2, 3, 4, 5),
            startTime = "09:00",
            endTime = "17:00",
            enabled = true,
        ),
    )

    val zones = listOf(
        AdminZone(
            id = "zone-001",
            placeId = "p1",
            name = "Lobby",
            description = "Main entrance, reception, and guest waiting area",
            status = "active",
            doorCount = 3,
            cameraCount = 1,
            holidayRegionCount = 1,
            createdAt = "2026-01-15",
        ),
        AdminZone(
            id = "zone-002",
            placeId = "p1",
            name = "Data Center",
            description = "Restricted server room zone",
            status = "active",
            doorCount = 1,
            cameraCount = 1,
            holidayRegionCount = 1,
            createdAt = "2026-02-01",
        ),
        AdminZone(
            id = "zone-003",
            placeId = "p1",
            name = "Parking",
            description = "Vehicle gate and basement access",
            status = "active",
            doorCount = 1,
            cameraCount = 1,
            holidayRegionCount = 1,
            createdAt = "2026-03-01",
        ),
    )

    val holidayRegions = listOf(
        HolidayRegion(
            id = "hr-id-jk",
            name = "Indonesia Public Holidays",
            countryCode = "ID",
            regionCode = "JK",
            timezone = "Asia/Jakarta",
            holidayCount = 16,
        ),
        HolidayRegion(
            id = "hr-id",
            name = "Indonesia National",
            countryCode = "ID",
            timezone = "Asia/Jakarta",
            holidayCount = 14,
        ),
    )

    val holidaysByRegion = mapOf(
        "hr-id-jk" to listOf(
            Holiday("holiday-001", "hr-id-jk", "New Year's Day", "2026-01-01", "public"),
            Holiday("holiday-002", "hr-id-jk", "Eid al-Fitr", "2026-03-20", "public"),
            Holiday("holiday-003", "hr-id-jk", "Independence Day", "2026-08-17", "public"),
        ),
        "hr-id" to listOf(
            Holiday("holiday-004", "hr-id", "Pancasila Day", "2026-06-01", "public"),
            Holiday("holiday-005", "hr-id", "Christmas Day", "2026-12-25", "public"),
        ),
    )

    val cards = listOf(
        AdminCard(
            id = "card-001",
            uid = "04A1B2C3D4",
            cardNumber = "MP-1001",
            cardType = "mifare",
            status = "active",
            assignedTo = "Ahmad Wijaya",
            assignedEmail = "ahmad@example.com",
            lastUsed = "2026-05-07T08:12:00Z",
            issuedAt = "2026-01-15",
        ),
        AdminCard(
            id = "card-002",
            uid = "04E5F6A7B8",
            cardNumber = "MP-1002",
            cardType = "mifare",
            status = "active",
            assignedTo = "Siti Rahayu",
            assignedEmail = "siti@example.com",
            lastUsed = "2026-05-06T15:00:00Z",
            issuedAt = "2026-02-01",
        ),
        AdminCard(
            id = "card-003",
            uid = "04C9D0E1F2",
            cardNumber = "MP-1003",
            cardType = "mifare",
            status = "suspended",
            assignedTo = "Budi Santoso",
            assignedEmail = "budi@example.com",
            issuedAt = "2026-02-12",
        ),
    )

    val digitalCredentials = listOf(
        AdminDigitalCredential(
            id = "cred-001",
            deviceName = "iPhone 17 Pro",
            deviceModel = "iPhone 17 Pro",
            platform = "ios",
            credentialType = "wallet",
            status = "active",
            isActive = true,
            userName = "Ahmad Wijaya",
            userEmail = "ahmad@example.com",
            usageCount = 187,
            issuedAt = "2026-01-15",
            expiresAt = "2026-08-23",
        ),
        AdminDigitalCredential(
            id = "cred-002",
            deviceName = "Pixel 10",
            deviceModel = "Pixel 10",
            platform = "android",
            credentialType = "wallet",
            status = "active",
            isActive = true,
            userName = "Siti Rahayu",
            userEmail = "siti@example.com",
            usageCount = 156,
            issuedAt = "2026-02-01",
            expiresAt = "2026-09-12",
        ),
        AdminDigitalCredential(
            id = "cred-003",
            deviceName = "Visitor QR",
            platform = "qr",
            credentialType = "qr",
            status = "active",
            isActive = true,
            userName = "John Chen",
            userEmail = "john@example.com",
            usageCount = 12,
            issuedAt = "2026-05-01",
            expiresAt = "2026-05-31",
        ),
    )

    val guestVisits = listOf(
        GuestVisit(
            id = "guest-001",
            name = "John Doe",
            email = "john.doe@example.com",
            phone = "+62 812 0000 1001",
            company = "MistyPass",
            purpose = "Product demo",
            hostName = "Ahmad Wijaya",
            status = "expected",
            expectedAt = "2026-05-28T14:00:00Z",
        ),
        GuestVisit(
            id = "guest-002",
            name = "Jane Smith",
            email = "jane.smith@example.com",
            phone = "+62 812 0000 1002",
            company = "Partner Co.",
            purpose = "Security review",
            hostName = "Siti Rahayu",
            status = "checked_in",
            expectedAt = "2026-05-28T10:00:00Z",
            checkedInAt = "2026-05-28T09:55:00Z",
        ),
        GuestVisit(
            id = "guest-003",
            name = "Maria Santos",
            email = "maria@example.com",
            company = "Tenant Ops",
            purpose = "Tenant meeting",
            hostName = "Budi Santoso",
            status = "checked_out",
            expectedAt = "2026-05-27T11:00:00Z",
            checkedInAt = "2026-05-27T10:58:00Z",
            checkedOutAt = "2026-05-27T12:15:00Z",
        ),
    )

    val alarms = listOf(
        Alarm(
            id = "alarm-001",
            type = "Door forced open",
            status = "open",
            severity = "critical",
            location = "Server Room",
            triggeredAt = isoMinutesAgo(12),
        ),
        Alarm(
            id = "alarm-002",
            type = "Gateway offline",
            status = "open",
            severity = "high",
            location = "Parking Gate",
            triggeredAt = isoMinutesAgo(48),
        ),
        Alarm(
            id = "alarm-003",
            type = "Repeated denied access",
            status = "acknowledged",
            severity = "medium",
            location = "Executive Suite",
            triggeredAt = isoMinutesAgo(96),
        ),
    )

    val alarmSchedules = listOf(
        AlarmSchedule(
            id = "alarm-schedule-001",
            name = "After Hours Monitoring",
            enabled = true,
            alarmTypes = listOf("Door forced open", "Gateway offline"),
            startTime = "18:00",
            endTime = "07:00",
            daysOfWeek = listOf(0, 1, 2, 3, 4, 5, 6),
            timezone = "Asia/Jakarta",
        ),
        AlarmSchedule(
            id = "alarm-schedule-002",
            name = "Server Room Watch",
            enabled = true,
            alarmTypes = listOf("Repeated denied access"),
            startTime = "00:00",
            endTime = "23:59",
            daysOfWeek = listOf(1, 2, 3, 4, 5),
            timezone = "Asia/Jakarta",
        ),
    )

    val alarmCalendar = listOf(
        AlarmCalendarEntry(
            id = "alarm-calendar-001",
            date = LocalDate.now().format(dateFormatter),
            alarmCount = alarms.size,
            alarms = alarms,
        ),
        AlarmCalendarEntry(
            id = "alarm-calendar-002",
            date = LocalDate.now().minusDays(1).format(dateFormatter),
            alarmCount = 1,
            alarms = alarms.takeLast(1),
        ),
    )

    val liveActivity = listOf(
        LiveActivityRecord("live-001", "Ahmad Wijaya", "unlock", "Main Entrance", isoMinutesAgo(4)),
        LiveActivityRecord("live-002", "Siti Rahayu", "unlock", "Meeting Room A", isoMinutesAgo(9)),
        LiveActivityRecord("live-003", "Dewi Lestari", "entered", "Lobby", isoMinutesAgo(17)),
    )

    val events = listOf(
        AdminEvent(
            id = "evt-001",
            placeId = "p1",
            eventType = "door",
            objectName = "Main Entrance",
            objectId = "door-001",
            doorId = "door-001",
            actor = "Ahmad Wijaya",
            action = "unlock",
            result = "granted",
            resultColor = "green",
            timestamp = isoMinutesAgo(8),
        ),
        AdminEvent(
            id = "evt-002",
            placeId = "p1",
            eventType = "door",
            objectName = "Parking Gate",
            objectId = "door-003",
            doorId = "door-003",
            actor = "Siti Rahayu",
            action = "unlock",
            result = "granted",
            resultColor = "green",
            timestamp = isoMinutesAgo(62),
        ),
        AdminEvent(
            id = "evt-003",
            placeId = "p1",
            eventType = "door",
            objectName = "Server Room",
            objectId = "door-002",
            doorId = "door-002",
            actor = "Gunawan Tan",
            action = "unlock",
            result = "denied",
            resultColor = "red",
            timestamp = isoMinutesAgo(126),
            detail = "No permission",
        ),
        AdminEvent(
            id = "evt-004",
            placeId = "p1",
            eventType = "door",
            objectName = "Meeting Room A",
            objectId = "door-004",
            doorId = "door-004",
            actor = "Dewi Lestari",
            action = "remote_unlock",
            result = "granted",
            resultColor = "green",
            timestamp = isoMinutesAgo(210),
        ),
    )

    val incidents = listOf(
        AdminIncident(
            id = "inc-001",
            placeId = "p1",
            title = "failed_unlocks",
            state = "open",
            severity = "high",
            status = "investigating",
            subjectType = "door",
            subjectId = "door-002",
            description = "Repeated denied attempts at Server Room.",
            createdAt = "2026-05-07T09:14:00Z",
            count = 4,
            events = listOf(
                IncidentEvent("fe1", "Gunawan Tan", "2026-05-07T09:14:00Z"),
                IncidentEvent("fe4", "Joko Wibowo", "2026-05-06T14:22:00Z"),
            ),
        ),
        AdminIncident(
            id = "inc-002",
            placeId = "p1",
            title = "gateway_offline",
            state = "open",
            severity = "medium",
            status = "open",
            subjectType = "gateway",
            subjectId = "gw-003",
            description = "Parking gateway has reported offline state.",
            createdAt = "2026-05-06T18:30:00Z",
            count = 1,
        ),
    )

    val incidentOccurrences = listOf(
        IncidentOccurrence(
            eventId = "fe1",
            actor = "Gunawan Tan",
            doorId = "Server Room",
            gatewayId = "gw-002",
            detail = "No permission",
            result = "denied",
            occurredAt = "2026-05-07T09:14:00Z",
        ),
        IncidentOccurrence(
            eventId = "fe4",
            actor = "Joko Wibowo",
            doorId = "Server Room",
            gatewayId = "gw-002",
            detail = "No permission",
            result = "denied",
            occurredAt = "2026-05-06T14:22:00Z",
        ),
    )

    val userPresenceRecords = listOf(
        UserPresenceRecord("u1", "Ahmad Wijaya", "ahmad@example.com", 26, 187, "2026-04-08", listOf(32, 28, 30, 27, 31, 8, 5)),
        UserPresenceRecord("u2", "Siti Rahayu", "siti@example.com", 24, 156, "2026-04-08", listOf(28, 25, 22, 30, 26, 4, 2)),
        UserPresenceRecord("u3", "Budi Santoso", "budi@example.com", 22, 134, "2026-04-10", listOf(24, 22, 20, 25, 23, 6, 3)),
        UserPresenceRecord("u4", "Dewi Lestari", "dewi@example.com", 20, 98, "2026-04-08", listOf(18, 16, 20, 15, 19, 2, 0)),
        UserPresenceRecord("u5", "Eko Prasetyo", "eko@example.com", 18, 112, "2026-04-12", listOf(20, 18, 22, 17, 21, 10, 8)),
        UserPresenceRecord("u6", "Fitri Handayani", "fitri@example.com", 16, 78, "2026-04-15", listOf(14, 12, 16, 13, 15, 0, 0)),
        UserPresenceRecord("u7", "Gunawan Tan", "gunawan@example.com", 12, 45, "2026-04-20", listOf(8, 10, 6, 9, 7, 3, 1)),
        UserPresenceRecord("u8", "Hesti Wulandari", "hesti@example.com", 8, 32, "2026-04-22", listOf(6, 5, 7, 4, 6, 0, 0)),
        UserPresenceRecord("u9", "Irfan Maulana", "irfan@example.com", 6, 21, "2026-04-28", listOf(4, 3, 5, 3, 4, 1, 0)),
        UserPresenceRecord("u10", "Joko Wibowo", "joko@example.com", 4, 14, "2026-05-01", listOf(3, 2, 3, 2, 3, 0, 0)),
    )

    fun analyticsSummary(days: Int): AnalyticsSummary {
        val safeDays = days.coerceAtLeast(1)
        val today = LocalDate.now()
        val dailyTrend = (safeDays - 1 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val isWeekend = date.dayOfWeek.value >= 6
            val base = if (isWeekend) 15 else 65
            val unlocks = (base + ((date.dayOfMonth * 7 + daysAgo * 3) % 24) - 8).coerceAtLeast(4)
            val uniqueUsers = (unlocks / 3 + ((date.dayOfMonth + daysAgo) % 5) - 1).coerceAtLeast(3)
            val failed = (date.dayOfMonth + daysAgo) % 5
            DailyTrendPoint(
                id = date.format(dateFormatter),
                date = date.format(dateFormatter),
                unlocks = unlocks,
                uniqueUsers = uniqueUsers,
                failed = failed,
            )
        }
        val totalUnlocks = dailyTrend.sumOf { it.unlocks }
        val failed = dailyTrend.sumOf { it.failed }
        return AnalyticsSummary(
            totalUnlocks = totalUnlocks,
            uniqueUsers = 23,
            failedAttempts = failed,
            avgDailyUnlocks = totalUnlocks.toDouble() / safeDays.toDouble(),
            dailyTrend = dailyTrend,
            unlocksByMethod = listOf(
                MethodCount("mobile", 412),
                MethodCount("card", 298),
                MethodCount("ble", 187),
                MethodCount("pin", 63),
                MethodCount("qr", 34),
                MethodCount("visitor", 12),
            ),
            topDoors = listOf(
                TopDoor("d1", "Main Entrance", 458),
                TopDoor("d2", "Staff Room", 231),
                TopDoor("d3", "Parking Gate", 189),
                TopDoor("d4", "Server Room", 76),
                TopDoor("d5", "Meeting Room A", 52),
            ),
            heatmap = (0..6).flatMap { day ->
                (0..23).map { hour ->
                    val isWorkday = day < 5
                    val isWorkHour = hour in 8..18
                    val value = when {
                        isWorkday && isWorkHour -> 3 + ((day * 5 + hour * 3) % 16)
                        isWorkday -> (day + hour) % 4
                        else -> (day + hour) % 6
                    }
                    HeatmapCell(day, hour, value)
                }
            },
            weeklyUsers = (5 downTo 0).map { weeksAgo ->
                val date = today.minusWeeks(weeksAgo.toLong())
                WeeklyUserPoint(
                    id = date.format(dateFormatter),
                    weekStart = date.format(dateFormatter),
                    uniqueUsers = 12 + ((date.dayOfMonth + weeksAgo * 3) % 13),
                )
            },
        )
    }

    fun failedAttempts(): List<FailedAttemptEvent> = listOf(
        FailedAttemptEvent("fe1", "Gunawan Tan", "Server Room", "mobile", "No permission", "2026-05-07T09:14:00Z"),
        FailedAttemptEvent("fe2", "Hesti Wulandari", "Executive Suite", "mobile", "Access denied", "2026-05-07T08:32:00Z"),
        FailedAttemptEvent("fe3", "Irfan Maulana", "Main Entrance", "card", "Invalid card", "2026-05-06T17:45:00Z"),
        FailedAttemptEvent("fe4", "Joko Wibowo", "Server Room", "pin", "Invalid PIN", "2026-05-06T14:22:00Z"),
        FailedAttemptEvent("fe5", "Fitri Handayani", "Parking Gate", "qr", "Expired pass", "2026-05-05T11:08:00Z"),
        FailedAttemptEvent("fe6", "Dewi Lestari", "Meeting Room A", "mobile", "Schedule restricted", "2026-05-05T09:55:00Z"),
        FailedAttemptEvent("fe7", "Eko Prasetyo", "Executive Suite", "ble", "No permission", "2026-05-04T16:30:00Z"),
    )

    fun event(id: String): AdminEvent? = events.firstOrNull { it.id == id }

    fun relatedEvents(eventId: String): List<RelatedAdminEvent> {
        val current = event(eventId) ?: return emptyList()
        return events.filter { it.id != eventId }.take(3).mapIndexed { index, event ->
            RelatedAdminEvent(
                id = "${event.id}-related",
                placeId = event.placeId,
                eventType = event.eventType,
                objectName = event.objectName,
                objectId = event.objectId,
                doorId = event.doorId,
                areaId = event.areaId,
                gatewayId = event.gatewayId,
                actor = event.actor,
                action = event.action,
                result = event.result,
                resultColor = event.resultColor,
                timestamp = event.timestamp,
                displayTime = event.displayTime,
                relation = if (index == 0 && event.objectId == current.objectId) "same_object" else "nearby_time",
            )
        }
    }

    fun incident(id: String): AdminIncident? = incidents.firstOrNull { it.id == id }

    private fun isoMinutesAgo(minutes: Long): String =
        Instant.now().minus(Duration.ofMinutes(minutes)).toString()
}
