package com.taxi.realtime.utils

import org.slf4j.LoggerFactory

object LoggerUtil {
  private val LOGGER = LoggerFactory.getLogger(getClass)

  def debug(message: String): Unit = {
    LOGGER.debug(message)
  }

  def info(message: String): Unit = {
    LOGGER.info(message)
  }

  def info(message: String, arg: Any): Unit = {
    LOGGER.info(message, arg)
  }

  def warn(message: String): Unit = {
    LOGGER.warn(message)
  }

  def warn(message: String, arg: Any): Unit = {
    LOGGER.warn(message, arg)
  }

  def error(message: String): Unit = {
    LOGGER.error(message)
  }

  def error(message: String, t: Throwable): Unit = {
    LOGGER.error(message, t)
  }

  def error(message: String, arg: Any): Unit = {
    LOGGER.error(message, arg)
  }
}