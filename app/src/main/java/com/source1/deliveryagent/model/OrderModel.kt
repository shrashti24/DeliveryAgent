
package com.source1.deliveryagent.model

import java.io.Serializable

data class OrderModel(
    var itemPushKey: String? = "",
    var userUid: String? = "",
    var userName: String? = "",
    var address: String? = "",
    var phoneNumber: String? = "",
    var totalPrice: String? = "",
    var status: String? = "pending",   // pending, assigned, picked, on_way, delivered
    var assignedTo: String? = "",
    var orderAccepted: Boolean = false,
    var paymentReceived: Boolean = false,
    var currentTime: Long = 0,
    var foodNames: ArrayList<String>? = null,
    var foodImages: ArrayList<String>? = null,
    var foodPrices: ArrayList<String>? = null,
    var foodQuantities: ArrayList<Int>? = null,
    var deliveryOTP: String? = null,
    var restaurantLat: Double? = 0.0,
    var restaurantLng: Double? = 0.0,
    var customerLat: Double? = 0.0,
    var customerLng: Double? = 0.0,
    var restaurantName: String? = "",
    var estimatedTime: String? = "",
    var distance: String? = ""
) : Serializable