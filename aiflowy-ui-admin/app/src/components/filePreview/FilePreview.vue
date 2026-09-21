<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { createViewer } from 'jit-viewer';
import 'jit-viewer/style.css';

defineOptions({ name: 'FilePreview' });

interface Props {
  /** 文件源：URL / File / Blob / ArrayBuffer */
  url?: File | Blob | ArrayBuffer | string;
  /** 文件名（带后缀，必填，用于类型推断） */
  filename?: string;
  /** 容器宽，默认 '100%' */
  width?: string | number;
  /** 容器高，默认 '70vh' */
  height?: string | number;
}

const props = withDefaults(defineProps<Props>(), {
  url: undefined,
  filename: undefined,
  theme: 'light',
  toolbar: true,
  width: '100%',
  height: '70vh',
});

const containerRef = ref<HTMLElement | null>(null);
let viewer: any = null;

async function mount() {
  if (!containerRef.value) return;
  if (
    props.url === undefined ||
    props.url === null ||
    props.url === '' ||
    !props.filename
  ) {
    return;
  }

  destroy();

  try {
    viewer = createViewer({
      target: containerRef.value,
      file: props.url,
      filename: props.filename,
      toolbar: true,
      width: '100%',
      height: '100%',
      onReady: () => console.log('[FilePreview] ready'),
      onLoad: () => console.log('[FilePreview] loaded'),
      onError: (err: Error) => console.error('[FilePreview] error:', err),
    });
    viewer.mount(containerRef.value);
  } catch (err) {
    console.error('[FilePreview] mount error:', err);
  }
}

function destroy() {
  if (viewer) {
    try {
      viewer.destroy();
    } catch { }
    viewer = null;
    if (containerRef.value) containerRef.value.innerHTML = '';
  }
}

watch(
  () => [props.url, props.filename],
  () => mount(),
);

onMounted(mount);
onBeforeUnmount(destroy);

const containerStyle = {
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  height:
    typeof props.height === 'number' ? `${props.height}px` : props.height,
};
</script>

<template>
  <div class="file-preview-wrapper" :style="containerStyle">
    <div ref="containerRef" class="file-preview-container" />
  </div>
</template>

<style scoped>
.file-preview-wrapper {
  position: relative;
  overflow: hidden;
}

.file-preview-container {
  width: 100%;
  height: 100%;
}
</style>
