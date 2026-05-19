<template>
  <div ref="chartDom" class="echarts-container"></div>
</template>

<script>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'

export default {
  name: 'ChartRenderer',
  props: {
    config: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    const chartDom = ref(null)
    let chartInstance = null

    const initChart = () => {
      if (!chartDom.value || !props.config) return

      if (!chartInstance) {
        chartInstance = echarts.init(chartDom.value)
      }

      const option = {
        title: {
          text: props.config.title || '',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: props.config.type === 'pie' ? 'item' : 'axis'
        },
        legend: {
          bottom: 10,
          left: 'center'
        },
        xAxis: props.config.type === 'pie' ? undefined : {
          type: 'category',
          data: props.config.xAxis || []
        },
        yAxis: props.config.type === 'pie' ? undefined : {
          type: 'value',
          name: props.config.yAxis?.[0]?.name || ''
        },
        series: props.config.series || []
      }

      chartInstance.setOption(option)
      chartInstance.resize()
    }

    onMounted(() => {
      initChart()
      window.addEventListener('resize', () => {
        if (chartInstance) {
          chartInstance.resize()
        }
      })
    })

    watch(() => props.config, () => {
      initChart()
    }, { deep: true })

    onUnmounted(() => {
      if (chartInstance) {
        chartInstance.dispose()
      }
      window.removeEventListener('resize', () => {})
    })

    return { chartDom }
  }
}
</script>

<style scoped>
.echarts-container {
  width: 100%;
  height: 300px;
}
</style>