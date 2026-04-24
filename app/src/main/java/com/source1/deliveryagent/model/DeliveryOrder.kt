package com.source1.deliveryagent.model

data class DeliveryOrder(
    val orderId: String = "",
    val customerName: String = "",
    val items: String = "",
    val address: String = "",
    val note: String = "",
    val status: String = "",
    val assignedTo: String = ""
)