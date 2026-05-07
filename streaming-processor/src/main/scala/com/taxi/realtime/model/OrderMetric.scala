package com.taxi.realtime.model

case class OrderMetric(
  windowStart: Long,
  windowEnd: Long,
  city: String,
  orderCount: Long,
  totalFare: Double,
  avgFare: Double
)