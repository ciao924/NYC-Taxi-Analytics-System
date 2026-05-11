package com.taxi.etl.quality

import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.time.LocalDate

object QualityScoreCalculator {

  private val logger = LoggerFactory.getLogger(getClass)
  private val CRITICAL_THRESHOLD = 80.0

  def calculateDailyScore(date: LocalDate): QualityScoreResult = {
    logger.info(s"Calculating quality score for date: $date")

    try {
      val recordCheckResults = getRecordCheckResults(date)
      val ruleResults = getRuleResults(date)
      val nullCheckResults = getNullCheckResults(date)

      val recordScore = calculateRecordScore(recordCheckResults)
      val ruleScore = calculateRuleScore(ruleResults)
      val nullScore = calculateNullScore(nullCheckResults)

      val overallScore = (recordScore * 0.4 + ruleScore * 0.3 + nullScore * 0.3)

      val result = QualityScoreResult(
        scoreDate = date,
        recordCheckScore = recordScore,
        ruleCheckScore = ruleScore,
        nullCheckScore = nullScore,
        overallScore = overallScore,
        passed = overallScore >= CRITICAL_THRESHOLD,
        alertLevel = determineAlertLevel(overallScore),
        details = Map(
          "recordCheckResults" -> recordCheckResults,
          "ruleResults" -> ruleResults,
          "nullCheckResults" -> nullCheckResults
        ),
        calculatedAt = new Timestamp(System.currentTimeMillis())
      )

      saveScoreResult(result)
      logger.info(s"Quality score calculated: overall=$overallScore, alertLevel=${result.alertLevel}")

      result

    } catch {
      case e: Exception =>
        logger.error(s"Failed to calculate quality score for date: $date", e)
        QualityScoreResult(
          scoreDate = date,
          recordCheckScore = 0.0,
          ruleCheckScore = 0.0,
          nullCheckScore = 0.0,
          overallScore = 0.0,
          passed = false,
          alertLevel = "ERROR",
          details = Map("error" -> e.getMessage),
          calculatedAt = new Timestamp(System.currentTimeMillis())
        )
    }
  }

  private def getRecordCheckResults(date: LocalDate): List[Map[String, Any]] = {
    try {
      List.empty
    } catch {
      case e: Exception =>
        logger.warn(s"Failed to get record check results: ${e.getMessage}")
        List.empty
    }
  }

  private def getRuleResults(date: LocalDate): List[Map[String, Any]] = {
    try {
      List.empty
    } catch {
      case e: Exception =>
        logger.warn(s"Failed to get rule results: ${e.getMessage}")
        List.empty
    }
  }

  private def getNullCheckResults(date: LocalDate): Map[String, Long] = {
    try {
      Map.empty
    } catch {
      case e: Exception =>
        logger.warn(s"Failed to get null check results: ${e.getMessage}")
        Map.empty
    }
  }

  private def calculateRecordScore(results: List[Map[String, Any]]): Double = {
    if (results.isEmpty) return 100.0
    100.0
  }

  private def calculateRuleScore(results: List[Map[String, Any]]): Double = {
    if (results.isEmpty) return 100.0
    100.0
  }

  private def calculateNullScore(nullCounts: Map[String, Long]): Double = {
    if (nullCounts.isEmpty) return 100.0

    val totalNulls = nullCounts.values.sum
    val maxAcceptableNulls = 1000

    if (totalNulls <= maxAcceptableNulls) {
      100.0 - (totalNulls.toDouble / maxAcceptableNulls * 10)
    } else {
      Math.max(0, 90.0 - (totalNulls - maxAcceptableNulls).toDouble / 100)
    }
  }

  private def determineAlertLevel(score: Double): String = {
    if (score >= 95) "NORMAL"
    else if (score >= CRITICAL_THRESHOLD) "WARNING"
    else "CRITICAL"
  }

  private def saveScoreResult(result: QualityScoreResult): Unit = {
    try {
      if (!result.passed) {
        sendAlert(result)
      }
    } catch {
      case e: Exception =>
        logger.error(s"Failed to save quality score result: ${e.getMessage}", e)
    }
  }

  private def sendAlert(result: QualityScoreResult): Unit = {
    logger.warn(s"Quality score below threshold! Score: ${result.overallScore}, Alert: ${result.alertLevel}")
  }
}

case class QualityScoreResult(
  scoreDate: LocalDate,
  recordCheckScore: Double,
  ruleCheckScore: Double,
  nullCheckScore: Double,
  overallScore: Double,
  passed: Boolean,
  alertLevel: String,
  details: Map[String, Any],
  calculatedAt: Timestamp
)