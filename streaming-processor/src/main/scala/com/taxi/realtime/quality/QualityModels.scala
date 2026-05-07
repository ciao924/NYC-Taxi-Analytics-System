package com.taxi.realtime.quality

import java.sql.Timestamp

case class QualityMetric(
    checkDate: java.sql.Date,
    tableName: String,
    checkType: String,
    checkStatus: String,
    expectedValue: Option[BigDecimal],
    actualValue: Option[BigDecimal],
    deviationRate: Option[BigDecimal],
    detailJson: Option[String]
)

case class QualityWindowResult(
    windowStart: Long,
    windowEnd: Long,
    totalRecords: Long,
    parseErrors: Long,
    cleanErrors: Long,
    timestampErrors: Long
) {
  def validRecords: Long = totalRecords - parseErrors - cleanErrors - timestampErrors
  def parseErrorRate: BigDecimal = if (totalRecords == 0) BigDecimal(0) else BigDecimal(parseErrors) / BigDecimal(totalRecords) * 100
  def cleanErrorRate: BigDecimal = if (totalRecords == 0) BigDecimal(0) else BigDecimal(cleanErrors) / BigDecimal(totalRecords) * 100
  def timestampErrorRate: BigDecimal = if (totalRecords == 0) BigDecimal(0) else BigDecimal(timestampErrors) / BigDecimal(totalRecords) * 100
  def overallQualityRate: BigDecimal = if (totalRecords == 0) BigDecimal(100) else BigDecimal(validRecords) / BigDecimal(totalRecords) * 100
}

case class AlertConfig(
    id: Long,
    alertName: String,
    checkType: String,
    tableName: Option[String],
    thresholdType: String,
    warningThreshold: BigDecimal,
    criticalThreshold: BigDecimal,
    enabled: Boolean,
    webhookUrl: Option[String],
    emailRecipients: Option[String]
)

case class AlertRecord(
    alertConfigId: Long,
    alertLevel: String,
    alertContent: String,
    checkDate: java.sql.Date,
    tableName: String,
    checkType: String,
    actualValue: BigDecimal,
    thresholdValue: BigDecimal,
    isResolved: Boolean = false,
    resolveTime: Option[Timestamp] = None,
    createTime: Timestamp = new Timestamp(System.currentTimeMillis())
)

sealed trait QualityEvent
case class NormalEvent() extends QualityEvent
case class ParseErrorEvent() extends QualityEvent
case class CleanErrorEvent() extends QualityEvent
case class TimestampErrorEvent() extends QualityEvent