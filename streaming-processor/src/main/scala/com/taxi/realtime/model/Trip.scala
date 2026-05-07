package com.taxi.realtime.model

case class Trip(
  vendorId: String,
  pickupTime: Long,
  dropoffTime: Long,
  passengerCount: Int,
  tripDistance: Double,
  pickupZone: String,
  dropoffZone: String,
  fareAmount: Double,
  paymentType: String,
  taxiType: String
)