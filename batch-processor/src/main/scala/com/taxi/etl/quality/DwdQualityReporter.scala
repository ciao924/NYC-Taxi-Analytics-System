package com.taxi.etl.quality

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DwdQualityReporter {

  private val logger = LoggerFactory.getLogger(getClass)
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def generateDailyReport(spark: SparkSession, date: LocalDate): QualityReport = {
    logger.info(s"Starting DWD layer quality report generation for date: $date")

    val report = QualityReport(
      reportDate = date,
      reportTimestamp = new Timestamp(System.currentTimeMillis()),
      totalRecords = 0L,
      validRecords = 0L,
      invalidRecords = 0L,
      nullCountMap = Map.empty,
      ruleResults = List.empty,
      overallScore = 0.0,
      alertLevel = "NORMAL"
    )

    try {
      val dwdTrips = readDwdTrips(spark, date)
      val nullCheckResult = checkNullColumns(dwdTrips)
      val valueRangeResult = checkValueRanges(dwdTrips)
      val consistencyResult = checkConsistency(dwdTrips)

      val updatedReport = report.copy(
        totalRecords = dwdTrips.count(),
        validRecords = dwdTrips.filter(validTripFilter).count(),
        invalidRecords = dwdTrips.filter(invalidTripFilter).count(),
        nullCountMap = nullCheckResult,
        ruleResults = List(
          RuleResult("NULL_CHECK", nullCheckResult.values.sum == 0, nullCheckResult.values.sum, "通过"),
          RuleResult("VALUE_RANGE", valueRangeResult, 0, if (valueRangeResult) "通过" else "失败"),
          RuleResult("CONSISTENCY", consistencyResult, 0, if (consistencyResult) "通过" else "失败")
        ),
        overallScore = calculateOverallScore(nullCheckResult, valueRangeResult, consistencyResult),
        alertLevel = determineAlertLevel(nullCheckResult, valueRangeResult, consistencyResult)
      )

      logger.info(s"DWD quality report generated successfully. Score: ${updatedReport.overallScore}")
      updatedReport

    } catch {
      case e: Exception =>
        logger.error(s"Failed to generate DWD quality report for date: $date", e)
        report.copy(
          alertLevel = "ERROR",
          ruleResults = List(RuleResult("GENERATION", false, 0, e.getMessage))
        )
    }
  }

  private def readDwdTrips(spark: SparkSession, date: LocalDate): DataFrame = {
    val partitionPath = s"dt=${date.format(dateFormatter)}"
    spark.sql(s"SELECT * FROM dwd_taxi_trips WHERE $partitionPath")
  }

  private def checkNullColumns(df: DataFrame): Map[String, Long] = {
    val columnsToCheck = List(
      "vendor_id", "pickup_datetime", "dropoff_datetime",
      "passenger_count", "trip_distance", "fare_amount"
    )

    columnsToCheck.map { colName =>
      import org.apache.spark.sql.functions._
      val nullCount = df.filter(col(colName).isNull || col(colName).isNaN).count()
      colName -> nullCount
    }.toMap
  }

  private def checkValueRanges(df: DataFrame): Boolean = {
    import org.apache.spark.sql.functions._
    val fareCheck = df.filter(col("fare_amount") < 0 || col("fare_amount") > 1000).count() == 0
    val distanceCheck = df.filter(col("trip_distance") < 0 || col("trip_distance") > 500).count() == 0
    val passengerCheck = df.filter(col("passenger_count") < 1 || col("passenger_count") > 9).count() == 0

    fareCheck && distanceCheck && passengerCheck
  }

  private def checkConsistency(df: DataFrame): Boolean = {
    import org.apache.spark.sql.functions._
    val pickupBeforeDropoff = df.filter(col("pickup_datetime") >= col("dropoff_datetime")).count() == 0
    val distanceAndFareConsistent = df.filter(
      col("trip_distance") > 0 && col("fare_amount") < col("trip_distance") * 0.1
    ).count() == 0

    pickupBeforeDropoff && distanceAndFareConsistent
  }

  private def calculateOverallScore(
    nullCheck: Map[String, Long],
    valueRange: Boolean,
    consistency: Boolean
  ): Double = {
    val nullPenalty = if (nullCheck.values.sum > 0) 10.0 else 0.0
    val rangePenalty = if (!valueRange) 5.0 else 0.0
    val consistencyPenalty = if (!consistency) 5.0 else 0.0
    100.0 - nullPenalty - rangePenalty - consistencyPenalty
  }

  private def determineAlertLevel(
    nullCheck: Map[String, Long],
    valueRange: Boolean,
    consistency: Boolean
  ): String = {
    val score = calculateOverallScore(nullCheck, valueRange, consistency)
    if (score < 80) "CRITICAL"
    else if (score < 90) "WARNING"
    else "NORMAL"
  }

  private def validTripFilter: String = "vendor_id IS NOT NULL AND pickup_datetime IS NOT NULL AND fare_amount >= 0"

  private def invalidTripFilter: String = "vendor_id IS NULL OR pickup_datetime IS NULL OR fare_amount < 0"
}

case class QualityReport(
  reportDate: LocalDate,
  reportTimestamp: Timestamp,
  totalRecords: Long,
  validRecords: Long,
  invalidRecords: Long,
  nullCountMap: Map[String, Long],
  ruleResults: List[RuleResult],
  overallScore: Double,
  alertLevel: String
)

case class RuleResult(
  ruleName: String,
  passed: Boolean,
  failedCount: Long,
  message: String
)