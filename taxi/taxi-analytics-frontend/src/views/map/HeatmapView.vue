<template>
  <div class="heatmap-page">
    <div class="heatmap-container">
      <div class="dashboard-header">
        <div class="header-left">
          <h2 class="page-title">热力图分析</h2>
          <p class="page-subtitle">分析出租车上下车热点区域分布</p>
        </div>
        <div class="header-right">
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
      </div>
      <div v-if="error" class="error-message">
        <el-alert
          :title="error"
          type="error"
          show-icon
          :closable="false"
        />
      </div>
      <div class="chart-card">
        <div id="heatmap-map" class="map-container" v-loading="isMapLoading"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import AMapLoader from '@amap/amap-jsapi-loader';
import { mapApi } from '@/api/map';
import { ElMessage } from 'element-plus';

const AMAP_KEY = '0d812bcc4a5e2d24dfeb8d447fda7e5c';
const NYC_CENTER = [-74.006, 40.7128] as [number, number];

const selectedDate = ref('2025-03-31');
const hotspotType = ref('pickup');
const loading = ref(false);
const isMapLoading = ref(true);
const error = ref('');
let map: any = null;
let heatmap: any = null;
let mapLoadingInstance: any = null;

interface HeatmapPoint {
  lng: number;
  lat: number;
  count: number;
}

const fetchHeatmapData = async (): Promise<HeatmapPoint[]> => {
  try {
    if (hotspotType.value === 'pickup') {
      return await mapApi.getPickupHeatmap({ date: selectedDate.value });
    } else {
      return await mapApi.getDropoffHeatmap({ date: selectedDate.value });
    }
  } catch (err) {
    console.error('Failed to fetch heatmap data:', err);
    return [];
  }
};

const renderHeatmap = async () => {
  if (!map || !heatmap) {
    console.warn('Map or heatmap not initialized');
    return;
  }

  loading.value = true;
  error.value = '';
  
  try {
    const data = await fetchHeatmapData();
    
    if (!data || data.length === 0) {
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
      const maxCount = Math.max(...heatmapData.map((d: HeatmapPoint) => d.count), 100);
      
      if (typeof heatmap.setData === 'function') {
        heatmap.setData(heatmapData, maxCount);
      } else if (typeof heatmap.setDataSet === 'function') {
        heatmap.setDataSet({
          data: heatmapData,
          max: maxCount
        });
      } else {
        throw new Error('Heatmap data setter method not found');
      }
      
      heatmap.show();
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

const initHeatmap = (AMap: any) => {
  return new Promise<void>((resolve, reject) => {
    map.plugin(['AMap.HeatMap'], () => {
      try {
        if (typeof AMap.HeatMap !== 'function') {
          throw new Error('AMap.HeatMap plugin not loaded');
        }
        
        heatmap = new AMap.HeatMap(map, {
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
        
        console.log('Heatmap initialized successfully');
        resolve();
      } catch (err) {
        reject(err);
      }
    });
  });
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
    console.log('Loading AMap...');
    
    const AMap = await AMapLoader.load({
      key: AMAP_KEY,
      version: '2.0',
      plugins: ['AMap.HeatMap'],
    });

    console.log('AMap loaded successfully');

    map = new AMap.Map('heatmap-map', {
      zoom: 12,
      center: NYC_CENTER,
      viewMode: '2D',
    });

    console.log('Map initialized');

    await initHeatmap(AMap);
    
    isMapLoading.value = false;
    await renderHeatmap();
    
  } catch (err: any) {
    error.value = `地图加载失败: ${err.message || '未知错误'}`;
    isMapLoading.value = false;
    ElMessage.error('地图加载失败');
    console.error('Failed to load AMap:', err);
  }
});

onUnmounted(() => {
  if (mapLoadingInstance) {
    mapLoadingInstance.close();
  }
  if (heatmap) {
    heatmap.hide();
    heatmap = null;
  }
  if (map) {
    map.destroy();
    map = null;
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
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.date-picker {
  width: 180px;
}

.error-message {
  margin-bottom: 16px;
}

.chart-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.map-container {
  height: 600px;
  width: 100%;
  background: #f9f9f9;
}
</style>
