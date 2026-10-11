package com.example.attenbot

import com.example.attenbot.data.parser.CsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvParserTest {

    private val parser = CsvParser()

    @Test
    fun parseValidCsvLines() {
        val csvData = """
            101,Rahul Sharma
            102,Priya Patel
            103,Amit Verma
        """.trimIndent()

        val result = parser.parse(csvData)

        assertEquals(3, result.size)
        assertEquals("101", result[0].rollNumber)
        assertEquals("Rahul Sharma", result[0].studentName)
        assertEquals("102", result[1].rollNumber)
        assertEquals("Priya Patel", result[1].studentName)
        assertEquals("103", result[2].rollNumber)
        assertEquals("Amit Verma", result[2].studentName)
    }

    @Test
    fun parseCsvWithHeaderAndEmptyLines() {
        val csvData = """
            roll_number,student_name
            
            101,Rahul Sharma
            
            102,Priya Patel
            
        """.trimIndent()

        val result = parser.parse(csvData)

        assertEquals(2, result.size)
        assertEquals("101", result[0].rollNumber)
        assertEquals("102", result[1].rollNumber)
    }

    @Test
    fun parseInvalidLines() {
        val csvData = """
            invalid line without comma
            ,Only Name
            101,
            102,Valid Student
        """.trimIndent()

        val result = parser.parse(csvData)

        assertEquals(1, result.size)
        assertEquals("102", result[0].rollNumber)
        assertEquals("Valid Student", result[0].studentName)
    }

    @Test
    fun parseEmptyCsv() {
        val result = parser.parse("")
        assertTrue(result.isEmpty())
    }
}
