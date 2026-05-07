package com.taxi.realtime.model

import java.sql.Timestamp

case class GreenTripRecord(
    id: Option[Long] = None,
    vendorId: Option[Int] = None,
    pickupDatetime: Option[Timestamp] = None,
    dropoffDatetime: Option[Timestamp] = None,
    passengerCount: Option[Int] = None,
    tripDistance: Option[Double] = None,
    puLocationId: Option[Int] = None,
    doLocationId: Option[Int] = None,
    fareAmount: Option[Double] = None,
    tipAmount: Option[Double] = None,
    totalAmount: Option[Double] = None,
    processTime: Option[Timestamp] = None,
    taxiType: Option[String] = None,
    // 额外字段，用于lambda包
    paymentType: Option[Int] = None,
    tollsAmount: Option[Double] = None,
    extra: Option[Double] = None,
    improvementSurcharge: Option[Double] = None,
    // 兼容字段
    pickupLocationId: Option[Int] = None,
    dropoffLocationId: Option[Int] = None
) {
  override def toString: String = {
    s"GreenTripRecord(id=$id, vendorId=$vendorId, pickupDatetime=$pickupDatetime, " +
      s"dropoffDatetime=$dropoffDatetime, passengerCount=$passengerCount, tripDistance=$tripDistance, " +
      s"puLocationId=$puLocationId, doLocationId=$doLocationId, fareAmount=$fareAmount, " +
      s"tipAmount=$tipAmount, totalAmount=$totalAmount, processTime=$processTime, taxiType=$taxiType, " +
      s"paymentType=$paymentType, tollsAmount=$tollsAmount, extra=$extra, improvementSurcharge=$improvementSurcharge, " +
      s"pickupLocationId=$pickupLocationId, dropoffLocationId=$dropoffLocationId)"
  }
}
