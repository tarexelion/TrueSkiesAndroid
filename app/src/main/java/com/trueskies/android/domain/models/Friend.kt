package com.trueskies.android.domain.models

/**
 * Friend connection — ported from iOS Friend.swift.
 * Represents a friend connection with status and metadata.
 */
data class Friend(
    val id: String,
    val userId: String,
    val userName: String,
    val email: String? = null,
    val profilePhotoUrl: String? = null,
    val friendsSince: Long = System.currentTimeMillis(),
    val mutualFlights: Int = 0,
    val status: FriendConnectionStatus = FriendConnectionStatus.ACTIVE
) {
    /** Display name with fallback */
    val displayName: String
        get() = userName.ifBlank { email ?: "Unknown" }

    /** Initials for avatar placeholder */
    val initials: String
        get() = userName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }

    val isActive: Boolean get() = status == FriendConnectionStatus.ACTIVE
    val isPending: Boolean get() = status == FriendConnectionStatus.PENDING
    val isBlocked: Boolean get() = status == FriendConnectionStatus.BLOCKED
}

enum class FriendConnectionStatus(val displayName: String) {
    ACTIVE("Active"),
    PENDING("Pending"),
    BLOCKED("Blocked");
}

/**
 * Friend request — ported from iOS FriendRequest.
 */
data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isPending: Boolean get() = status == RequestStatus.PENDING
    val isAccepted: Boolean get() = status == RequestStatus.ACCEPTED

    /** Sender initials for avatar */
    val senderInitials: String
        get() = fromUserName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}

enum class RequestStatus { PENDING, ACCEPTED, DECLINED, EXPIRED }

/**
 * User search result — ported from iOS UserSearchResult.
 */
data class UserSearchResult(
    val id: String,
    val userName: String,
    val email: String? = null,
    val profilePhotoUrl: String? = null,
    val mutualFriends: Int = 0,
    val isFriend: Boolean = false,
    val hasPendingRequest: Boolean = false,
    val isRequestFromThem: Boolean = false
) {
    val displayName: String get() = userName
    val initials: String
        get() = userName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }

    val canSendRequest: Boolean
        get() = !isFriend && !hasPendingRequest
}

/**
 * Friend activity — ported from iOS FriendActivity.
 */
data class FriendActivity(
    val id: String,
    val userId: String,
    val userName: String,
    val type: ActivityType,
    val flightIdent: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayText: String
        get() = when (type) {
            ActivityType.FLIGHT_TRACKED -> "$userName tracked a flight"
            ActivityType.FLIGHT_COMPLETED -> "$userName completed a flight"
            ActivityType.FLIGHT_SHARED -> "$userName shared a flight"
            ActivityType.FRIEND_ADDED -> "$userName connected"
            ActivityType.ACHIEVEMENT_UNLOCKED -> "$userName unlocked an achievement"
        }

    val routeText: String?
        get() = if (origin != null && destination != null) "$origin → $destination" else null
}

enum class ActivityType {
    FLIGHT_TRACKED,
    FLIGHT_COMPLETED,
    FLIGHT_SHARED,
    FRIEND_ADDED,
    ACHIEVEMENT_UNLOCKED
}
