package com.taxi.realtime.utils

import com.taxi.realtime.model.GreenTripRecord

import java.sql.Timestamp
import java.util.concurrent.atomic.AtomicLong

object DataCleaner {
  private val totalRecords = new AtomicLong(0)
  private val filteredRecords = new AtomicLong(0)

  def clean(record: Option[GreenTripRecord]): Option[GreenTripRecord] = {
    totalRecords.incrementAndGet()

    record match {
      case None =>
        filteredRecords.incrementAndGet()
        MetricsCollector.incrementDataQualityFail()
        LoggerUtil.warn("Filtered None record")
        None
      case Some(r) =>
        if (isValidRecord(r)) {
          val currentTotal = totalRecords.get()
          if (currentTotal % 1000 == 0) {
            LoggerUtil.info(s"Cleaner stats: total=$currentTotal, filtered=${filteredRecords.get()}")
          }
          MetricsCollector.incrementDataQualityPass()
          Some(r)
        } else {
          filteredRecords.incrementAndGet()
          MetricsCollector.incrementDataQualityFail()
          LoggerUtil.warn(s"Filtered invalid record: ${r.toString.take(200)}")
          None
        }
    }
  }

  private def isValidRecord(record: GreenTripRecord): Boolean = {
    val fareAmount = record.fareAmount.getOrElse(0.0)
    val tripDistance = record.tripDistance.getOrElse(0.0)
    val passengerCount = record.passengerCount.getOrElse(0)
    val pickupTime = record.pickupDatetime.map(_.getTime).getOrElse(0L)
    val dropoffTime = record.dropoffDatetime.map(_.getTime).getOrElse(0L)

    fareAmount > 0 &&
    tripDistance > 0 && tripDistance < 100 &&
    passengerCount >= 1 && passengerCount <= 6 &&
    dropoffTime >= pickupTime &&
    (record.puLocationId.isEmpty || record.puLocationId.get != 0)
  }

  def getTotalRecords: Long = totalRecords.get()
  def getFilteredRecords: Long = filteredRecords.get()
  def getFilterRate: Double = {
    val total = totalRecords.get()
    if (total == 0) 0.0
    else (filteredRecords.get().toDouble / total) * 100
  }
}