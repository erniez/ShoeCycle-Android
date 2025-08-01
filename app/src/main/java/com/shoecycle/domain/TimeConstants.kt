package com.shoecycle.domain

/**
 * Time constants matching iOS TimeInterval extensions
 */
object TimeConstants {
    const val SECONDS_IN_DAY: Long = 24 * 60 * 60
    const val SECONDS_IN_WEEK: Long = SECONDS_IN_DAY * 7
    const val SECONDS_IN_SIX_MONTHS: Long = (SECONDS_IN_DAY * 30.42 * 6).toLong()
    
    const val MILLIS_IN_DAY: Long = SECONDS_IN_DAY * 1000
    const val MILLIS_IN_WEEK: Long = SECONDS_IN_WEEK * 1000
    const val MILLIS_IN_SIX_MONTHS: Long = SECONDS_IN_SIX_MONTHS * 1000
}