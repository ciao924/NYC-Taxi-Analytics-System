package com.taxi.etl.models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.slf4j.MDC

case class JobContext(
                       executionId: String,
                       jobName: String,
                       startTime: Long,
                       startDate: String,
                       endDate: String,
                       environment: String = "dev"
                     ) {
  def setMDC(): Unit = {
    MDC.put("executionId", executionId)
    MDC.put("jobName", jobName)
    MDC.put("environment", environment)
  }
  def clearMDC(): Unit = {
    MDC.remove("executionId")
    MDC.remove("jobName")
    MDC.remove("environment")
  }
}

object JobContext {
  private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

  def create(jobName: String, startDate: String, endDate: String, environment: String = "dev"): JobContext = {
    val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER)
    val uuid = UUID.randomUUID().toString.take(8)
    JobContext(s"${jobName}_${timestamp}_$uuid", jobName, System.currentTimeMillis(), startDate, endDate, environment)
  }

  def fromArgs(jobName: String, args: Array[String], environment: String = "dev"): JobContext = {
    val startDate = if (args.length > 0) args(0) else "2025-01-01"
    val endDate = if (args.length > 1) args(1) else "2025-03-31"
    create(jobName, startDate, endDate, environment)
  }
}