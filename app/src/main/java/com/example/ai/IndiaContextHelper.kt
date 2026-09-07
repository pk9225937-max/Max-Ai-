package com.example.ai

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object IndiaContextHelper {

    private val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun getCurrentIndianDateTimeFormatted(): String {
        val now = Date()
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy, hh:mm a 'IST'", Locale("en", "IN")).apply {
            timeZone = istTimeZone
        }
        return dateFormat.format(now)
    }

    fun getCurrentIndianTimeOnly(): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "IN")).apply {
            timeZone = istTimeZone
        }
        return timeFormat.format(now)
    }

    fun getCurrentIndianDateOnly(): String {
        val now = Date()
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("en", "IN")).apply {
            timeZone = istTimeZone
        }
        return dateFormat.format(now)
    }

    fun getIndianFestivalsOverview(): String {
        val cal = Calendar.getInstance(istTimeZone)
        val month = cal.get(Calendar.MONTH) + 1 // 1-based
        val day = cal.get(Calendar.DAY_OF_MONTH)

        return """
            Current Indian Context:
            - Standard Time: ${getCurrentIndianDateTimeFormatted()}
            - Location/Context: India (IST timezone, UTC+5:30)
            - Popular Indian Festivals & Occasions:
              * Diwali / Deepavali, Dhanteras, Chhath Puja, Bhai Dooj (Major lights festival, prosperity, family celebration)
              * Holi & Holika Dahan (Festival of colors, gujiya, celebration of spring)
              * Navratri, Durga Puja, Dussehra / Vijayadashami (9 days of Garba/Dandiya, victory of good over evil)
              * Raksha Bandhan (Brother-sister bond, rakhi celebration)
              * Janmashtami & Ganesh Chaturthi (Lord Krishna and Ganesha celebrations)
              * Eid-ul-Fitr, Eid-ul-Adha, Ramadan, Muharram
              * National Days: Republic Day (26 Jan), Independence Day (15 Aug), Gandhi Jayanti (2 Oct)
              * Karwa Chauth (Fasting for spouse's long life), Maha Shivratri, Makar Sankranti/Pongal/Lohri, Baisakhi, Guru Nanak Jayanti, Christmas.
            - Cultural Familiarity:
              * Uses natural Indian conversational tone: "Haanji", "Arre", "Suno", "Boss", "Babu", "Namaste", "Chai time".
              * Familiar with Indian foods (Biryani, Chai, Samosa, Golgappe, Paneer, Sweets like Kaju Katli & Jalebi).
              * Understands Indian daily life, traffic, weather (Monsoon/Garmi/Sardi), cricket, Bollywood, memes, and family relations.
        """.trimIndent()
    }
}
