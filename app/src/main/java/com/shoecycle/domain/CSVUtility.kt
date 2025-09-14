package com.shoecycle.domain

import com.shoecycle.domain.models.History
import com.shoecycle.domain.models.Shoe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CSVUtility {
    
    companion object {
        private const val DATE_FORMAT = "MM-dd-yyyy"
        private const val CSV_HEADER = "Run Date, Shoe, Distance\n"
        private const val DECIMAL_FORMAT = "%.2f"
    }
    
    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US)
    
    /**
     * Creates CSV data string from shoes and their histories
     * Always includes shoe name in the output
     * @param shoesWithHistories List of pairs of shoes and their histories
     * @return CSV formatted string with format: "Run Date, Shoe, Distance"
     */
    fun createCSVData(shoesWithHistories: List<Pair<Shoe, List<History>>>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(CSV_HEADER)

        // Collect all histories with their shoe names and sort by date (most recent first)
        val allHistoriesWithShoes = mutableListOf<Triple<Date, String, Double>>()

        shoesWithHistories.forEach { (shoe, histories) ->
            val shoeName = shoe.displayName
            histories.forEach { history ->
                allHistoriesWithShoes.add(Triple(history.runDate, shoeName, history.runDistance))
            }
        }

        // Sort by date descending (most recent first)
        allHistoriesWithShoes.sortByDescending { it.first }

        // Add each entry as a row
        allHistoriesWithShoes.forEach { (date, shoeName, distance) ->
            val dateString = dateFormatter.format(date)
            val distanceString = String.format(Locale.US, DECIMAL_FORMAT, distance)
            stringBuilder.append("$dateString, $shoeName, $distanceString\n")
        }

        return stringBuilder.toString()
    }

    /**
     * Convenience method for exporting a single shoe's history
     * @param shoe The shoe being exported
     * @param histories The list of history entries for this shoe
     * @return CSV formatted string
     */
    fun createCSVData(shoe: Shoe, histories: List<History>): String {
        return createCSVData(listOf(shoe to histories))
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