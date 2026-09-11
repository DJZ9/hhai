<script setup lang="ts">
import { ref } from 'vue';

import { Delete, Refresh } from '@element-plus/icons-vue';
import {
  ElButton,
  ElDialog,
  ElMessage,
  ElMessageBox,
  ElPagination,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTooltip,
} from 'element-plus';

import { api } from '#/api/request';
import { $t } from '#/locales';

defineExpose({
  openDialog,
});

const dialogVisible = ref(false);
const recordList = ref<any[]>([]);
const pageNumber = ref(1);
const pageSize = ref(10);
const totalCount = ref(0);
const loading = ref(false);
const selectedRows = ref<any[]>([]);
const deleteLoading = ref(false);

function openDialog() {
  dialogVisible.value = true;
  fetchRecords();
}

function closeDialog() {
  dialogVisible.value = false;
  selectedRows.value = [];
}

async function fetchRecords() {
  loading.value = true;
  try {
    const res = await api.get('/api/v1/datacenterTableImportRecord/page', {
      params: {
        pageNumber: pageNumber.value,
        pageSize: pageSize.value,
      },
    });
    if (res.errorCode === 0) {
      recordList.value = res.data?.records || [];
      totalCount.value = res.data?.totalRow || 0;
      // 清空选中状态
      selectedRows.value = [];
    }
  } finally {
    loading.value = false;
  }
}

function handlePageChange(page: number) {
  pageNumber.value = page;
  fetchRecords();
}

function handleSizeChange(size: number) {
  pageSize.value = size;
  pageNumber.value = 1;
  fetchRecords();
}

// 表格多选
function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows;
}

// 批量删除
async function handleDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning($t('message.selectAtLeastOne') || '请至少选择一条记录');
    return;
  }

  try {
    await ElMessageBox.confirm(
      $t('message.deleteAlert') ||
        `确定删除选中的 ${selectedRows.value.length} 条记录吗？`,
      $t('message.noticeTitle') || '提示',
      {
        confirmButtonText: $t('message.ok') || '确定',
        cancelButtonText: $t('message.cancel') || '取消',
        type: 'warning',
      },
    );

    deleteLoading.value = true;
    const recordIds = selectedRows.value.map((row) => row.id);
    const res = await api.post(
      '/api/v1/datacenterTableImportRecord/removeBatch',
      { ids: recordIds },
    );

    if (res.errorCode === 0) {
      ElMessage.success($t('message.deleteOkMessage') || '删除成功');
      fetchRecords();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch {
    // 用户取消删除
  } finally {
    deleteLoading.value = false;
  }
}

// 获取状态标签类型
function getStatusType(
  status: number,
): any{
  switch (status) {
    case 0: {
      return 'warning';
    } // 处理中
    case 1: {
      return 'success';
    } // 成功
    case 2: {
      return 'danger';
    } // 失败
    default: {
      return 'info';
    }
  }
}

// 获取状态文本
function getStatusText(status: number): string {
  switch (status) {
    case 0: {
      return $t('datacenterTable.importStatus.processing') || '处理中';
    }
    case 1: {
      return $t('datacenterTable.importStatus.success') || '成功';
    }
    case 2: {
      return $t('datacenterTable.importStatus.failed') || '失败';
    }
    default: {
      return '未知';
    }
  }
}
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    draggable
    :title="$t('datacenterTable.importRecord.title') || '导入记录'"
    :before-close="closeDialog"
    width="800px"
  >
    <div class="flex flex-col gap-4">
      <!-- 操作栏 -->
      <div class="flex items-center justify-between">
        <ElButton
          v-if="selectedRows.length > 0"
          type="danger"
          :icon="Delete"
          :loading="deleteLoading"
          @click="handleDelete"
        >
          {{ $t('button.delete') || '删除' }} ({{ selectedRows.length }})
        </ElButton>
        <ElButton
          type="primary"
          link
          :icon="Refresh"
          :loading="loading"
          @click="fetchRecords"
        >
          {{ $t('button.refresh') || '刷新' }}
        </ElButton>
      </div>

      <!-- 记录列表 -->
      <ElTable
        :data="recordList"
        border
        v-loading="loading"
        :empty-text="$t('common.noDataAvailable')"
        @selection-change="handleSelectionChange"
      >
        <ElTableColumn type="selection" width="50" />
        <ElTableColumn
          prop="fileName"
          :label="$t('datacenterTable.importRecord.fileName') || '文件名'"
          min-width="150"
          show-overflow-tooltip
        />
        <ElTableColumn
          prop="tableName"
          :label="$t('datacenterTable.tableName') || '表名'"
          min-width="120"
          show-overflow-tooltip
        />
        <ElTableColumn
          :label="$t('datacenterTable.importRecord.status') || '状态'"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <ElTooltip
              v-if="row.status === 2 && row.errorInfo"
              :content="row.errorInfo"
              placement="top"
              :show-after="300"
            >
              <ElTag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </ElTag>
            </ElTooltip>
            <ElTag v-else :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn
          :label="$t('datacenterTable.importRecord.progress') || '进度'"
          width="140"
          align="center"
        >
          <template #default="{ row }">
            <span v-if="row.status === 0" class="text-sm">
              {{ row.successCount + row.errorCount }}/{{ row.totalCount }}
            </span>
            <span v-else-if="row.status === 1" class="text-sm text-green-600">
              {{ row.successCount }}/{{ row.totalCount }}
            </span>
            <span v-else-if="row.status === 2" class="text-sm text-red-600">
              {{ row.successCount }}/{{ row.totalCount }}
            </span>
          </template>
        </ElTableColumn>
        <ElTableColumn
          prop="created"
          :label="$t('datacenterTable.created') || '创建时间'"
          width="160"
        />
      </ElTable>

      <!-- 分页 -->
      <ElPagination
        v-if="totalCount > 0"
        v-model:current-page="pageNumber"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="totalCount"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <template #footer>
      <ElButton @click="closeDialog">
        {{ $t('button.close') || '关闭' }}
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped></style>
