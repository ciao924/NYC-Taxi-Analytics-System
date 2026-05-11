package com.taxi.etl.exception

class QualityCheckException(message: String, cause: Throwable = null) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}