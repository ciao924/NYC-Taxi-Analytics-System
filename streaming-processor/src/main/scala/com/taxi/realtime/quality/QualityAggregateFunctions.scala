package com.taxi.realtime.quality

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

class QualityAggregateFunction extends AggregateFunction[(String, QualityEvent), QualityWindowResult, QualityWindowResult] {
  override def createAccumulator(): QualityWindowResult = QualityWindowResult(0L, 0L, 0L, 0L, 0L, 0L)

  override def add(value: (String, QualityEvent), acc: QualityWindowResult): QualityWindowResult = {
    value._2 match {
      case NormalEvent() => acc.copy(totalRecords = acc.totalRecords + 1)
      case ParseErrorEvent() => acc.copy(totalRecords = acc.totalRecords + 1, parseErrors = acc.parseErrors + 1)
      case CleanErrorEvent() => acc.copy(totalRecords = acc.totalRecords + 1, cleanErrors = acc.cleanErrors + 1)
      case TimestampErrorEvent() => acc.copy(totalRecords = acc.totalRecords + 1, timestampErrors = acc.timestampErrors + 1)
    }
  }

  override def getResult(acc: QualityWindowResult): QualityWindowResult = acc

  override def merge(a: QualityWindowResult, b: QualityWindowResult): QualityWindowResult = {
    QualityWindowResult(
      math.min(a.windowStart, b.windowStart),
      math.max(a.windowEnd, b.windowEnd),
      a.totalRecords + b.totalRecords,
      a.parseErrors + b.parseErrors,
      a.cleanErrors + b.cleanErrors,
      a.timestampErrors + b.timestampErrors
    )
  }
}

class QualityWindowFunction extends org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction[QualityWindowResult, QualityWindowResult, String, TimeWindow] {
  override def process(key: String, context: org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction[QualityWindowResult, QualityWindowResult, String, TimeWindow]#Context, elements: java.lang.Iterable[QualityWindowResult], out: org.apache.flink.util.Collector[QualityWindowResult]): Unit = {
    val result = elements.iterator().next()
    out.collect(result.copy(windowStart = context.window.getStart, windowEnd = context.window.getEnd))
  }
}