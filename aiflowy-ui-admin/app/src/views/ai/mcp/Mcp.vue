<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import { computed, markRaw, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Delete, MoreFilled, Plus, Refresh, ShoppingCart } from '@element-plus/icons-vue';
import {
  ElButton,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElMessage,
  ElMessageBox,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTooltip,
} from 'element-plus';

import { api } from '#/api/request';
import HeaderSearch from '#/components/headerSearch/HeaderSearch.vue';
import PageData from '#/components/page/PageData.vue';
import { $t } from '#/locales';

import McpModal from './McpModal.vue';

const router = useRouter();

const formRef = ref<FormInstance>();
const pageDataRef = ref();
const saveDialog = ref();
function reset(formEl: FormInstance | undefined) {
  formEl?.resetFields();
  pageDataRef.value.setQuery({});
}
function showDialog(row: any) {
  saveDialog.value.openDialog({ ...row });
}

function isCatalogReference(row: any) {
  try {
    const config = JSON.parse(row?.configJson || '{}');
    return config?.aiflowyMcpSource?.type === 'catalog';
  } catch {
    return false;
  }
}

function handleStatusChange(row: any, value: boolean) {
  if (!isCatalogReference(row)) {
    handleUpdate(row, false);
    return;
  }
  const actionText = value ? '启动' : '停止';
  const content = value
    ? '该操作会为所有使用者启动共享 MCP 服务，是否继续？'
    : '该操作会立即停止共享 MCP 服务，并影响所有正在使用的用户，是否继续？';
  ElMessageBox.confirm(content, `共享 MCP ${actionText}`, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      handleUpdate(row, false);
    })
    .catch(() => {
      row.status = !value;
    });
}
function remove(row: any) {
  ElMessageBox.confirm($t('message.deleteAlert'), $t('message.noticeTitle'), {
    confirmButtonText: $t('message.ok'),
    cancelButtonText: $t('message.cancel'),
    type: 'warning',
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        instance.confirmButtonLoading = true;
        api
          .post('/api/v1/mcp/remove', { id: row.id })
          .then((res) => {
            instance.confirmButtonLoading = false;
            if (res.errorCode === 0) {
              ElMessage.success(res.message);
              reset(formRef.value);
              done();
            }
          })
          .catch(() => {
            instance.confirmButtonLoading = false;
          });
      } else {
        done();
      }
    },
  }).catch(() => {});
}
const handleUpdate = (row: any, isRefresh: boolean) => {
  if (isRefresh) {
    refreshLoadingMap.value[row.id] = true;
  } else {
    loadingMap.value[row.id] = true;
  }
  api.post('/api/v1/mcp/update', { ...row }).then((res) => {
    if (isRefresh) {
      refreshLoadingMap.value[row.id] = false;
    } else {
      loadingMap.value[row.id] = false;
    }
    if (res.errorCode === 0) {
      if (row.status) {
        ElMessage.success($t('mcp.message.startupSuccessful'));
      } else {
        ElMessage.success($t('mcp.message.stopSuccessful'));
      }
    }
    pageDataRef.value.setQuery({});
  });
};
// 是否显示 MCP 市场入口，由系统设置 mcp_market_enabled 控制，默认不显示
const marketVisible = ref(false);

onMounted(() => {
  api.get('/api/v1/sysOption/list?keys=mcp_market_enabled').then((res) => {
    if (res.errorCode === 0) {
      marketVisible.value = res.data?.mcp_market_enabled === 'true';
    }
  });
});

const headerButtons = computed(() => {
  const buttons = [
    {
      key: 'market',
      type: 'primary',
      text: $t('mcp.mcpMarket'),
      icon: markRaw(ShoppingCart),
      data: { action: 'market' },
    },
    {
      key: 'create',
      type: 'primary',
      text: $t('button.add'),
      icon: markRaw(Plus),
      data: { action: 'create' },
    },
  ];
  return marketVisible.value
    ? buttons
    : buttons.filter((item) => item.key !== 'market');
});
const handleSearch = (params: string) => {
  pageDataRef.value.setQuery({ packageName: params, isQueryOr: true });
};
const handleHeaderButtonClick = (button: any) => {
  if (button.key === 'create') {
    showDialog({});
  } else if (button.key === 'market') {
    router.push({ name: 'IframeMcpMarket' });
  }
};
const loadingMap = ref<Record<number | string, boolean>>({});
const refreshLoadingMap = ref<Record<number | string, boolean>>({});
</script>

<template>
  <div class="flex h-full flex-col gap-6 p-6">
    <McpModal ref="saveDialog" @reload="reset" />
    <HeaderSearch
      :buttons="headerButtons"
      @search="handleSearch"
      @button-click="handleHeaderButtonClick"
    />
    <div class="bg-background border-border flex-1 rounded-lg border p-5">
      <PageData ref="pageDataRef" page-url="/api/v1/mcp/page" :page-size="10">
        <template #default="{ pageList }">
          <ElTable :data="pageList" border>
            <ElTableColumn prop="title" :label="$t('mcp.title')">
              <template #default="{ row }">
                <ElTooltip
                  :content="
                    row.clientOnline
                      ? $t('mcp.labels.clientOnline')
                      : $t('mcp.labels.clientOffline')
                  "
                  placement="top"
                >
                  <span
                    class="mr-2 inline-block h-2 w-2 rounded-full"
                    :class="row.clientOnline ? 'bg-green-500' : 'bg-red-500'"
                  ></span>
                </ElTooltip>
                {{ row.title }}
              </template>
            </ElTableColumn>
            <ElTableColumn prop="description" :label="$t('mcp.description')">
              <template #default="{ row }">
                {{ row.description }}
              </template>
            </ElTableColumn>
            <ElTableColumn prop="created" :label="$t('mcp.created')">
              <template #default="{ row }">
                {{ row.created }}
              </template>
            </ElTableColumn>
            <ElTableColumn prop="status" :label="$t('mcp.status')">
              <template #default="{ row }">
                <ElSwitch
                  v-model="row.status"
                  @change="(value) =>
                    handleStatusChange(row, value === true)
                  "
                  :loading="loadingMap[row.id]"
                  :disabled="loadingMap[row.id]"
                />
              </template>
            </ElTableColumn>
            <ElTableColumn
              :label="$t('common.handle')"
              width="150"
              align="right"
            >
              <template #default="{ row }">
                <div class="flex items-center gap-3">
                  <div v-access:code="'/api/v1/mcp/save'">
                    <ElButton
                      @click="handleUpdate({ ...row, status: true }, true)"
                      type="primary"
                      link
                      :icon="Refresh"
                      :loading="refreshLoadingMap[row.id]"
                    >
                      {{ $t('重启') }}
                    </ElButton>
                  </div>
                  <div v-access:code="'/api/v1/mcp/save'">
                    <ElButton type="primary" link @click="showDialog(row)">
                      {{ $t('button.edit') }}
                    </ElButton>
                  </div>

                  <ElDropdown>
                    <ElButton link :icon="MoreFilled" />
                    <template #dropdown>
                      <ElDropdownMenu>
                        <div v-access:code="'/api/v1/mcp/remove'">
                          <ElDropdownItem @click="remove(row)">
                            <ElButton type="danger" :icon="Delete" link>
                              {{ $t('button.delete') }}
                            </ElButton>
                          </ElDropdownItem>
                        </div>
                      </ElDropdownMenu>
                    </template>
                  </ElDropdown>
                </div>
              </template>
            </ElTableColumn>
          </ElTable>
        </template>
      </PageData>
    </div>
  </div>
</template>

<style scoped></style>
