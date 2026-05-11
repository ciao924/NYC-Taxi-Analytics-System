package taxi.analysis

import taxi.DataUtil._

object CheckAnalysisSchema {

  def main(args: Array[String]): Unit = {

    println("=" * 70)
    println("MySQL分析表结构检查")
    println("=" * 70)

    // 需要检查的表列表
    val tables = List(
      "analysis_kpi_daily",
      "analysis_hourly_distribution",
      "analysis_weekday_distribution",
      "analysis_weekend_comparison",
      "analysis_hourly_amount",
      "analysis_payment_distribution",
      "analysis_payment_amount",
      "analysis_tip_distribution",
      "analysis_tip_distance",
      "analysis_pickup_hotspots",
      "analysis_dropoff_hotspots",
      "analysis_borough_flow",
      "analysis_fee_composition",
      "analysis_fee_percentage",
      "analysis_distance_distribution",
      "analysis_passenger_distribution",
      "analysis_distance_amount",
      "analysis_duration_distribution",
      "analysis_revenue_contribution",
      "analysis_hour_zone_cross",
      "analysis_vendor",
      "analysis_ratecode",
      "analysis_airport"
    )

    tables.foreach { table =>
      try {
        println(s"\n📋 表名: $table")
        println("-" * 50)

        // 读取前5条数据查看结构
        val df = loadMysql(s"SELECT * FROM $table LIMIT 5")

        // 打印表结构
        println("字段结构:")
        df.printSchema()

        // 打印数据样例
        println("数据样例:")
        df.show(3, false)

        // 统计记录数
        val count = loadMysql(s"SELECT COUNT(*) as cnt FROM $table").collect()(0).getLong(0)
        println(s"总记录数: $count")

      } catch {
        case e: Exception => println(s"   ⚠️ 表不存在或查询失败: ${e.getMessage}")
      }
    }

    println("\n" + "=" * 70)
    println("✅ 检查完成")
    println("=" * 70)

    spark.stop()
  }
}