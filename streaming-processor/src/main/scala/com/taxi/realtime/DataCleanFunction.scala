package com.taxi.realtime

import com.taxi.realtime.model.{ErrorRecordJava, GreenTripRecord}
import com.taxi.realtime.utils.{Constants, DataCleaner, LoggerUtil}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.{Collector, OutputTag}

class DataCleanFunction extends ProcessFunction[GreenTripRecord, GreenTripRecord] {
  
  // 显式指定 OutputTag 的类型信息
  val cleaningErrors: OutputTag[ErrorRecordJava] = new OutputTag[ErrorRecordJava]("cleaning-errors") {
    override def getTypeInfo: TypeInformation[ErrorRecordJava] = 
      TypeInformation.of(classOf[ErrorRecordJava])
  }

  override def processElement(
    record: GreenTripRecord, 
    context: ProcessFunction[GreenTripRecord, GreenTripRecord]#Context, 
    collector: Collector[GreenTripRecord]
  ): Unit = {
    // 检查记录是否为 null
    if (record == null) {
      context.output(cleaningErrors, new ErrorRecordJava("null", "Null record received"))
      LoggerUtil.warn("Received null record in cleaning function")
      return
    }
    
    DataCleaner.clean(Some(record)) match {
      case Some(cleanedRecord) => 
        collector.collect(cleanedRecord)
        LoggerUtil.debug(s"Successfully cleaned record: ${cleanedRecord.toString.take(200)}")
      case None => 
        context.output(cleaningErrors, new ErrorRecordJava(record.toString, Constants.ERROR_DATA_QUALITY))
        LoggerUtil.warn(s"Data cleaning failed for record: ${record.toString.take(200)}")
    }
  }
}