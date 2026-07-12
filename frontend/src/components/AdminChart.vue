<template>
  <div class="admin-chart-shell" v-loading="loading">
    <div v-if="error" class="admin-chart-state">
      <el-empty description="图表加载失败" :image-size="64">
        <el-button size="small" @click="$emit('retry')">重试</el-button>
      </el-empty>
    </div>
    <el-empty v-else-if="empty && !loading" description="暂无数据" :image-size="64" />
    <div v-show="!error && !empty" ref="chartEl" class="admin-chart" :style="{ height }" />
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import * as echarts from 'echarts/core'

use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  option: { type: Object, required: true },
  loading: Boolean,
  empty: Boolean,
  error: Boolean,
  height: { type: String, default: '300px' }
})
defineEmits(['retry'])

const chartEl = ref()
let chart
let observer

async function render() {
  if (props.empty || props.error || !chartEl.value) return
  await nextTick()
  chart ||= echarts.init(chartEl.value)
  chart.setOption(props.option, true)
  chart.resize()
}

watch(() => [props.option, props.empty, props.error], render, { deep: true })
onMounted(() => {
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(chartEl.value)
  render()
})
onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>
