# 数据分析模块修复日志

## 修复概述

修复时间：2026-05-09

修复人员：系统维护团队

## 问题描述

### 问题1：数据展示问题（查询后数据不显示）

**问题原因**：后端MyBatis的AnalysisMapper接口没有对应的XML映射文件，导致所有数据库查询都会抛出异常，只能返回mock数据。当使用查询功能后，由于数据库连接或SQL执行失败，服务层捕获异常并返回mock数据，但mock数据生成逻辑正确，只是缺少真实数据库查询能力。

**影响范围**：所有数据分析模块的四个数据展示（机场统计、供应商对比、支付方式、行程分析）都无法正确显示数据库中的真实数据。

---

### 问题2：支付方式订单数显示为百分比

**问题原因**：数据库表`analysis_payment_analysis`中存储占比的字段名为`trip_ratio`，但前端API接口期望的字段名为`percentage`，导致数据映射不一致。

**影响范围**：支付方式模块的订单数字段显示的是百分比值而不是正确的订单数。

---

## 解决方案

### 修复1：创建MyBatis XML映射文件

创建`src/main/resources/mapper/AnalysisMapper.xml`，包含所有数据分析相关的SQL查询语句：

- `selectAirportStatistics` - 机场统计查询
- `selectAirportDetailedStatistics` - 机场详细统计查询
- `selectVendorComparison` - 供应商对比查询
- `selectVendorTrend` - 供应商趋势查询
- `selectPaymentDistribution` - 支付方式分布查询（关键修复）
- `selectPaymentTrend` - 支付方式趋势查询
- `selectDistanceDistribution` - 距离分布查询
- `selectDurationDistribution` - 时长分布查询

**修改位置**：`backend/src/main/resources/mapper/AnalysisMapper.xml`（新建文件）

---

### 修复2：修复字段映射问题

在支付方式分布查询中，将数据库字段`trip_ratio`映射为前端期望的`percentage`字段：

```xml
<select id="selectPaymentDistribution" resultType="map">
    SELECT 
        CASE 
            WHEN payment_name = '信用卡' THEN '1'
            WHEN payment_name = '现金' THEN '2'
            WHEN payment_name = '无消费' THEN '3'
            WHEN payment_name = '争议' THEN '4'
            ELSE '1'
        END as payment_type,
        payment_name as payment_desc,
        CASE 
            WHEN payment_name = '信用卡' THEN 'credit_card'
            WHEN payment_name = '现金' THEN 'cash'
            WHEN payment_name = '无消费' THEN 'no_charge'
            WHEN payment_name = '争议' THEN 'dispute'
            ELSE 'credit_card'
        END as payment_code,
        SUM(trip_count) as trip_count,
        AVG(trip_ratio) as percentage,  <!-- 关键修复：将trip_ratio映射为percentage -->
        SUM(total_amount) as total_amount
    FROM analysis_payment_analysis
    WHERE stat_date BETWEEN #{startDate} AND #{endDate}
    GROUP BY payment_name
</select>
```

**修改位置**：`backend/src/main/resources/mapper/AnalysisMapper.xml`

---

## 验证方式

1. 启动后端服务：
   ```bash
   cd taxi/taxi-analytics-backend
   mvn spring-boot:run
   ```

2. 启动前端开发服务器：
   ```bash
   cd taxi/taxi-analytics-frontend
   npm run dev
   ```

3. 访问数据分析页面：
   ```
   http://localhost:5173/analysis
   ```

4. 验证项目：
   - 点击"查询数据"按钮，确认数据正常显示
   - 切换各个Tab（机场统计、供应商对比、支付方式、行程分析），确认数据正常展示
   - 支付方式模块显示正确的订单数和百分比

---

## 修复结果

| 问题 | 状态 | 修复方式 |
|------|------|----------|
| 数据展示问题（查询后数据不显示） | ✅ 已修复 | 创建MyBatis XML映射文件 |
| 支付方式订单数显示为百分比 | ✅ 已修复 | 修复字段映射（trip_ratio → percentage） |

---

## 相关文件

- `backend/src/main/resources/mapper/AnalysisMapper.xml` - MyBatis映射文件（新建）
- `backend/src/main/java/com/taxi/analytics/modules/analysis/mapper/AnalysisMapper.java` - Mapper接口
- `backend/src/main/java/com/taxi/analytics/modules/analysis/service/impl/AnalysisServiceImpl.java` - 服务实现类
- `taxi/taxi-analytics-frontend/src/views/analysis/Index.vue` - 数据分析前端组件
- `taxi/taxi-analytics-frontend/docs/ANALYSIS_FIX_LOG.md` - 修复日志文档