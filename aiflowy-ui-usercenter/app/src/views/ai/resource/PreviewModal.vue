<script setup lang="ts">
import { computed, ref } from 'vue';

import { ElDialog } from 'element-plus';

import FilePreview from '#/components/filePreview/FilePreview.vue';

defineExpose({
  openDialog,
});
const dialogVisible = ref(false);
const data = ref<any>();
function openDialog(row: any) {
  data.value = row;
  dialogVisible.value = true;
}
function closeDialog() {
  dialogVisible.value = false;
}

// 构造带后缀的文件名给 jit-viewer 推断格式
const filename = computed(() => {
  if (!data.value) return '';
  const name = data.value.resourceName || '';
  const suffix = data.value.suffix || '';
  // 文件名已带后缀则直接用
  if (suffix && name.endsWith(`.${suffix}`)) return name;
  return suffix ? `${name}.${suffix}` : name;
});
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    draggable
    :title="$t('message.preview')"
    :before-close="closeDialog"
    :destroy-on-close="true"
  >
    <FilePreview
      v-if="data"
      :url="data.resourceUrl"
      :filename="filename"
      height="60vh"
    />
  </ElDialog>
</template>

<style scoped></style>
