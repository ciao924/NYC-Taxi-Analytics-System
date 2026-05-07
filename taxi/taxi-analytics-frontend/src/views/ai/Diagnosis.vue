<template>
  <div class="ai-diagnosis-container">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="数据倾斜诊断" name="skew">
        <el-card>
          <template #header>
            <span>Spark 数据倾斜诊断</span>
          </template>
          <el-form :model="skewForm" label-width="120px">
            <el-form-item label="Job ID">
              <el-input v-model="skewForm.jobId" placeholder="输入Job ID" />
            </el-form-item>
            <el-form-item label="执行计划">
              <el-input
                v-model="skewForm.executionPlan"
                type="textarea"
                :rows="8"
                placeholder="粘贴Spark执行计划JSON或文本"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="skewLoading" @click="diagnoseSkew">
                开始诊断
              </el-button>
            </el-form-item>
          </el-form>

          <el-divider v-if="skewResult.hasSkew !== null" />

          <div v-if="skewResult.hasSkew !== null" class="result-card">
            <el-alert :type="skewResult.hasSkew ? 'warning' : 'success'" show-icon>
              <template #title>
                {{ skewResult.hasSkew ? '检测到数据倾斜' : '未检测到明显数据倾斜' }}
              </template>
            </el-alert>

            <div v-if="skewResult.hasSkew" class="skew-details">
              <h4>倾斜Stage列表</h4>
              <el-tag v-for="stage in skewResult.skewedStages" :key="stage" class="mr-2">
                {{ stage }}
              </el-tag>

              <h4>倾斜因子</h4>
              <el-progress
                :percentage="Math.min(skewResult.skewFactor * 100, 100)"
                :color="skewResult.skewFactor > 0.5 ? '#f56c6c' : '#e6a23c'"
              />

              <h4>优化建议</h4>
              <el-list>
                <el-list-item v-for="(rec, index) in skewResult.recommendations" :key="index">
                  {{ rec }}
                </el-list-item>
              </el-list>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Flink反压诊断" name="backpressure">
        <el-card>
          <template #header>
            <span>Flink 反压诊断</span>
          </template>
          <el-form :model="flinkForm" label-width="120px">
            <el-form-item label="Job ID">
              <el-input v-model="flinkForm.jobId" placeholder="输入Flink Job ID" />
            </el-form-item>
            <el-form-item label="算子指标">
              <el-input
                v-model="flinkForm.operatorMetricsText"
                type="textarea"
                :rows="6"
                placeholder="输入算子指标JSON，每行一个算子"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="flinkLoading" @click="diagnoseFlink">
                开始诊断
              </el-button>
            </el-form-item>
          </el-form>

          <div v-if="flinkResult.hasBackpressure !== null" class="result-card">
            <el-alert :type="flinkResult.hasBackpressure ? 'warning' : 'success'" show-icon>
              <template #title>
                {{ flinkResult.hasBackpressure ? '检测到反压' : '未检测到反压' }}
              </template>
            </el-alert>

            <div v-if="flinkResult.hasBackpressure" class="backpressure-details">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="瓶颈算子">
                  {{ flinkResult.bottleneckOperator }}
                </el-descriptions-item>
                <el-descriptions-item label="反压比例">
                  {{ (flinkResult.backpressureRatio * 100).toFixed(2) }}%
                </el-descriptions-item>
              </el-descriptions>

              <h4>优化建议</h4>
              <el-list>
                <el-list-item v-for="(rec, index) in flinkResult.recommendations" :key="index">
                  {{ rec }}
                </el-list-item>
              </el-list>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="任务失败诊断" name="task">
        <el-card>
          <template #header>
            <span>DolphinScheduler 任务诊断</span>
          </template>
          <el-form :model="taskForm" label-width="120px">
            <el-form-item label="Task ID">
              <el-input v-model="taskForm.taskId" placeholder="输入Task ID" />
            </el-form-item>
            <el-form-item label="任务日志">
              <el-input
                v-model="taskForm.taskLog"
                type="textarea"
                :rows="10"
                placeholder="粘贴任务日志"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="taskLoading" @click="diagnoseTask">
                开始诊断
              </el-button>
            </el-form-item>
          </el-form>

          <div v-if="taskResult.rootCause" class="result-card">
            <el-alert type="info" show-icon>
              <template #title>
                诊断结果 (置信度: {{ (taskResult.confidence * 100).toFixed(0) }}%)
              </template>
            </el-alert>

            <el-descriptions :column="1" border class="mt-4">
              <el-descriptions-item label="错误类别">
                <el-tag>{{ taskResult.category }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="根因分析">
                {{ taskResult.rootCause }}
              </el-descriptions-item>
            </el-descriptions>

            <h4 class="mt-4">解决建议</h4>
            <el-steps direction="vertical" :space="60">
              <el-step
                v-for="(suggestion, index) in taskResult.suggestions"
                :key="index"
                :title="`步骤 ${index + 1}`"
                :description="suggestion"
              />
            </el-steps>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="并行度推荐" name="parallelism">
        <el-card>
          <template #header>
            <span>Flink 并行度推荐</span>
          </template>
          <el-form :model="parallelismForm" label-width="140px">
            <el-form-item label="Job ID">
              <el-input v-model="parallelismForm.jobId" placeholder="输入Job ID" />
            </el-form-item>
            <el-form-item label="当前吞吐量">
              <el-input-number v-model="parallelismForm.currentThroughput" :min="0" />
              <span class="ml-2">records/sec</span>
            </el-form-item>
            <el-form-item label="当前并行度">
              <el-input-number v-model="parallelismForm.currentParallelism" :min="1" :max="1000" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="parallelismLoading" @click="recommendParallelism">
                获取推荐
              </el-button>
            </el-form-item>
          </el-form>

          <div v-if="parallelismResult.recommendedParallelism" class="result-card">
            <el-result
              icon="success"
              :title="`推荐并行度: ${parallelismResult.recommendedParallelism}`"
              :sub-title="`预期性能提升: ${parallelismResult.expectedImprovement.toFixed(1)}%`"
            >
              <template #extra>
                <el-button type="primary">应用推荐</el-button>
              </template>
            </el-result>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="智能规则生成" name="rules">
        <el-card>
          <template #header>
            <span>数据质量规则推荐</span>
          </template>
          <el-form :model="rulesForm" label-width="120px">
            <el-form-item label="表名">
              <el-select v-model="rulesForm.tableName" placeholder="选择表名">
                <el-option label="dwd_taxi_trip" value="dwd_taxi_trip" />
                <el-option label="ods_taxi_trip" value="ods_taxi_trip" />
                <el-option label="dwd_quality_metrics" value="dwd_quality_metrics" />
              </el-select>
            </el-form-item>
            <el-form-item label="列样本">
              <el-input
                v-model="rulesForm.columnSamplesText"
                type="textarea"
                :rows="6"
                placeholder="输入列样本数据，每行一个字段"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="rulesLoading" @click="suggestRules">
                生成规则
              </el-button>
            </el-form-item>
          </el-form>

          <div v-if="rulesResult.suggestedRules?.length" class="result-card">
            <el-alert type="success" show-icon>
              <template #title>
                推荐规则 (置信度: {{ (rulesResult.confidence * 100).toFixed(0) }}%)
              </template>
            </el-alert>

            <el-table :data="rulesResult.suggestedRules" stripe class="mt-4">
              <el-table-column prop="columnName" label="字段" />
              <el-table-column prop="ruleType" label="规则类型">
                <template #default="{ row }">
                  <el-tag>{{ row.ruleType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="threshold" label="阈值" />
              <el-table-column prop="description" label="描述" />
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const activeTab = ref('skew')

const skewForm = reactive({
  jobId: '',
  executionPlan: ''
})

const skewResult = reactive({
  hasSkew: null,
  skewedStages: [],
  skewFactor: 0,
  recommendations: []
})

const flinkForm = reactive({
  jobId: '',
  operatorMetricsText: ''
})

const flinkResult = reactive({
  hasBackpressure: null,
  bottleneckOperator: '',
  backpressureRatio: 0,
  recommendations: []
})

const taskForm = reactive({
  taskId: '',
  taskLog: ''
})

const taskResult = reactive({
  rootCause: '',
  category: '',
  suggestions: [],
  confidence: 0
})

const parallelismForm = reactive({
  jobId: '',
  currentThroughput: 1000,
  currentParallelism: 4
})

const parallelismResult = reactive({
  recommendedParallelism: 0,
  expectedImprovement: 0,
  operatorRecommendations: []
})

const rulesForm = reactive({
  tableName: 'dwd_taxi_trip',
  columnSamplesText: ''
})

const rulesResult = reactive({
  suggestedRules: [],
  confidence: 0
})

const skewLoading = ref(false)
const flinkLoading = ref(false)
const taskLoading = ref(false)
const parallelismLoading = ref(false)
const rulesLoading = ref(false)

const diagnoseSkew = async () => {
  skewLoading.value = true
  try {
    const response = await axios.post('/api/ai/skew-diagnose', {
      jobId: skewForm.jobId,
      executionPlan: skewForm.executionPlan
    })

    const data = response.data.data
    skewResult.hasSkew = data.hasSkew
    skewResult.skewedStages = data.skewedStages || []
    skewResult.skewFactor = data.skewFactor || 0
    skewResult.recommendations = data.recommendations || []

    ElMessage.success('诊断完成')
  } catch (error) {
    ElMessage.error('诊断失败: ' + (error.message || '未知错误'))
  } finally {
    skewLoading.value = false
  }
}

const diagnoseFlink = async () => {
  flinkLoading.value = true
  try {
    const metrics = flinkForm.operatorMetricsText
      .split('\n')
      .filter(line => line.trim())

    const response = await axios.post('/api/ai/flink/backpressure', {
      jobId: flinkForm.jobId,
      operatorMetrics: metrics
    })

    const data = response.data.data
    flinkResult.hasBackpressure = data.hasBackpressure
    flinkResult.bottleneckOperator = data.bottleneckOperator || ''
    flinkResult.backpressureRatio = data.backpressureRatio || 0
    flinkResult.recommendations = data.recommendations || []

    ElMessage.success('诊断完成')
  } catch (error) {
    ElMessage.error('诊断失败: ' + (error.message || '未知错误'))
  } finally {
    flinkLoading.value = false
  }
}

const diagnoseTask = async () => {
  taskLoading.value = true
  try {
    const response = await axios.post('/api/ai/diagnose-task', {
      taskId: taskForm.taskId,
      taskLog: taskForm.taskLog
    })

    const data = response.data.data
    taskResult.rootCause = data.rootCause || ''
    taskResult.category = data.category || ''
    taskResult.suggestions = data.suggestions || []
    taskResult.confidence = data.confidence || 0

    ElMessage.success('诊断完成')
  } catch (error) {
    ElMessage.error('诊断失败: ' + (error.message || '未知错误'))
  } finally {
    taskLoading.value = false
  }
}

const recommendParallelism = async () => {
  parallelismLoading.value = true
  try {
    const response = await axios.post('/api/ai/flink/parallelism', {
      jobId: parallelismForm.jobId,
      currentThroughput: parallelismForm.currentThroughput,
      currentParallelism: parallelismForm.currentParallelism
    })

    const data = response.data.data
    parallelismResult.recommendedParallelism = data.recommendedParallelism || 0
    parallelismResult.expectedImprovement = data.expectedImprovement || 0
    parallelismResult.operatorRecommendations = data.operatorRecommendations || []

    ElMessage.success('推荐生成完成')
  } catch (error) {
    ElMessage.error('推荐失败: ' + (error.message || '未知错误'))
  } finally {
    parallelismLoading.value = false
  }
}

const suggestRules = async () => {
  rulesLoading.value = true
  try {
    const columnSamples = rulesForm.columnSamplesText
      .split('\n')
      .filter(line => line.trim())

    const response = await axios.post('/api/ai/suggest-rules', {
      tableName: rulesForm.tableName,
      columnSamples: columnSamples
    })

    const data = response.data.data
    rulesResult.suggestedRules = data.suggestedRules || []
    rulesResult.confidence = data.confidence || 0

    ElMessage.success('规则生成完成')
  } catch (error) {
    ElMessage.error('规则生成失败: ' + (error.message || '未知错误'))
  } finally {
    rulesLoading.value = false
  }
}
</script>

<style scoped>
.ai-diagnosis-container {
  padding: 0;
}

.result-card {
  margin-top: 20px;
}

.skew-details,
.backpressure-details {
  margin-top: 16px;
}

.skew-details h4,
.backpressure-details h4 {
  margin: 16px 0 8px;
  color: #606266;
}

.mr-2 {
  margin-right: 8px;
}

.mt-4 {
  margin-top: 16px;
}

.ml-2 {
  margin-left: 8px;
}
</style>