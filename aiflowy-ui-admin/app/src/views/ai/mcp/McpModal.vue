<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import { computed, onMounted, ref } from 'vue';

import {
  ElAlert,
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
} from 'element-plus';

import { api } from '#/api/request';
import { $t } from '#/locales';
import McpCatalogTab from './McpCatalogTab.vue';

interface PropValue {
  type?: string;
  description?: string;
}

interface McpTool {
  name: string;
  description: string;
  status: boolean;
  inputSchema?: {
    properties: Record<string, PropValue>;
    required?: string[];
  };
}

interface McpEntity {
  id?: string;
  title: string;
  description: string;
  configJson: string;
  deptId: string;
  status: boolean;
  tools: McpTool[];
}

const emit = defineEmits(['reload']);

onMounted(() => {});
defineExpose({
  openDialog,
});

const saveForm = ref<FormInstance>();
const dialogVisible = ref(false);
const isAdd = ref(true);
const btnLoading = ref(false);
const activeName = ref('manual');
const catalogTabRef = ref<InstanceType<typeof McpCatalogTab>>();

const defaultEntity: McpEntity = {
  title: '',
  description: '',
  configJson: '',
  deptId: '',
  status: false,
  tools: [],
};
const entity = ref<McpEntity>({ ...defaultEntity });

const rules = ref({
  title: [
    {
      required: true,
      message: $t('message.required'),
      trigger: 'blur',
    },
  ],
  configJson: [
    {
      required: true,
      message: $t('message.required'),
      trigger: 'blur',
    },
  ],
});

function openDialog(row: Partial<McpEntity> = {}) {
  isAdd.value = !row.id;
  entity.value = { ...defaultEntity, ...row };
  activeName.value = isAdd.value ? 'manual' : 'config';
  if (!isAdd.value) {
    getMcpTools(row);
  } else {
    catalogTabRef.value?.loadCatalog();
  }
  dialogVisible.value = true;
}

function getMcpTools(row: Partial<McpEntity>) {
  api.post('api/v1/mcp/getMcpTools', { id: row.id }).then((res) => {
    if (res.errorCode === 0) {
      entity.value.tools = res.data.tools;
    }
  });
}
function save() {
  if (isAdd.value && activeName.value === 'catalog') {
    return;
  }
  saveForm.value?.validate((valid) => {
    if (valid) {
      btnLoading.value = true;
      api
        .post(
          isAdd.value ? 'api/v1/mcp/save' : 'api/v1/mcp/update',
          entity.value,
        )
        .then((res) => {
          btnLoading.value = false;
          if (res.errorCode === 0) {
            if (isAdd.value) {
              ElMessage.success($t('message.saveOkMessage'));
            } else {
              ElMessage.success($t('message.updateOkMessage'));
            }
            emit('reload');
            closeDialog();
          }
        })
        .catch(() => {
          btnLoading.value = false;
        });
    }
  });
}

function closeDialog() {
  saveForm.value?.resetFields();
  isAdd.value = true;
  activeName.value = 'manual';
  entity.value = { ...defaultEntity };
  dialogVisible.value = false;
}

function handleCatalogImported() {
  emit('reload');
  closeDialog();
}

function handleTabChange(name: string | number) {
  if (name === 'catalog') {
    catalogTabRef.value?.loadCatalog();
  }
}

const isCatalogReference = computed(() => {
  try {
    const config = JSON.parse(entity.value.configJson || '{}');
    return config?.aiflowyMcpSource?.type === 'catalog';
  } catch {
    return false;
  }
});

const catalogKeyText = computed(() => {
  try {
    const config = JSON.parse(entity.value.configJson || '{}');
    return config?.aiflowyMcpSource?.key || '';
  } catch {
    return '';
  }
});

const jsonPlaceholder = ref(`{
  "mcpServers": {
    "remote-mcp": {
      "type": "http-sse",
      "url": "https://mcp.example.com/sse",
      "headers": {
        "Authorization": "Bearer <token>"
      }
    }
  }
}`);
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    draggable
    :title="isAdd ? $t('button.add') : $t('button.edit')"
    :before-close="closeDialog"
    :close-on-click-modal="false"
  >
    <ElTabs
      v-if="isAdd"
      v-model="activeName"
      class="demo-tabs"
      @tab-change="handleTabChange"
    >
      <ElTabPane label="手动添加 HTTP MCP" name="manual">
        <ElAlert
          title="手动添加仅支持 HTTP、SSE、Streamable HTTP 等远程 MCP。npx、python、docker 等本地命令型 MCP 不能在前端填写，请联系管理员在服务器的 MCP 配置目录中添加，再从“从后台目录安装”页签导入。"
          type="info"
          :closable="false"
          show-icon
        />
        <ElForm
          class="mt-3"
          label-width="120px"
          ref="saveForm"
          :model="entity"
          status-icon
          :rules="rules"
        >
          <ElFormItem prop="title" :label="$t('mcp.title')">
            <ElInput v-model.trim="entity.title" />
          </ElFormItem>
          <ElFormItem prop="description" :label="$t('mcp.description')">
            <ElInput v-model.trim="entity.description" />
          </ElFormItem>
          <ElFormItem prop="configJson" :label="$t('mcp.configJson')">
            <ElInput
              type="textarea"
              :rows="15"
              v-model.trim="entity.configJson"
              :placeholder="$t('mcp.example') + jsonPlaceholder"
            />
          </ElFormItem>
          <ElFormItem prop="status" :label="$t('mcp.status')">
            <ElSwitch v-model="entity.status" />
          </ElFormItem>
        </ElForm>
      </ElTabPane>
      <ElTabPane label="从后台目录安装" name="catalog">
        <McpCatalogTab ref="catalogTabRef" @imported="handleCatalogImported" />
      </ElTabPane>
    </ElTabs>
    <ElTabs v-else v-model="activeName" class="demo-tabs">
      <ElTabPane :label="$t('mcp.modal.config')" name="config">
        <ElAlert
          v-if="isCatalogReference"
          title="管理员共享 MCP：配置由服务器 MCP 配置目录统一维护，所有安装用户共享服务实例和认证信息。"
          type="warning"
          :closable="false"
          show-icon
        />
        <ElForm
          class="mt-3"
          label-width="120px"
          ref="saveForm"
          :model="entity"
          status-icon
          :rules="rules"
        >
          <ElFormItem prop="title" :label="$t('mcp.title')">
            <ElInput v-model.trim="entity.title" />
          </ElFormItem>
          <ElFormItem prop="description" :label="$t('mcp.description')">
            <ElInput v-model.trim="entity.description" />
          </ElFormItem>
          <ElFormItem v-if="isCatalogReference" label="目录 KEY">
            <ElInput :model-value="catalogKeyText" readonly />
          </ElFormItem>
          <ElFormItem prop="configJson" :label="$t('mcp.configJson')">
            <ElInput
              type="textarea"
              :rows="15"
              v-model.trim="entity.configJson"
              :readonly="isCatalogReference"
              :placeholder="$t('mcp.example') + jsonPlaceholder"
            />
          </ElFormItem>
          <ElFormItem prop="status" :label="$t('mcp.status')">
            <ElSwitch v-model="entity.status" />
          </ElFormItem>
        </ElForm>
      </ElTabPane>
      <ElTabPane :label="$t('mcp.modal.tool')" name="tool">
          <ElTable
            :data="entity.tools"
            border
            :preserve-expanded-content="true"
          >
            <ElTableColumn type="expand">
              <template #default="scope">
                <!-- 解构获取properties和required，同时做空值保护 -->
                <div
                  v-if="scope.row?.inputSchema?.properties"
                  class="params-list"
                >
                  <div
                    v-for="([propKey, propValue], index) in Object.entries(
                      scope.row.inputSchema.properties,
                    )"
                    :key="index"
                    class="params-content-container"
                  >
                    <div class="params-left-title-container">
                      <div class="content-title">
                        {{ propKey }}
                        <span
                          v-if="
                            scope.row.inputSchema.required &&
                            scope.row.inputSchema.required.includes(propKey)
                          "
                          class="required-mark"
                        >
                          *
                        </span>
                      </div>
                    </div>
                    <div class="params-desc-container">
                      <div class="content-title">
                        {{ (propValue as PropValue).type || '未知类型' }}
                      </div>
                      <div class="content-desc">
                        {{ (propValue as PropValue).description || '无描述' }}
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="params-name">暂无属性配置</div>
              </template>
            </ElTableColumn>

            <ElTableColumn :label="$t('mcp.modal.table.availableTools')">
              <template #default="{ row }">
                <div class="content-left">
                  <span class="content-title">{{ row.name }}</span>
                  <span class="content-desc">{{ row.description }}</span>
                </div>
              </template>
            </ElTableColumn>
            <!--          <ElTableColumn :label="$t('mcp.status')">
              <template #default="{ row }">
                <ElSwitch v-model="row.status" />
              </template>
            </ElTableColumn>-->
          </ElTable>
        </ElTabPane>
    </ElTabs>
    <template #footer>
      <ElButton @click="closeDialog">
        {{ $t('button.cancel') }}
      </ElButton>
      <ElButton
        v-if="!isAdd || activeName === 'manual'"
        type="primary"
        @click="save"
        :loading="btnLoading"
        :disabled="btnLoading"
      >
        {{ $t('button.save') }}
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.mt-3 {
  margin-top: 12px;
}
.content-left {
  display: flex;
  flex-direction: column;
}
.content-title {
  font-weight: 500;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.85);
  line-height: 24px;
  text-align: left;
  font-style: normal;
  text-transform: none;
}
.content-desc {
  font-weight: 400;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 22px;
  text-align: left;
  font-style: normal;
  text-transform: none;
}
.params-name {
  flex: 1;
  background-color: #fafafa;
  display: flex;
  align-items: center;
  border: 1px solid #e6e9ee;
  border-radius: 8px;
  padding: 8px;
}
.params-content-container {
  display: flex;
  flex-direction: row;
  gap: 8px;
  flex: 1;
  border-radius: 8px;
  padding: 8px;
}
.params-desc-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  border-radius: 8px;
  border: 1px solid #e6e9ee;
  padding: 8px;
}
.params-list {
  display: flex;
  flex-direction: column;
}

.params-left-title-container {
  display: flex;
  flex-direction: row;
  background-color: #fafafa;
  gap: 8px;
  flex: 1;
  border: 1px solid #e6e9ee;
  border-radius: 8px;
  padding: 8px;
  align-items: center;
}
.required-mark {
  color: #f56c6c;
  margin-left: 2px;
  font-size: 14px;
}
</style>
