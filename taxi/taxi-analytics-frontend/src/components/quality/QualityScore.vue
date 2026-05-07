<template>
  <div class="quality-score">
    <div class="score-header">
      <div class="score-title">{{ title }}</div>
      <div class="score-subtitle">{{ subtitle }}</div>
    </div>
    <div class="score-content">
      <div class="score-circle">
        <div class="circle-container" ref="circleContainer">
          <canvas ref="scoreCanvas" :width="200" :height="200"></canvas>
          <div class="score-value">{{ score }}</div>
          <div class="score-label">综合评分</div>
        </div>
      </div>
      <div class="score-details">
        <div class="detail-item" v-for="(item, index) in scoreDetails" :key="index">
          <div class="detail-label">{{ item.label }}</div>
          <div class="detail-value">{{ item.value }}</div>
          <div class="detail-bar">
            <el-progress 
              :percentage="item.value" 
              :color="getProgressColor(item.value)"
              :stroke-width="8"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

const props = defineProps<{
  title: string
  subtitle?: string
  score: number
  scoreDetails: Array<{
    label: string
    value: number
  }>
}>()

const circleContainer = ref<HTMLElement | null>(null)
const scoreCanvas = ref<HTMLCanvasElement | null>(null)

const getProgressColor = (value: number) => {
  if (value >= 80) {
    return '#67c23a'
  } else if (value >= 60) {
    return '#e6a23c'
  } else {
    return '#f56c6c'
  }
}

const drawScoreCircle = () => {
  if (!scoreCanvas.value) return
  
  const canvas = scoreCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  const width = canvas.width
  const height = canvas.height
  const centerX = width / 2
  const centerY = height / 2
  const radius = 80
  const lineWidth = 12
  
  // 清空画布
  ctx.clearRect(0, 0, width, height)
  
  // 绘制背景圆
  ctx.beginPath()
  ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI)
  ctx.strokeStyle = '#e4e7ed'
  ctx.lineWidth = lineWidth
  ctx.stroke()
  
  // 绘制进度圆
  const progress = props.score / 100
  const startAngle = -Math.PI / 2
  const endAngle = startAngle + 2 * Math.PI * progress
  
  ctx.beginPath()
  ctx.arc(centerX, centerY, radius, startAngle, endAngle)
  ctx.strokeStyle = getProgressColor(props.score)
  ctx.lineWidth = lineWidth
  ctx.lineCap = 'round'
  ctx.stroke()
}

onMounted(() => {
  drawScoreCircle()
})

watch(() => props.score, () => {
  drawScoreCircle()
})

watch(() => props.scoreDetails, () => {
  drawScoreCircle()
}, { deep: true })
</script>

<style scoped>
.quality-score {
  width: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.score-header {
  text-align: center;
  margin-bottom: 24px;
}

.score-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.score-subtitle {
  font-size: 14px;
  color: #909399;
}

.score-content {
  display: flex;
  align-items: center;
  gap: 40px;
  flex-wrap: wrap;
}

.score-circle {
  flex: 0 0 auto;
}

.circle-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 36px;
  font-weight: bold;
  color: #303133;
}

.score-label {
  position: absolute;
  top: 65%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 14px;
  color: #909399;
}

.score-details {
  flex: 1;
  min-width: 300px;
}

.detail-item {
  margin-bottom: 16px;
}

.detail-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 14px;
  color: #606266;
}

.detail-value {
  font-weight: 500;
  color: #303133;
}

.detail-bar {
  margin-top: 4px;
}

@media (max-width: 768px) {
  .score-content {
    flex-direction: column;
    text-align: center;
  }
  
  .score-details {
    width: 100%;
  }
}
</style>