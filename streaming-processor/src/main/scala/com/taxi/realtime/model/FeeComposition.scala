package com.taxi.realtime.model

case class FeeComposition(
  windowStart: Long,
  windowEnd: Long,
  paymentType: String,
  totalAmount: Double
)