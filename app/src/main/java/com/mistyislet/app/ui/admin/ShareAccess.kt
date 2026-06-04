package com.mistyislet.app.ui.admin

import com.mistyislet.app.domain.model.AccessRight

/**
 * Door IDs to include when a resident-admin shares a user's access: every door the
 * user can currently access. Filters out doors they can't access and blank/duplicate
 * IDs so the resulting `door_ids` payload is always valid for the backend.
 */
internal fun List<AccessRight>.shareableDoorIds(): List<String> =
    filter { it.canAccess }
        .map { it.doorId }
        .filter { it.isNotBlank() }
        .distinct()
