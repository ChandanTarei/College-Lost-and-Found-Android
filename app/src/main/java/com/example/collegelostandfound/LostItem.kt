
package com.example.collegelostandfound

data class LostItem(
    var userId: String = "",
    var itemName: String = "",
    var category: String = "",
    var type: String = "",
    var description: String = "",
    var location: String = "",
    var date: String = "",
    var reportId: String = "",
    var status: String = "Pending",
    var contactNumber: String = "",
    var photoPath: String = ""
)
