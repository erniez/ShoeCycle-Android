package com.shoecycle.ui.screens.add_distance.utils

import java.util.Date

/**
 * iOS-compatible weekly collated data model
 * Matches the structure from ShoeCycle iOS WeeklyCollated.swift
 * 
 * This class aggregates run distances for a week starting from a specific date.
 * The runDistance property is mutable to allow accumulation of multiple runs
 * within the same week during the collation process.
 */
data class WeeklyCollatedNew(
    val date: Date,
    var runDistance: Double
) {
    /**
     * Unique identifier for this weekly data point
     * Uses the week's start date as the ID
     */
    val id: Date
        get() = date
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WeeklyCollatedNew) return false
        
        return date == other.date && runDistance == other.runDistance
    }
    
    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + runDistance.hashCode()
        return result
    }
}