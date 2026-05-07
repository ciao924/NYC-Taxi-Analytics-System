package com.taxi.realtime.model

import java.io.Serializable

/**
 * 死信队列记录类
 * @param originalData 原始消息数据
 * @param errorReason 错误原因
 */
case class ErrorRecordJava(
                            originalData: String = "",
                            errorReason: String = ""
                          ) extends Serializable {

  def getOriginalData: String = if (originalData != null) originalData else ""

  def getErrorReason: String = if (errorReason != null) errorReason else ""

  def setOriginalData(data: String): ErrorRecordJava = {
    this.copy(originalData = if (data != null) data else "")
  }

  def setErrorReason(reason: String): ErrorRecordJava = {
    this.copy(errorReason = if (reason != null) reason else "")
  }

  override def toString: String = {
    s"ErrorRecordJava(originalData='$originalData', errorReason='$errorReason')"
  }
}

// 伴生对象，提供工厂方法
object ErrorRecordJava {
  def apply(originalData: String, errorReason: String): ErrorRecordJava = {
    new ErrorRecordJava(
      originalData = if (originalData != null) originalData else "",
      errorReason = if (errorReason != null) errorReason else ""
    )
  }
}