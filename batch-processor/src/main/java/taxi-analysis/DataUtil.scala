package taxi

import org.apache.spark.sql.catalyst.analysis

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions

import java.time.LocalDate
import java.time.format.DateTimeFormatter


object DataUtil {
  System.setProperty("HADOOP_USER_NAME", "root")
  // 初始化SparkSession
  val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .config("spark.sql.parquet.writeLegacyFormat", "true")
    .config("spark.sql.legacy.parquet.int96RebaseInWrite", "LEGACY")
    .appName("fet-bigdata")
    .enableHiveSupport()
    .getOrCreate()

  // ETL日期
  val etl_date: String = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))

  // ========== Hive操作 ==========

  /**
   * 读取Hive表
   * @param db 数据库名
   * @param table 表名
   * @return DataFrame
   */
  def readHive(db: String, table: String): DataFrame = spark.table(s"$db.$table")

  /**
   * 写入Hive表（支持分区）
   * @param db 数据库名
   * @param writeMode 写入模式
   * @param table 表名
   * @param df 数据DataFrame
   * @param partition 分区字段，默认为"etl_date"
   */
  def writeHive(db: String, writeMode: SaveMode, table: String, df: DataFrame, partition: String = "etl_date"): Unit = {
    df.write
      .mode(writeMode)
      .partitionBy(partition)
      .saveAsTable(s"$db.$table")
  }

  // ========== MySQL操作 ==========

  /**
   * 获取JDBC连接选项
   * @param db 数据库名，默认为"Taxi_TLC"
   * @return JDBC连接选项Map
   */
  def getJdbcOptions(db: String = "taxi_analysis"): Map[String, String] = {
    Map(
      JDBCOptions.JDBC_URL -> s"jdbc:mysql://192.168.127.102:3306/$db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&user=root&password=BAi@123456&characterEncoding=utf-8",
      JDBCOptions.JDBC_DRIVER_CLASS -> "com.mysql.cj.jdbc.Driver"
    )
  }

  /**
   * 从MySQL读取数据
   * @param query SQL查询语句
   * @return DataFrame
   */
  def loadMysql(query: String): DataFrame = {
    spark.read.format("jdbc")
      .options(getJdbcOptions())
      .option(JDBCOptions.JDBC_QUERY_STRING, query)
      .load()
  }

  /**
   * 写入数据到MySQL
   * @param df 数据DataFrame
   * @param tableName 目标表名
   * @param saveMode 保存模式
   * @param db 数据库名，默认为"Taxi_TLC"
   */
  def writeMysql(df: DataFrame, tableName: String, saveMode: SaveMode, db: String = "taxi_analysis"): Unit = {
    df.write
      .format("jdbc")
      .options(getJdbcOptions(db))
      .option("dbtable", tableName)
      .mode(saveMode)
      .save()
  }

  /**
   * 执行SQL语句
   * @param sql SQL语句
   * @return DataFrame
   */
  def executeSql(sql: String): DataFrame = {
    spark.sql(sql)
  }

}

