
package com.example.collegelostandfound

data class ContactRequest(
    var reportId: String = "",
    var requesterId: String = "",
    var ownerId: String = "",
    var itemName: String = "",
    var status: String = "Pending"
)
