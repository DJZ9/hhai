<script setup lang="ts">
import { ref } from 'vue';

import { ElAlert, ElButton, ElMessage, ElMessageBox, ElTable, ElTableColumn, ElTag } from 'element-plus';

import { api } from '#/api/request';

interface CatalogItem {
  key: string;
  title: string;
  description?: string;
  protocol?: string;
  running?: boolean;
}

const emit = defineEmits(['imported']);

const loading = ref(false);
const importingKey = ref('');
const items = ref<CatalogItem[]>([]);

async function loadCatalog() {
  loading.value = true;
  try {
    const res: any = await api.get('/api/v1/mcp/catalog');
    items.value = res?.errorCode === 0 ? res.data || [] : [];
  } finally {
    loading.value = false;
  }
}

async function importItem(item: CatalogItem) {
  try {
    await ElMessageBox.confirm(
      '安装后使用共享服务实例和共享认证信息。任意用户停止服务都会影响所有使用者，请谨慎操作。',
      '安装共享 MCP',
      {
        confirmButtonText: '确认安装',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );
  } catch {
    return;
  }
  importingKey.value = item.key;
  try {
    const res: any = await api.post('/api/v1/mcp/importFromCatalog', {
      catalogKey: item.key,
      title: item.title,
      description: item.description,
    });
    if (res?.errorCode === 0) {
      ElMessage.success('安装成功，当前为停用状态，请确认后手动启动');
      emit('imported');
    }
  } finally {
    importingKey.value = '';
  }
}

defineExpose({ loadCatalog });
</script>

<template>
  <div class="catalog-panel">
    <ElAlert
      title="目录条目由管理员在服务器的 MCP 配置目录中维护；安装后使用共享服务实例和共享认证信息。"
      type="warning"
      :closable="false"
      show-icon
    />
    <ElTable v-loading="loading" :data="items" border class="mt-3">
      <ElTableColumn prop="title" label="名称" min-width="160" />
      <ElTableColumn prop="description" label="描述" min-width="220" />
      <ElTableColumn prop="protocol" label="协议" width="110">
        <template #default="{ row }">
          <ElTag size="small">{{ row.protocol || 'unknown' }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="运行状态" width="110">
        <template #default="{ row }">
          <ElTag :type="row.running ? 'success' : 'info'" size="small">
            {{ row.running ? '运行中' : '未运行' }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <ElButton
            type="primary"
            link
            :loading="importingKey === row.key"
            @click="importItem(row)"
          >
            安装
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
  </div>
</template>

<style scoped>
.catalog-panel {
  min-height: 260px;
}

.mt-3 {
  margin-top: 12px;
}
</style>
