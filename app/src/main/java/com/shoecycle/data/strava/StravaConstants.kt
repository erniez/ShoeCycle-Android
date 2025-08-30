package com.shoecycle.data.strava

object StravaURLs {
    const val OAUTH_URL = "https://www.strava.com/oauth/mobile/authorize?client_id=4002&redirect_uri=ShoeCycle%3A%2F%2Fshoecycleapp.com/callback%2F&response_type=code&approval_prompt=auto&scope=activity%3Awrite%2Cread&state=test"
    const val OAUTH_REFRESH_URL = "https://www.strava.com/oauth/token"
    const val ACTIVITIES_URL = "https://www.strava.com/api/v3/activities"
}

object StravaKeys {
    const val CLIENT_ID_VALUE = "4002"
    const val CLIENT_ID_KEY = "client_id"
    const val SECRET_KEY = "client_secret"
}