package com.shoecycle.domain

/**
 * Manages First Time User hints for progressive feature discovery.
 * Ported from iOS FTUHintManager to maintain feature parity.
 */
class FTUHintManager {
    
    enum class HintKey(val key: String, val message: String) {
        SWIPE_FEATURE(
            "ShoeCycleFTUSwipeFeature",
            "You can swipe between shoes just by swiping up or down on the shoe image in the \"Add Distance\" screen."
        ),
        STRAVA_FEATURE(
            "ShoeCycleFTUStravaFeature", 
            "You can integrate with Strava! Add your runs to Strava as easily as tapping the \"+\" button. Just tap on the \"Settings\" tab to get started!"
        ),
        EMAIL_HISTORY_FEATURE(
            "ShoeCycleFTUEmailHistoryFeature",
            "You can export your run history as a CSV file via email! Just tap \"Email Data\" at the top left of the Run History screen."
        ),
        HOF_FEATURE(
            "ShoeCycleFTUHOFFeature",
            "You can add shoes to the Hall of Fame section, so they don't crowd your active sneakers."
        ),
        GRAPH_ALL_SHOES_FEATURE(
            "ShoeCycleFTUGraphAllShoesFeature",
            "You can tap the button at the bottom right of the graph to toggle between showing data for all active shoes or just the currently selected shoe."
        ),
        YEARLY_HISTORY_FEATURE(
            "ShoeCycleFTUYearlyHistoryFeature",
            "You can see yearly distances in the History view. The shoes tracked will match what the graph tracks."
        );
        
        companion object {
            fun fromKey(key: String): HintKey? {
                return values().find { it.key == key }
            }
        }
    }
    
    companion object {
        const val COMPLETED_HINTS_KEY = "ShoeCycleFTUCompletedFeatures"
        
        /**
         * Order in which hints should be shown to users
         */
        val hintOrder = listOf(
            HintKey.SWIPE_FEATURE,
            HintKey.STRAVA_FEATURE,
            HintKey.HOF_FEATURE,
            HintKey.GRAPH_ALL_SHOES_FEATURE,
            HintKey.EMAIL_HISTORY_FEATURE,
            HintKey.YEARLY_HISTORY_FEATURE
        )
    }
    
    /**
     * Gets the next available hint that hasn't been completed
     */
    fun getNextHint(completedHints: Set<String>): HintKey? {
        return hintOrder.firstOrNull { hint ->
            !completedHints.contains(hint.key)
        }
    }
    
    /**
     * Gets the message for a specific hint
     */
    fun getHintMessage(hintKey: HintKey): String {
        return hintKey.message
    }
    
    /**
     * Checks if all hints have been completed
     */
    fun allHintsCompleted(completedHints: Set<String>): Boolean {
        return hintOrder.all { hint ->
            completedHints.contains(hint.key)
        }
    }
    
    /**
     * Checks if a specific hint has been completed
     */
    fun isHintCompleted(hintKey: HintKey, completedHints: Set<String>): Boolean {
        return completedHints.contains(hintKey.key)
    }
}