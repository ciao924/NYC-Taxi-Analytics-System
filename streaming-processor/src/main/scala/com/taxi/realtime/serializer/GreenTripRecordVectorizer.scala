package com.taxi.realtime.serializer

import com.taxi.realtime.model.GreenTripRecord
import org.apache.flink.orc.vector.Vectorizer
import org.apache.hadoop.hive.ql.exec.vector.{BytesColumnVector, DoubleColumnVector, LongColumnVector, TimestampColumnVector, VectorizedRowBatch}
import org.apache.orc.TypeDescription

class GreenTripRecordVectorizer extends Vectorizer[GreenTripRecord](
  "struct<vendor_id:int,lpep_pickup_datetime:timestamp,lpep_dropoff_datetime:timestamp,store_and_fwd_flag:string,ratecode_id:int,pu_location_id:int,do_location_id:int,passenger_count:bigint,trip_distance:double,fare_amount:double,extra:double,mta_tax:double,tip_amount:double,tolls_amount:double,ehail_fee:double,improvement_surcharge:double,total_amount:double,payment_type:bigint,trip_type:bigint,congestion_surcharge:double,cbd_congestion_fee:double>"
) {

  override def vectorize(record: GreenTripRecord, batch: VectorizedRowBatch): Unit = {
    val row = batch.size
    batch.ensureSize(row + 1)

    // vendor_id (INT) - 第0列
    if (record.vendorId.isDefined) {
      batch.cols(0).asInstanceOf[LongColumnVector].vector(row) = record.vendorId.get.toLong
    } else {
      batch.cols(0).asInstanceOf[LongColumnVector].noNulls = false
      batch.cols(0).asInstanceOf[LongColumnVector].isNull(row) = true
    }

    // lpep_pickup_datetime (TIMESTAMP) - 第1列
    if (record.pickupDatetime.isDefined) {
      val ts: java.sql.Timestamp = record.pickupDatetime.get
      batch.cols(1).asInstanceOf[TimestampColumnVector].set(row, ts)
    } else {
      batch.cols(1).asInstanceOf[TimestampColumnVector].noNulls = false
      batch.cols(1).asInstanceOf[TimestampColumnVector].isNull(row) = true
    }

    // lpep_dropoff_datetime (TIMESTAMP) - 第2列
    if (record.dropoffDatetime.isDefined) {
      val ts: java.sql.Timestamp = record.dropoffDatetime.get
      batch.cols(2).asInstanceOf[TimestampColumnVector].set(row, ts)
    } else {
      batch.cols(2).asInstanceOf[TimestampColumnVector].noNulls = false
      batch.cols(2).asInstanceOf[TimestampColumnVector].isNull(row) = true
    }

    // store_and_fwd_flag (STRING) - 第3列
    val flagBytes = "N".getBytes("UTF-8")
    batch.cols(3).asInstanceOf[BytesColumnVector].setRef(row, flagBytes, 0, flagBytes.length)

    // ratecode_id (INT) - 第4列
    if (record.puLocationId.isDefined) {
      batch.cols(4).asInstanceOf[LongColumnVector].vector(row) = record.puLocationId.get.toLong
    } else {
      batch.cols(4).asInstanceOf[LongColumnVector].noNulls = false
      batch.cols(4).asInstanceOf[LongColumnVector].isNull(row) = true
    }

    // pu_location_id (INT) - 第5列
    if (record.puLocationId.isDefined) {
      batch.cols(5).asInstanceOf[LongColumnVector].vector(row) = record.puLocationId.get.toLong
    } else {
      batch.cols(5).asInstanceOf[LongColumnVector].noNulls = false
      batch.cols(5).asInstanceOf[LongColumnVector].isNull(row) = true
    }

    // do_location_id (INT) - 第6列
    if (record.doLocationId.isDefined) {
      batch.cols(6).asInstanceOf[LongColumnVector].vector(row) = record.doLocationId.get.toLong
    } else {
      batch.cols(6).asInstanceOf[LongColumnVector].noNulls = false
      batch.cols(6).asInstanceOf[LongColumnVector].isNull(row) = true
    }

    // passenger_count (BIGINT) - 第7列
    if (record.passengerCount.isDefined) {
      batch.cols(7).asInstanceOf[LongColumnVector].vector(row) = record.passengerCount.get.toLong
    } else {
      batch.cols(7).asInstanceOf[LongColumnVector].noNulls = false
      batch.cols(7).asInstanceOf[LongColumnVector].isNull(row) = true
    }

    // trip_distance (DOUBLE) - 第8列
    if (record.tripDistance.isDefined) {
      batch.cols(8).asInstanceOf[DoubleColumnVector].vector(row) = record.tripDistance.get
    } else {
      batch.cols(8).asInstanceOf[DoubleColumnVector].noNulls = false
      batch.cols(8).asInstanceOf[DoubleColumnVector].isNull(row) = true
    }

    // fare_amount (DOUBLE) - 第9列
    if (record.fareAmount.isDefined) {
      batch.cols(9).asInstanceOf[DoubleColumnVector].vector(row) = record.fareAmount.get
    } else {
      batch.cols(9).asInstanceOf[DoubleColumnVector].noNulls = false
      batch.cols(9).asInstanceOf[DoubleColumnVector].isNull(row) = true
    }

    // extra (DOUBLE) - 第10列
    batch.cols(10).asInstanceOf[DoubleColumnVector].noNulls = false
    batch.cols(10).asInstanceOf[DoubleColumnVector].isNull(row) = true

    // mta_tax (DOUBLE) - 第11列
    batch.cols(11).asInstanceOf[DoubleColumnVector].noNulls = false
    batch.cols(11).asInstanceOf[DoubleColumnVector].isNull(row) = true

    // tip_amount (DOUBLE) - 第12列
    if (record.tipAmount.isDefined) {
      batch.cols(12).asInstanceOf[DoubleColumnVector].vector(row) = record.tipAmount.get
    } else {
      batch.cols(12).asInstanceOf[DoubleColumnVector].noNulls = false
      batch.cols(12).asInstanceOf[DoubleColumnVector].isNull(row) = true
    }

    // tolls_amount (DOUBLE) - 第13列
    batch.cols(13).asInstanceOf[DoubleColumnVector].noNulls = false
    batch.cols(13).asInstanceOf[DoubleColumnVector].isNull(row) = true

    // ehail_fee (DOUBLE) - 第14列
    batch.cols(14).asInstanceOf[DoubleColumnVector].noNulls = false
    batch.cols(14).asInstanceOf[DoubleColumnVector].isNull(row) = true

    // improvement_surcharge (DOUBLE) - 第15列
    batch.cols(15).asInstanceOf[DoubleColumnVector].noNulls = false
    batch.cols(15).asInstanceOf[DoubleColumnVector].isNull(row) = true

    // total_amount (DOUBLE) - 第16列
    if (record.totalAmount.isDefined) {
      batch.cols(16).asInstanceOf[DoubleColumnVector].vector(row) = record.totalAmount.get
    } else {
      batch.cols(16).asInstanceOf[DoubleColumnVector].noNulls = false
      batch.cols(16).asInstanceOf[DoubleColumnVector].isNull(row) = true
    }

    // payment_type (BIGINT) - 第17列
    batch.cols(17).asInstanceOf[LongColumnVector].noNulls = false
    batch.cols(17).asInstanceOf[LongColumnVector].isNull(row) = true

    // trip_type (BIGINT) - 第18列
    batch.cols(18).asInstanceOf[LongColumnVector].noNulls = false
    batch.cols(18).asInstanceOf[LongColumnVector].isNull(row) = true

    // congestion_surcharge (DOUBLE) - 第19列
    batch.cols(19).asInstanceOf[DoubleColumnVector].noNulls = false
    batch.cols(19).asInstanceOf[DoubleColumnVector].isNull(row) = true

    // cbd_congestion_fee (DOUBLE) - 第20列
    batch.cols(20).asInstanceOf[DoubleColumnVector].noNulls = false
    batch.cols(20).asInstanceOf[DoubleColumnVector].isNull(row) = true

    batch.size += 1
  }
}