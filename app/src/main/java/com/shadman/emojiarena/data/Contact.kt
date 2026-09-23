package com.shadman.emojiarena.data

/** A dummy contact. No real accounts, no backend — just a fixed cast. */
data class Contact(
    val id: String,
    val name: String
)

val sampleContacts = listOf(
    Contact(id = "1", name = "Ayesha Rahman"),
    Contact(id = "2", name = "Tanvir Chowdhury"),
    Contact(id = "3", name = "Farhan Kabir"),
    Contact(id = "4", name = "Nusrat Jahan"),
    Contact(id = "5", name = "Rafiul Islam"),
    Contact(id = "6", name = "Meherun Nesa"),
    Contact(id = "7", name = "Sabbir Ahmed"),
    Contact(id = "8", name = "Priya Das")
)
