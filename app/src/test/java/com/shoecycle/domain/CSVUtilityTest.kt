package com.shoecycle.domain

import com.shoecycle.domain.models.History
import com.shoecycle.domain.models.Shoe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CSVUtilityTest {
    
    private val csvUtility = CSVUtility()
    private val dateFormatter = SimpleDateFormat("MM-dd-yyyy", Locale.US)
    
    @Test
    fun `createCSVData with empty history list returns header only`() {
        val shoe = Shoe.createDefault(brand = "Test Shoe", maxDistance = 500.0)
        val histories = emptyList<History>()
        val csvData = csvUtility.createCSVData(shoe, histories)

        assertEquals("Run Date, Shoe, Distance\n", csvData)
    }
    
    @Test
    fun `createCSVData with single history entry formats correctly`() {
        val shoe = Shoe.createDefault(brand = "Nike Air", maxDistance = 500.0)
        val date = dateFormatter.parse("12-25-2023")!!
        val history = History(
            id = 1L,
            shoeId = shoe.id,
            runDate = date,
            runDistance = 5.25
        )

        val csvData = csvUtility.createCSVData(shoe, listOf(history))

        val expected = "Run Date, Shoe, Distance\n12-25-2023, Nike Air, 5.25\n"
        assertEquals(expected, csvData)
    }
    
    @Test
    fun `createCSVData with multiple histories sorts by date descending`() {
        val shoe = Shoe.createDefault(brand = "Adidas", maxDistance = 400.0)
        val date1 = dateFormatter.parse("01-01-2023")!!
        val date2 = dateFormatter.parse("06-15-2023")!!
        val date3 = dateFormatter.parse("12-31-2023")!!

        val histories = listOf(
            History(1L, shoe.id, date1, 3.0),
            History(2L, shoe.id, date3, 10.5),
            History(3L, shoe.id, date2, 5.75)
        )

        val csvData = csvUtility.createCSVData(shoe, histories)

        val lines = csvData.lines()
        assertEquals("Run Date, Shoe, Distance", lines[0])
        assertTrue(lines[1].startsWith("12-31-2023"))
        assertTrue(lines[1].contains("Adidas"))
        assertTrue(lines[2].startsWith("06-15-2023"))
        assertTrue(lines[2].contains("Adidas"))
        assertTrue(lines[3].startsWith("01-01-2023"))
        assertTrue(lines[3].contains("Adidas"))
    }
    
    @Test
    fun `createCSVData formats distances with two decimal places`() {
        val shoe = Shoe.createDefault(brand = "Brooks", maxDistance = 450.0)
        val date = Date()
        val histories = listOf(
            History(1L, shoe.id, date, 5.0),
            History(2L, shoe.id, date, 3.333333),
            History(3L, shoe.id, date, 10.999)
        )

        val csvData = csvUtility.createCSVData(shoe, histories)

        assertTrue(csvData.contains("5.00"))
        assertTrue(csvData.contains("3.33"))
        assertTrue(csvData.contains("11.00"))
        assertTrue(csvData.contains("Brooks"))
    }
    
    @Test
    fun `createCSVData with shoe includes shoe name in output`() {
        val shoe = Shoe.createDefault(brand = "Nike", maxDistance = 500.0)
        val date = dateFormatter.parse("03-15-2024")!!
        val histories = listOf(
            History(1L, shoe.id, date, 5.25)
        )

        val csvData = csvUtility.createCSVData(shoe, histories)

        val expected = "Run Date, Shoe, Distance\n03-15-2024, Nike, 5.25\n"
        assertEquals(expected, csvData)
    }
    
    @Test
    fun `createCSVData with multiple shoes always includes shoe column`() {
        val shoe1 = Shoe.createDefault(brand = "Nike", maxDistance = 500.0)
        val shoe2 = Shoe.createDefault(brand = "Adidas", maxDistance = 400.0)
        val date = dateFormatter.parse("01-01-2024")!!

        val shoesWithHistories = listOf(
            Pair(shoe1, listOf(History(1L, shoe1.id, date, 5.0))),
            Pair(shoe2, listOf(History(2L, shoe2.id, date, 3.0)))
        )

        val csvData = csvUtility.createCSVData(shoesWithHistories)

        assertTrue(csvData.startsWith("Run Date, Shoe, Distance\n"))
        assertTrue(csvData.contains("Nike"))
        assertTrue(csvData.contains("Adidas"))
    }
    
    @Test
    fun `createCSVData with multiple shoes sorts all entries by date`() {
        val shoe1 = Shoe.createDefault(brand = "Nike", maxDistance = 500.0)
        val shoe2 = Shoe.createDefault(brand = "Adidas", maxDistance = 400.0)
        val date1 = dateFormatter.parse("01-01-2024")!!
        val date2 = dateFormatter.parse("02-01-2024")!!
        val date3 = dateFormatter.parse("03-01-2024")!!

        val shoesWithHistories = listOf(
            Pair(shoe1, listOf(
                History(1L, shoe1.id, date1, 5.0),
                History(3L, shoe1.id, date3, 7.0)
            )),
            Pair(shoe2, listOf(
                History(2L, shoe2.id, date2, 3.0)
            ))
        )

        val csvData = csvUtility.createCSVData(shoesWithHistories)
        val lines = csvData.lines().filter { it.isNotEmpty() }

        assertEquals("Run Date, Shoe, Distance", lines[0])
        assertTrue(lines[1].startsWith("03-01-2024") && lines[1].contains("Nike"))
        assertTrue(lines[2].startsWith("02-01-2024") && lines[2].contains("Adidas"))
        assertTrue(lines[3].startsWith("01-01-2024") && lines[3].contains("Nike"))
    }
    
    @Test
    fun `generateFileName with shoe creates proper filename`() {
        val shoe = Shoe.createDefault(brand = "Nike Air Max", maxDistance = 500.0)
        val fileName = csvUtility.generateFileName(shoe)
        
        assertEquals("ShoeCycleShoeData-Nike_Air_Max.csv", fileName)
    }
    
    @Test
    fun `generateFileName with empty brand uses Unknown`() {
        val shoe = Shoe.createDefault(brand = "", maxDistance = 500.0)
        val fileName = csvUtility.generateFileName(shoe)
        
        assertEquals("ShoeCycleShoeData-Unknown.csv", fileName)
    }
    
    @Test
    fun `generateFileName without shoe returns generic name`() {
        val fileName = csvUtility.generateFileName(null)
        
        assertEquals("ShoeCycleShoeData.csv", fileName)
    }
    
    @Test
    fun `generateFileName sanitizes special characters`() {
        val shoe = Shoe.createDefault(brand = "Nike / Adidas & More", maxDistance = 500.0)
        val fileName = csvUtility.generateFileName(shoe)
        
        assertEquals("ShoeCycleShoeData-Nike___Adidas_&_More.csv", fileName)
    }
    
    private fun assertFalse(condition: Boolean) {
        assertTrue(!condition)
    }
}