<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';

import * as echarts from 'echarts';

// 保留原有props.code逻辑，同时添加可选配置
const props = defineProps<{
  code: string; // 原始JSON字符串配置
  height?: string; // 可选：图表高度
  theme?: string; // 可选：图表主题
  width?: string; // 可选：图表宽度
}>();

const refEle = ref<HTMLElement>();
const loading = ref(true);
let myChart: echarts.ECharts | null = null; // 图表实例引用

function parseEChartsOption(str: string): any {
  try {
    // 移除开头的 'option =' 和结尾的分号
    let cleanedStr = str
      .trim()
      .replace(/^option\s*=\s*/, '')
      .replace(/;\s*$/, '');

    // 对于包含函数的配置，使用Function解析
    if (
      cleanedStr.includes('=>') ||
      cleanedStr.includes('function') ||
      cleanedStr.includes('formatter:')
    ) {
      // 处理单引号问题，确保字符串使用双引号
      cleanedStr = cleanedStr
        .replaceAll(/([(\s:,])'([^']+)'([\s),])/g, '$1"$2"$3')
        .replaceAll(/([(\s:,])'([^']+)'$/g, '$1"$2"');

      // 处理未加引号的属性名（函数参数除外）
      cleanedStr = cleanedStr
        .replaceAll(/([{,]\s*)(\w+)\s*:/g, '$1"$2":')
        .replaceAll(/([{,]\s*)'(\w+)'\s*:/g, '$1"$2":')
        .replaceAll(/([{,]\s*)"(\w+)"\s*:/g, '$1"$2":');

      // 确保函数内部的字符串被正确转义
      cleanedStr = cleanedStr.replaceAll(/"(params)/g, 'params');

      try {
        // 使用Function解析，这是最安全的方式
        // eslint-disable-next-line no-new-func
        const fn = new Function(`return ${cleanedStr}`);
        return fn();
      } catch {
        try {
          // eslint-disable-next-line no-eval
          return eval(`(${cleanedStr})`);
        } catch {
          return null;
        }
      }
    } else {
      // 对于普通JSON，使用原有逻辑
      cleanedStr = cleanedStr
        .replaceAll("'", '"')
        .replaceAll(/(\w+)\s*:/g, '"$1":');
      return JSON.parse(cleanedStr);
    }
  } catch {
    return null;
  }
}

// 核心渲染逻辑（保留原始解析流程）
function renderChart() {
  if (!refEle.value) return;

  try {
    // 解析JSON配置（保留原有逻辑）
    const cleanedStr = parseEChartsOption(props.code);

    // 初始化/更新图表
    if (!myChart && cleanedStr) {
      loading.value = false;
      myChart = echarts.init(refEle.value, props.theme);
      myChart.setOption(cleanedStr);
    }
  } catch (error) {
    console.error('图表配置解析失败:', error);
  }
}

// 窗口resize处理
function handleResize() {
  myChart?.resize();
}

// 销毁逻辑
function destroyChart() {
  if (myChart) {
    myChart.dispose(); // 释放ECharts实例
    myChart = null;
  }
  window.removeEventListener('resize', handleResize);
}

// 初始化渲染
onMounted(() => {
  renderChart();
  window.addEventListener('resize', handleResize); // 添加resize监听
});

// 监听code变化自动更新（关键优化）
watch(
  () => props.code,
  () => {
    renderChart(); // 配置变化时重新渲染
  },
);

// 卸载时清理资源
onUnmounted(() => {
  destroyChart();
});
</script>

<template>
  <div id="echarts" class="relative w-full min-w-28" v-loading="loading">
    <div
      ref="refEle"
      :style="{
        height: height || '400px', // 可选高度，默认400px
        width: width || '95%', // 可选宽度，默认100%
      }"
    ></div>
  </div>
</template>
