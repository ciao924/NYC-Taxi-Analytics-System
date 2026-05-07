<template>
  <div class="heatmap-page">
    <div class="heatmap-container">
      <div class="date-selector">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
          class="date-picker"
        />
        <el-radio-group v-model="hotspotType" @change="onTypeChange">
          <el-radio value="pickup">上车热点</el-radio>
          <el-radio value="dropoff">下车热点</el-radio>
        </el-radio-group>
        <el-button type="primary" @click="loadData" :loading="loading">查询</el-button>
      </div>
      <div v-if="error" class="error-message">
        <el-alert
          :title="error"
          type="error"
          show-icon
          :closable="false"
        />
      </div>
      <div v-if="mapReady" id="heatmap-map" class="map-container"></div>
      <div v-else class="loading-map">
        <el-spinner size="large" />
        <p>地图加载中...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import AMapLoader from '@amap/amap-jsapi-loader';
import { mapApi } from '@/api/map';
import { ElMessage } from 'element-plus';

const AMAP_KEY = '0d812bcc4a5e2d24dfeb8d447fda7e5c';
const NYC_CENTER = [-74.006, 40.7128] as [number, number];

const selectedDate = ref('2025-03-31');
const hotspotType = ref('pickup');
const loading = ref(false);
const mapReady = ref(false);
const error = ref('');
let map: any = null;
let heatmap: any = null;

interface HeatmapPoint {
  lng: number;
  lat: number;
  count: number;
}

const fetchHeatmapData = async (): Promise<HeatmapPoint[]> => {
  if (hotspotType.value === 'pickup') {
    return await mapApi.getPickupHeatmap({ date: '2025-03-31' });
  } else {
    return await mapApi.getDropoffHeatmap({ date: '2025-03-31' });
  }
};

const renderHeatmap = async () => {
  if (!map || !heatmap) return;

  loading.value = true;
  error.value = '';
  
  try {
    const data = await fetchHeatmapData();
    
    if (data.length === 0) {
      error.value = '未获取到热力图数据，请尝试其他日期';
      ElMessage.warning('未获取到热力图数据');
      return;
    }
    
    const heatmapData = data.map((item: HeatmapPoint) => ({
      lng: item.lng,
      lat: item.lat,
      count: item.count || 1
    }));

    if (heatmapData.length > 0) {
      heatmap.setDataSet({
        data: heatmapData,
        max: Math.max(...heatmapData.map((d: HeatmapPoint) => d.count), 100)
      });
      ElMessage.success('热力图数据加载成功');
    }
  } catch (err: any) {
    error.value = `数据加载失败: ${err.message || '未知错误'}`;
    ElMessage.error('数据加载失败，请检查网络连接');
    console.error('Failed to load heatmap data:', err);
  } finally {
    loading.value = false;
  }
};

const onDateChange = () => {
  renderHeatmap();
};

const onTypeChange = () => {
  renderHeatmap();
};

const loadData = () => {
  renderHeatmap();
};

onMounted(async () => {
  try {
    const AMap = await AMapLoader.load({
      key: AMAP_KEY,
      version: '2.0',
      plugins: ['AMap.Heatmap'],
    });

    map = new AMap.Map('heatmap-map', {
      zoom: 12,
      center: NYC_CENTER,
      viewMode: '2D',
    });

    map.plugin(['AMap.Heatmap'], () => {
      try {
        heatmap = new AMap.Heatmap(map, {
          radius: 25,
          opacity: [0, 0.8],
          gradient: {
            0.4: 'blue',
            0.6: 'cyan',
            0.7: 'lime',
            0.8: 'yellow',
            1.0: 'red'
          }
        });
        mapReady.value = true;
        renderHeatmap();
      } catch (err) {
        error.value = '热力图层初始化失败';
        ElMessage.error('热力图层初始化失败');
        console.error('Failed to initialize heatmap:', err);
      }
    });
  } catch (err: any) {
    error.value = `地图加载失败: ${err.message || '未知错误'}`;
    ElMessage.error('地图加载失败');
    console.error('Failed to load AMap:', err);
  }
});
</script>

<style scoped lang="scss">
.heatmap-page {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.heatmap-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.date-selector {
  margin-bottom: 20px;
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.date-picker {
  width: 180px;
}

.error-message {
  margin-bottom: 16px;
}

.loading-map {
  height: 600px;
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
}

.loading-map p {
  margin-top: 16px;
  color: #666;
}

.map-container {
  height: 600px;
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>