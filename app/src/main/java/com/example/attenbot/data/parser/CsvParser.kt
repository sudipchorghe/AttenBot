package com.example.attenbot.data.parser

data class ParsedStudent(
    val rollNumber: String,
    val studentName: String
)

class CsvParser {
    fun parse(csvContent: String): List<ParsedStudent> {
        if (csvContent.isBlank()) return emptyList()

        val results = mutableListOf<ParsedStudent>()
        val lines = csvContent.lines()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val parts = trimmedLine.split(",")
            if (parts.size >= 2) {
                val rollNumber = parts[0].trim()
                val studentName = parts[1].trim()

                if (rollNumber.equals("roll_number", ignoreCase = true) ||
                    rollNumber.equals("roll", ignoreCase = true) ||
                    rollNumber.equals("rollno", ignoreCase = true) ||
                    rollNumber.equals("roll number", ignoreCase = true)) {
                    continue
                }

                if (rollNumber.isNotEmpty() && studentName.isNotEmpty()) {
                    results.add(ParsedStudent(rollNumber = rollNumber, studentName = studentName))
                }
            }
        }

        return results
    }
}
