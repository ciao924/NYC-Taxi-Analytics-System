package com.taxi.etl.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 版本信息
 *
 * v4-lite-fixed: 生产稳跑版（已修复所有已知问题）
 * - 线程安全修复
 * - Iceberg 写入逻辑修正
 * - 质量检测性能优化
 * - 配置管理完善
 */
object Version {

  /** 版本号 */
  val VERSION = "v4-lite-fixed"

  /** 构建日期 */
  val BUILD_DATE = "2026-01-15"

  /** 构建时间戳 */
  val BUILD_TIMESTAMP = 1736928000000L

  /** Git 提交哈希（构建时注入） */
  val GIT_COMMIT = sys.env.getOrElse("GIT_COMMIT", "unknown")

  /** 构建号（CI/CD 注入） */
  val BUILD_NUMBER = sys.env.getOrElse("BUILD_NUMBER", "dev")

  /**
   * 打印版本信息
   */
  def printVersion(): Unit = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val buildTime = LocalDateTime.ofInstant(
      java.time.Instant.ofEpochMilli(BUILD_TIMESTAMP),
      java.time.ZoneId.systemDefault()
    )

    println(
      s"""
         |========================================
         |  出租车大数据分析系统
         |  版本: $VERSION
         |  构建日期: $BUILD_DATE
         |  构建时间: ${buildTime.format(formatter)}
         |  Git Commit: $GIT_COMMIT
         |  构建号: $BUILD_NUMBER
         |========================================
      """.stripMargin)
  }

  /**
   * 获取版本字符串
   */
  def getVersionString: String = {
    s"$VERSION (build $BUILD_NUMBER, commit ${GIT_COMMIT.take(7)})"
  }

  /**
   * 检查版本兼容性
   * 用于运行时验证
   */
  def isCompatible(minVersion: String): Boolean = {
    // 简单版本比较，可根据需要扩展
    VERSION >= minVersion
  }
}