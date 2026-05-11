package taxi

import org.apache.spark.sql.{DataFrame, SaveMode}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, DoubleType}
import taxi.DataUtil._

object DataQualityCheck {

  def main(args: Array[String]): Unit = {

    println("=" * 70)
    println("ODS层数据质量检测")
    println("=" * 70)
    println(s"ETL日期: $etl_date\n")

    // 读取ODS数据
    val odsDF = spark.table("nyc_taxi_ods.yellow_taxi_trips_ods")
      .filter(col("etl_date") === etl_date)

    // 添加计算字段（trip_duration_minutes）
    val dfWithDuration = odsDF
      .withColumn("tpep_pickup_ts", to_timestamp(col("tpep_pickup_datetime"), "yyyy-MM-dd HH:mm:ss"))
      .withColumn("tpep_dropoff_ts", to_timestamp(col("tpep_dropoff_datetime"), "yyyy-MM-dd HH:mm:ss"))
      .withColumn("trip_duration_minutes",
        (col("tpep_dropoff_ts").cast("long") - col("tpep_pickup_ts").cast("long")) / 60)

    val totalCount = dfWithDuration.count()
    println(s"总记录数: $totalCount")

    // 1. 字段空值检测
    println("\n" + "=" * 70)
    println("1. 字段空值检测")
    println("=" * 70)
    checkNullValues(dfWithDuration, totalCount)

    // 2. 数据类型检测
    println("\n" + "=" * 70)
    println("2. 数据类型检测")
    println("=" * 70)
    checkDataTypes(dfWithDuration)

    // 3. 值域范围检测
    println("\n" + "=" * 70)
    println("3. 值域范围检测")
    println("=" * 70)
    checkValueRanges(dfWithDuration, totalCount)

    // 4. 业务逻辑检测
    println("\n" + "=" * 70)
    println("4. 业务逻辑检测")
    println("=" * 70)
    checkBusinessLogic(dfWithDuration, totalCount)

    // 5. 重复数据检测
    println("\n" + "=" * 70)
    println("5. 重复数据检测")
    println("=" * 70)
    checkDuplicates(dfWithDuration)

    // 6. 数据分布检测
    println("\n" + "=" * 70)
    println("6. 数据分布检测")
    println("=" * 70)
    checkDataDistribution(dfWithDuration, totalCount)

    // 7. 生成检测报告
    println("\n" + "=" * 70)
    println("7. 生成检测报告")
    println("=" * 70)
    generateQualityReport(dfWithDuration, totalCount)

    spark.stop()
  }

  def checkNullValues(df: DataFrame, totalCount: Long): Unit = {
    val nullStats = df.columns.map { colName =>
      val nullCount = df.filter(col(colName).isNull).count()
      val nullRate = nullCount.toDouble / totalCount * 100
      (colName, nullCount, nullRate)
    }.filter(_._2 > 0).sortBy(-_._2)

    if (nullStats.isEmpty) {
      println("✅ 无空值字段")
    } else {
      println("⚠️ 存在空值的字段:")
      println(f"${"字段名"}%-30s ${"空值数量"}%-12s ${"空值率"}%-10s")
      println("-" * 55)
      nullStats.foreach { case (colName, count, rate) =>
        println(f"$colName%-30s $count%-12d $rate%-10.2f%%")
      }
    }
  }

  def checkDataTypes(df: DataFrame): Unit = {
    println("字段数据类型:")
    println(f"${"字段名"}%-30s ${"当前类型"}%-20s ${"建议类型"}%-20s")
    println("-" * 70)

    df.dtypes.foreach { case (colName, dataType) =>
      val suggestedType = dataType match {
        case "StringType" if colName.contains("ID") && colName != "trip_id" => "IntType"
        case "StringType" if colName.contains("amount") => "DecimalType"
        case "StringType" if colName.contains("datetime") => "TimestampType"
        case "DoubleType" if colName.contains("ID") && colName != "trip_id" => "IntType"
        case "StringType" if colName == "store_and_fwd_flag" => "StringType"
        case _ => dataType
      }
      println(f"$colName%-30s $dataType%-20s $suggestedType%-20s")
    }
  }

  def checkValueRanges(df: DataFrame, totalCount: Long): Unit = {
    // 行程距离检测
    val distanceStats = df.agg(
      min("trip_distance").as("min"),
      max("trip_distance").as("max"),
      avg("trip_distance").as("avg")
    ).collect()(0)

    val invalidDistance = df.filter(col("trip_distance") <= 0 || col("trip_distance") > 100).count()
    println(s"行程距离检测:")
    println(s"   - 最小值: ${distanceStats.getDouble(0)}")
    println(s"   - 最大值: ${distanceStats.getDouble(1)}")
    println(s"   - 平均值: ${distanceStats.getDouble(2)}")
    println(s"   - 异常记录(≤0或>100): $invalidDistance 条 (${invalidDistance.toDouble/totalCount*100}%)")

    // 总费用检测
    val amountStats = df.agg(
      min("total_amount").as("min"),
      max("total_amount").as("max"),
      avg("total_amount").as("avg")
    ).collect()(0)

    val invalidAmount = df.filter(col("total_amount") <= 0 || col("total_amount") > 500).count()
    println(s"\n总费用检测:")
    println(s"   - 最小值: ${amountStats.getDouble(0)}")
    println(s"   - 最大值: ${amountStats.getDouble(1)}")
    println(s"   - 平均值: ${amountStats.getDouble(2)}")
    println(s"   - 异常记录(≤0或>500): $invalidAmount 条 (${invalidAmount.toDouble/totalCount*100}%)")

    // 乘客数量检测
    val passengerStats = df.agg(
      min("passenger_count").as("min"),
      max("passenger_count").as("max"),
      avg("passenger_count").as("avg")
    ).collect()(0)

    val invalidPassenger = df.filter(col("passenger_count") <= 0 || col("passenger_count") > 6).count()
    println(s"\n乘客数量检测:")
    println(s"   - 最小值: ${passengerStats.getDouble(0)}")
    println(s"   - 最大值: ${passengerStats.getDouble(1)}")
    println(s"   - 平均值: ${passengerStats.getDouble(2)}")
    println(s"   - 异常记录(≤0或>6): $invalidPassenger 条 (${invalidPassenger.toDouble/totalCount*100}%)")

    // 时长检测（使用计算字段）
    val durationStats = df.agg(
      min("trip_duration_minutes").as("min"),
      max("trip_duration_minutes").as("max"),
      avg("trip_duration_minutes").as("avg")
    ).collect()(0)

    val invalidDuration = df.filter(col("trip_duration_minutes") <= 1 || col("trip_duration_minutes") > 180).count()
    println(s"\n行程时长检测:")
    println(s"   - 最小值: ${durationStats.getDouble(0)}")
    println(s"   - 最大值: ${durationStats.getDouble(1)}")
    println(s"   - 平均值: ${durationStats.getDouble(2)}")
    println(s"   - 异常记录(≤1或>180): $invalidDuration 条 (${invalidDuration.toDouble/totalCount*100}%)")
  }

  def checkBusinessLogic(df: DataFrame, totalCount: Long): Unit = {
    // 时间逻辑检测
    val invalidTime = df.filter(col("tpep_dropoff_ts") <= col("tpep_pickup_ts")).count()
    println(s"时间逻辑检测:")
    println(s"   - 下车时间 <= 上车时间: $invalidTime 条 (${invalidTime.toDouble/totalCount*100}%)")

    // 机场费逻辑检测
    val airportZones = Seq(132, 138) // JFK和LGA
    val invalidAirportFee = df.filter(
      (col("Airport_fee") > 0 && !col("PULocationID").isin(airportZones: _*)) ||
        (col("Airport_fee") === 0 && col("PULocationID").isin(airportZones: _*) && col("RatecodeID").isin(2, 3))
    ).count()
    println(s"\n机场费逻辑检测:")
    println(s"   - 机场费异常: $invalidAirportFee 条 (${invalidAirportFee.toDouble/totalCount*100}%)")

    // 小费逻辑检测
    val invalidTip = df.filter(col("tip_amount") > 0 && col("payment_type") === 2).count()
    println(s"\n小费逻辑检测:")
    println(s"   - 现金支付但有小费: $invalidTip 条 (${invalidTip.toDouble/totalCount*100}%)")

    // 空车费检测
    val invalidFare = df.filter(col("fare_amount") === 0 && col("trip_distance") > 0).count()
    println(s"\n空车费检测:")
    println(s"   - 有距离但车费为0: $invalidFare 条 (${invalidFare.toDouble/totalCount*100}%)")
  }

  def checkDuplicates(df: DataFrame): Unit = {
    // 使用多字段组合检测重复
    val duplicateCount = df.groupBy("VendorID", "tpep_pickup_datetime", "tpep_dropoff_datetime", "PULocationID")
      .count()
      .filter("count > 1")
      .count()

    println(s"重复数据检测:")
    if (duplicateCount == 0) {
      println("   ✅ 无重复记录")
    } else {
      println(s"   ⚠️ 发现 $duplicateCount 组重复记录")
    }
  }

  def checkDataDistribution(df: DataFrame, totalCount: Long): Unit = {
    println("供应商分布:")
    df.groupBy("VendorID").count()
      .withColumnRenamed("count", "trip_count")
      .withColumn("percentage", col("trip_count") / totalCount * 100)
      .orderBy(desc("trip_count"))
      .show(false)

    println("支付方式分布:")
    df.groupBy("payment_type").count()
      .withColumnRenamed("count", "trip_count")
      .withColumn("percentage", col("trip_count") / totalCount * 100)
      .orderBy("payment_type")
      .show(false)

    println("费率代码分布:")
    df.groupBy("RatecodeID").count()
      .withColumnRenamed("count", "trip_count")
      .withColumn("percentage", col("trip_count") / totalCount * 100)
      .orderBy(desc("trip_count"))
      .show(false)

    println("上车区域分布（Top 10）:")
    df.groupBy("PULocationID").count()
      .withColumnRenamed("count", "trip_count")
      .withColumn("percentage", col("trip_count") / totalCount * 100)
      .orderBy(desc("trip_count"))
      .show(10, false)
  }

  def generateQualityReport(df: DataFrame, totalCount: Long): Unit = {
    import spark.implicits._

    // 计算各项质量指标
    val nullFields = df.columns.map(c => df.filter(col(c).isNull).count()).filter(_ > 0).length
    val abnormalDistance = df.filter(col("trip_distance") <= 0 || col("trip_distance") > 100).count()
    val abnormalAmount = df.filter(col("total_amount") <= 0 || col("total_amount") > 500).count()
    val abnormalPassenger = df.filter(col("passenger_count") <= 0 || col("passenger_count") > 6).count()
    val timeError = df.filter(col("tpep_dropoff_ts") <= col("tpep_pickup_ts")).count()

    val qualityMetrics = Seq(
      ("总记录数", totalCount.toString, "数据量"),
      ("空值字段数", nullFields.toString, "完整性"),
      ("异常距离记录", abnormalDistance.toString, "准确性"),
      ("异常费用记录", abnormalAmount.toString, "准确性"),
      ("异常乘客数记录", abnormalPassenger.toString, "准确性"),
      ("时间逻辑错误", timeError.toString, "一致性"),
      ("重复记录组数", df.groupBy("VendorID", "tpep_pickup_datetime", "tpep_dropoff_datetime", "PULocationID").count().filter("count > 1").count().toString, "唯一性")
    ).toDF("指标", "数值", "维度")

    qualityMetrics.show(false)

    // 计算综合质量分数
    val qualityScore = 100 - (
      nullFields.toDouble / df.columns.length * 15 +
        abnormalDistance.toDouble / totalCount * 15 +
        abnormalAmount.toDouble / totalCount * 15 +
        abnormalPassenger.toDouble / totalCount * 10 +
        timeError.toDouble / totalCount * 10
      )

    println(s"\n综合质量分数: ${qualityScore.formatted("%.2f")} 分")
    if (qualityScore >= 95) {
      println("评级: 优秀 ✅")
    } else if (qualityScore >= 80) {
      println("评级: 良好 ⚠️")
    } else {
      println("评级: 需改进 ❌")
    }

    // 生成清洗建议
    println("\n清洗建议:")
    if (nullFields > 0) {
      println("   - 建议删除空值记录或使用维度表填充")
    }
    if (abnormalDistance > 0) {
      println("   - 建议过滤异常距离记录（>100英里或≤0）")
    }
    if (abnormalAmount > 0) {
      println("   - 建议过滤异常费用记录（>500美元或≤0）")
    }
    if (abnormalPassenger > 0) {
      println("   - 建议过滤异常乘客数记录（>6人或≤0）")
    }
    if (timeError > 0) {
      println("   - 建议修复时间逻辑错误记录")
    }
    if (invalidAirportFeeCount(df) > 0) {
      println("   - 建议修复机场费逻辑异常")
    }
    if (invalidTipCount(df) > 0) {
      println("   - 建议检查现金支付但有小费的记录")
    }
  }

  def invalidAirportFeeCount(df: DataFrame): Long = {
    val airportZones = Seq(132, 138)
    df.filter(
      (col("Airport_fee") > 0 && !col("PULocationID").isin(airportZones: _*)) ||
        (col("Airport_fee") === 0 && col("PULocationID").isin(airportZones: _*) && col("RatecodeID").isin(2, 3))
    ).count()
  }

  def invalidTipCount(df: DataFrame): Long = {
    df.filter(col("tip_amount") > 0 && col("payment_type") === 2).count()
  }
}