package com.shoecycle.domain

import com.shoecycle.domain.models.History
import com.shoecycle.domain.models.Shoe
import java.text.SimpleDateFormat
import java.util.Locale

class CSVUtility {
    
    companion object {
        private const val DATE_FORMAT = "MM-dd-yyyy"
        private const val CSV_HEADER = "Run Date, Distance\n"
        private const val DECIMAL_FORMAT = "%.2f"
    }
    
    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US)
    
    /**
     * Creates CSV data string from a list of histories
     * Format matches iOS implementation:
     * - Header: "Run Date, Distance"
     * - Rows: "MM-dd-yyyy, X.XX"
     * @param histories The list of history entries to export
     * @return CSV formatted string
     */
    fun createCSVData(histories: List<History>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(CSV_HEADER)
        
        // Sort histories by date (most recent first)
        val sortedHistories = histories.sortedByDescending { it.runDate }
        
        // Add each history entry as a row
        sortedHistories.forEach { history ->
            val dateString = dateFormatter.format(history.runDate)
            val distanceString = String.format(Locale.US, DECIMAL_FORMAT, history.runDistance)
            stringBuilder.append("$dateString, $distanceString\n")
        }
        
        return stringBuilder.toString()
    }

    /**
     * Creates CSV data string from a shoe's history
     * @param shoe The shoe being exported (unused but kept for API compatibility)
     * @param histories The list of history entries for this shoe
     * @return CSV formatted string
     */
    @Suppress("UNUSED_PARAMETER")
    fun createCSVData(shoe: Shoe, histories: List<History>): String {
        return createCSVData(histories)
    }

    /**
     * Creates CSV data string from multiple shoes' histories
     * Useful for exporting all shoes data
     * @param shoesWithHistories List of pairs of shoes and their histories
     * @param includeShoeColumn Whether to include shoe name as additional column
     * @return CSV formatted string
     */
    fun createCSVData(shoesWithHistories: List<Pair<Shoe, List<History>>>, includeShoeColumn: Boolean = false): String {
        val stringBuilder = StringBuilder()
        
        if (includeShoeColumn) {
            stringBuilder.append("Shoe, Run Date, Distance\n")
            
            shoesWithHistories.forEach { (shoe, histories) ->
                val shoeName = shoe.displayName
                val sortedHistories = histories.sortedByDescending { it.runDate }
                
                sortedHistories.forEach { history ->
                    val dateString = dateFormatter.format(history.runDate)
                    val distanceString = String.format(Locale.US, DECIMAL_FORMAT, history.runDistance)
                    stringBuilder.append("$shoeName, $dateString, $distanceString\n")
                }
            }
        } else {
            // Combine all histories and sort by date
            val allHistories = shoesWithHistories.flatMap { it.second }
                .sortedByDescending { it.runDate }
            
            stringBuilder.append(CSV_HEADER)
            allHistories.forEach { history ->
                val dateString = dateFormatter.format(history.runDate)
                val distanceString = String.format(Locale.US, DECIMAL_FORMAT, history.runDistance)
                stringBuilder.append("$dateString, $distanceString\n")
            }
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * Generates a filename for the CSV export
     * @param shoe The shoe being exported (optional)
     * @return Filename string
     */
    fun generateFileName(shoe: Shoe? = null): String {
        return if (shoe != null) {
            val brand = shoe.brand.ifEmpty { "Unknown" }
                .replace(" ", "_")
                .replace("/", "_")
            "ShoeCycleShoeData-$brand.csv"
        } else {
            "ShoeCycleShoeData.csv"
        }
    }
}