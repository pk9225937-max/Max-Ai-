package com.example.data.repository

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.example.domain.model.ContactMatch

class ContactRepository(private val context: Context) {

    fun searchContacts(query: String): List<ContactMatch> {
        val matches = mutableListOf<ContactMatch>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenNumbers = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) else "Unknown"
                    val rawNumber = if (numberIndex != -1) it.getString(numberIndex) else ""
                    val normalizedNumber = rawNumber.replace("[^0-9+]".toRegex(), "")

                    if (normalizedNumber.isNotBlank() && !seenNumbers.contains(normalizedNumber)) {
                        seenNumbers.add(normalizedNumber)
                        matches.add(
                            ContactMatch(
                                id = id,
                                displayName = name,
                                phoneNumber = rawNumber
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission missing
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
        return matches
    }
}
