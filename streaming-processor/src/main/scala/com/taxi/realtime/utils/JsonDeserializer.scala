package com.taxi.realtime.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.taxi.realtime.model.GreenTripRecord

import java.sql.Timestamp

object JsonDeserializer {
  private val objectMapper = new ObjectMapper()
    .enable(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS)

  def deserialize(json: String): Option[GreenTripRecord] = {
    try {
      // 清洗 JSON 中的 NaN 值
      val cleanedJson = cleanJson(json)
      val node = objectMapper.readTree(cleanedJson)
      
      // 支持绿色出租车 (lpep_) 和黄色出租车 (tpep_) 的时间字段
      val pickupDatetimeField = if (node.has("lpep_pickup_datetime")) "lpep_pickup_datetime" else "tpep_pickup_datetime"
      val dropoffDatetimeField = if (node.has("lpep_dropoff_datetime")) "lpep_dropoff_datetime" else "tpep_dropoff_datetime"
      
      // 识别出租车类型
      val taxiType = if (node.has("lpep_pickup_datetime")) Some("green") else Some("yellow")
      
      val record = GreenTripRecord(
        vendorId = DataUtil.parseInteger(getNodeValue(node, "VendorID")),
        pickupDatetime = parseTimestamp(getNodeValue(node, pickupDatetimeField)),
        dropoffDatetime = parseTimestamp(getNodeValue(node, dropoffDatetimeField)),
        passengerCount = DataUtil.parseInteger(getNodeValue(node, "passenger_count")),
        tripDistance = DataUtil.parseDouble(getNodeValue(node, "trip_distance")),
        puLocationId = DataUtil.parseInteger(getNodeValue(node, "PULocationID")),
        doLocationId = DataUtil.parseInteger(getNodeValue(node, "DOLocationID")),
        fareAmount = DataUtil.parseDouble(getNodeValue(node, "fare_amount")),
        tipAmount = DataUtil.parseDouble(getNodeValue(node, "tip_amount")),
        totalAmount = DataUtil.parseDouble(getNodeValue(node, "total_amount")),
        processTime = Some(new Timestamp(System.currentTimeMillis())),
        taxiType = taxiType
      )
      Some(record)
    } catch {
      case e: Exception =>
        LoggerUtil.error(s"${Constants.ERROR_JSON_PARSING}: $json", e)
        None
    }
  }
  
  private def cleanJson(json: String): String = {
    // 替换 JSON 中的 NaN 值为 null
    json.replace(": NaN", ": null")
  }

  private def getNodeValue(node: JsonNode, field: String): Any = {
    // 尝试直接获取字段
    if (node.has(field)) {
      val fieldNode = node.get(field)
      return getNodeValueFromNode(fieldNode)
    }
    
    // 尝试不同的字段名格式
    val alternativeFields = getAlternativeFieldNames(field)
    for (altField <- alternativeFields) {
      if (node.has(altField)) {
        val fieldNode = node.get(altField)
        return getNodeValueFromNode(fieldNode)
      }
    }
    
    null
  }
  
  private def getNodeValueFromNode(fieldNode: JsonNode): Any = {
    if (fieldNode.isNull) {
      null
    } else if (fieldNode.isNumber) {
      fieldNode.numberValue()
    } else {
      fieldNode.asText()
    }
  }
  
  private def getAlternativeFieldNames(field: String): Seq[String] = {
    field match {
      case "vendor_id" => Seq("VendorID", "vendorID", "VendorId")
      case "pickup_datetime" => Seq("lpep_pickup_datetime", "PickupDateTime", "pickupDateTime")
      case "dropoff_datetime" => Seq("lpep_dropoff_datetime", "DropoffDateTime", "dropoffDateTime")
      case "pu_location_id" => Seq("PULocationID", "puLocationId", "PULocationId")
      case "do_location_id" => Seq("DOLocationID", "doLocationId", "DOLocationId")
      case _ => Seq()
    }
  }

  private def parseTimestamp(value: Any): Option[Timestamp] = {
    value match {
      case null => None
      case s: String => DataUtil.parseTimestamp(s)
      case _ => None
    }
  }
}
