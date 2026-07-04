package com.elsfm.mobile.core.network.auth

sealed interface SessionEvent {
    data object Expired : SessionEvent
}
