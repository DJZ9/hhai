<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { Infographic } from '@antv/infographic';
import { Download } from '@element-plus/icons-vue';
import { ElButton } from 'element-plus';

const props = defineProps<{
  code: string;
  height?: string;
  width?: string;
}>();

const containerRef = ref(null);
const loading = ref(true);
let infographic: Infographic | null = null;

// 核心渲染逻辑（保留原始解析流程）
function renderInfographic() {
  if (!containerRef.value) return;

  try {
    if (!infographic) {
      infographic = new Infographic({
        container: containerRef.value,
        width: props.width || '100%',
        height: props.height || '100%',
      });
    }

    if (infographic) {
      infographic.render(props.code);
    }
    if (props.code.includes('data\n') && loading.value) {
      loading.value = false;
    }
  } catch (error) {
    console.error('图表配置解析失败:', error);
  }
}

async function downloadImage(format: 'png' | 'svg') {
  if (infographic && infographic.rendered) {
    const url = await infographic.toDataURL({ type: format });
    const a = document.createElement('a');
    a.href = url;
    a.download = `aiflowy-infographic-${Date.now()}.${format}`;
    document.body.append(a);
    a.click();
    a.remove();
  }
}

// 初始化渲染
onMounted(() => {
  renderInfographic();
});

// 监听code变化自动更新（关键优化）
watch(
  () => props.code,
  () => {
    renderInfographic(); // 配置变化时重新渲染
  },
);

// 卸载时清理资源
onBeforeUnmount(() => {
  if (infographic) {
    infographic.destroy();
  }
});
</script>

<template>
  <div class="markdown-mermaid mt-3" v-loading="loading">
    <div class="toolbar-container">
      <div class="flex h-10 items-center justify-end bg-[#ebecef] px-3">
        <ElButton link :icon="Download" @click="downloadImage('png')">
          PNG
        </ElButton>
        <ElButton link :icon="Download" @click="downloadImage('svg')">
          SVG
        </ElButton>
      </div>
    </div>
    <div ref="containerRef" class="mermaid-content"></div>
  </div>
</template>
