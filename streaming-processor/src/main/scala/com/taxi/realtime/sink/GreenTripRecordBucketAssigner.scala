package com.taxi.realtime.sink

import com.taxi.realtime.model.GreenTripRecord
import org.apache.flink.core.io.SimpleVersionedSerializer
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner

import java.time.{Instant, LocalDateTime, ZoneId}

class GreenTripRecordBucketAssigner extends BucketAssigner[GreenTripRecord, String] {

  private val zoneId: ZoneId = ZoneId.systemDefault()

  override def getBucketId(element: GreenTripRecord, context: BucketAssigner.Context): String = {
    try {
      element.pickupDatetime match {
        case Some(dt) =>
          val instant = Instant.ofEpochMilli(dt.getTime)
          val localDateTime = LocalDateTime.ofInstant(instant, zoneId)
          f"year=${localDateTime.getYear}%04d/month=${localDateTime.getMonthValue}%02d"
        case None =>
          val now = LocalDateTime.now(zoneId)
          f"year=${now.getYear}%04d/month=${now.getMonthValue}%02d"
      }
    } catch {
      case e: Exception =>
        val now = LocalDateTime.now(zoneId)
        f"year=${now.getYear}%04d/month=${now.getMonthValue}%02d"
    }
  }

  override def getSerializer: SimpleVersionedSerializer[String] = {
    org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer.INSTANCE
  }
}