package com.shoecycle.domain.analytics

/**
 * Centralized constants for analytics events and parameters.
 * Mirrors the iOS AnalyticsKeys implementation for cross-platform consistency.
 */
object AnalyticsKeys {
    
    /**
     * Event names for analytics tracking.
     */
    object Event {
        const val LOG_MILEAGE = "log_mileage"
        const val STRAVA_EVENT = "log_mileage_strava"
        const val HEALTH_CONNECT_EVENT = "log_mileage_health_connect"
        const val ADD_SHOE = "add_shoe"
        const val SHOE_PICTURE_ADDED = "add_shoe_picture"
        const val SHOW_HISTORY = "show_history"
        const val SHOW_FAVORITE_DISTANCES = "show_favorite_distances"
        const val ADD_TO_HOF = "add_to_HOF"
        const val REMOVE_FROM_HOF = "remove_from_HOF"
        const val VIEW_SHOE_DETAIL = "view_shoe_detail"
        const val DID_EDIT_SHOE = "did_edit_shoe"
        const val EMAIL_SHOE_TAPPED = "email_shoe_tapped"
        const val DID_EMAIL_SHOE = "did_email_shoe"
    }
    
    /**
     * Parameter keys for analytics event data.
     */
    object Param {
        const val MILEAGE = "mileage"
        const val TOTAL_MILEAGE = "total_mileage"
        const val NUMBER_OF_FAVORITES = "number_of_favorites"
        const val DISTANCE_UNIT = "distance_unit"
        const val SHOE_BRAND = "shoe_brand"
        const val START_MILEAGE = "start_mileage"
    }
}