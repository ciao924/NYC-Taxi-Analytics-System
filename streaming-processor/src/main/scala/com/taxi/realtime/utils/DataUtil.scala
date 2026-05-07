package com.taxi.realtime.utils

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DataUtil {
  private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  def parseTimestamp(dateStr: String): Option[Timestamp] = {
    try {
      val localDateTime = LocalDateTime.parse(dateStr, DATE_FORMATTER)
      Some(Timestamp.valueOf(localDateTime))
    } catch {
      case e: Exception =>
        LoggerUtil.error(s"Failed to parse timestamp: $dateStr", e)
        None
    }
  }

  def parseDouble(value: Any): Option[Double] = {
    value match {
      case null => None
      case d: Double =>
        if (d.isNaN || d.isInfinite) {
          None
        } else {
          Some(d)
        }
      case n: Number =>
        val d = n.doubleValue
        if (d.isNaN || d.isInfinite) {
          None
        } else {
          Some(d)
        }
      case s: String =>
        try {
          val d = s.toDouble
          if (d.isNaN || d.isInfinite) {
            None
          } else {
            Some(d)
          }
        } catch {
          case e: NumberFormatException =>
            LoggerUtil.warn(s"Failed to parse double: $value")
            None
        }
      case _ => None
    }
  }

  def parseInteger(value: Any): Option[Int] = {
    value match {
      case null => None
      case i: Int => Some(i)
      case n: Number => Some(n.intValue)
      case s: String =>
        try {
          Some(s.toInt)
        } catch {
          case e: NumberFormatException =>
            LoggerUtil.warn(s"Failed to parse integer: $value")
            None
        }
      case _ => None
    }
  }

  def isNull(value: Any): Boolean = {
    value == null
  }

  def isNotNull(value: Any): Boolean = {
    !isNull(value)
  }
}