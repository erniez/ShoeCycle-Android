package com.shoecycle.data.strava

import com.shoecycle.data.strava.models.StravaActivity

interface StravaService {
    sealed class DomainError : Exception() {
        object Unknown : DomainError()
        object Reachability : DomainError()
        object Unauthorized : DomainError()
    }
    
    suspend fun sendActivity(activity: StravaActivity)
}