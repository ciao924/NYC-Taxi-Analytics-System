package com.taxi.realtime.model

case class HotspotResult(
  windowStart: Long,
  windowEnd: Long,
  zone: String,
  cnt: Long,
  rank: Int
)