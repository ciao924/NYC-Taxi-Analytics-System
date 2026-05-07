package com.taxi.realtime

import com.taxi.realtime.model.{ErrorRecordJava, GreenTripRecord}
import com.taxi.realtime.utils.{Constants, JsonDeserializer, LoggerUtil}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.{Collector, OutputTag}

class JsonParseFunction extends ProcessFunction[String, GreenTripRecord] {
  
  // 显式指定 OutputTag 的类型信息
  val parsingErrors: OutputTag[ErrorRecordJava] = new OutputTag[ErrorRecordJava]("parsing-errors") {
    override def getTypeInfo: TypeInformation[ErrorRecordJava] = 
      TypeInformation.of(classOf[ErrorRecordJava])
  }

  override def processElement(
    json: String, 
    context: ProcessFunction[String, GreenTripRecord]#Context, 
    collector: Collector[GreenTripRecord]
  ): Unit = {
    // 检查输入是否为 null 或空字符串
    if (json == null || json.trim.isEmpty) {
      context.output(parsingErrors, new ErrorRecordJava("null or empty", "Empty or null message"))
      LoggerUtil.warn("Received null or empty message")
      return
    }
    
    JsonDeserializer.deserialize(json) match {
      case Some(record) => 
        collector.collect(record)
        LoggerUtil.debug(s"Successfully parsed JSON: ${json.take(200)}")
      case None => 
        context.output(parsingErrors, new ErrorRecordJava(json, Constants.ERROR_JSON_PARSING))
        LoggerUtil.warn(s"JSON parsing failed: ${json.take(200)}")
    }
  }
}