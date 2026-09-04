<script setup>
import { onMounted, ref } from 'vue';

import { $t } from '@aiflowy/locales';

import { Delete, Edit, Minus, Plus } from '@element-plus/icons-vue';
import {
  ElButton,
  ElCollapse,
  ElCollapseItem,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElMessage,
  ElMessageBox,
} from 'element-plus';

import { getLlmProviderList } from '#/api/ai/llm.js';
import { api } from '#/api/request.js';
import ManageIcon from '#/components/icons/ManageIcon.vue';
import PageSide from '#/components/page/PageSide.vue';
import AddModelModal from '#/views/ai/model/AddModelModal.vue';
import AddModelProviderModal from '#/views/ai/model/AddModelProviderModal.vue';
import ManageModelModal from '#/views/ai/model/ManageModelModal.vue';
import {
  getIconByValue,
  isSvgString,
} from '#/views/ai/model/modelUtils/defaultIcon.ts';
import { modelTypes } from '#/views/ai/model/modelUtils/modelTypes.ts';
import ModelVerifyConfig from '#/views/ai/model/ModelVerifyConfig.vue';
import ModelViewItemOperation from '#/views/ai/model/ModelViewItemOperation.vue';

const brandListData = ref([]);
const defaultSelectProviderId = ref('');
const defaultIcon = ref('');
const modelListData = ref([]);
const providerLoading = ref(true);
onMounted(() => {
  getLlmProviderListData();
});

const checkAndFillDefaultIcon = (list) => {
  if (!list || list.length === 0) return;
  list.forEach((item) => {
    if (!item.icon) {
      item.icon = getIconByValue(item.providerType);
    }
  });
};
// 统计组内 publishStatus=1 的模型数量
const countPublishedModels = (llmList) => {
  if (!Array.isArray(llmList)) return 0;
  return llmList.filter((item) => Number(item?.publishStatus) === 1).length;
};
// 正在批量发布的分组key集合，用于防止重复提交
const batchPublishingKeys = ref(new Set());
// 批量发布当前分组的所有模型
const handleBatchPublish = (group, modelType) => {
  const key = `${modelType}:${group.groupName}`;
  if (batchPublishingKeys.value.has(key)) return;
  const ids = (group.llmList || []).map((item) => item.id);
  if (!ids.length) {
    ElMessage.warning($t('llm.noModelInGroup'));
    return;
  }
  batchPublishingKeys.value = new Set(batchPublishingKeys.value).add(key);
  api
    .post('/api/v1/model/batchPublish', null, {
      params: {
        ids: ids.join(','),
        modelType,
      },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        const data = res.data || {};
        const failCount = (data.fail || []).length;
        if (data.allSuccess) {
          ElMessage.success(data.message || $t('llm.publishAllSuccess'));
        } else {
          ElMessage.warning(
            `${data.message || $t('llm.publishPartFail')}（失败 ${failCount} 个）`,
          );
        }
        getLlmDetailList(defaultSelectProviderId.value);
      }
    })
    .finally(() => {
      const next = new Set(batchPublishingKeys.value);
      next.delete(key);
      batchPublishingKeys.value = next;
    });
};
const chatModelListData = ref([]);
const embeddingModelListData = ref([]);
const rerankModelListData = ref([]);
const ocrModelListData = ref([]);

const getLlmDetailList = (providerId) => {
  api
    .get(`/api/v1/model/getList?providerId=${providerId}&withUsed=true`, {})
    .then((res) => {
      if (res.errorCode === 0) {
        modelListData.value = res.data;
        // 初始化模型分组数据（按modelType分类，存储groupName和对应的llm列表）
        chatModelListData.value = [];
        embeddingModelListData.value = [];

        // 处理chatModel数据
        const chatModelMap = res.data.chatModel || {};
        // 将chatModel的key-value（groupName-llmList）转为数组，方便v-for遍历
        chatModelListData.value = Object.entries(chatModelMap).map(
          ([groupName, llmList]) => ({
            groupName,
            llmList,
          }),
        );
        // 处理embeddingModel数据
        const embeddingModelMap = res.data.embeddingModel || {};
        embeddingModelListData.value = Object.entries(embeddingModelMap).map(
          ([groupName, llmList]) => ({
            groupName,
            llmList,
          }),
        );

        // 处理rerankModel数据
        const rerankModelMap = res.data.rerankModel || {};
        rerankModelListData.value = Object.entries(rerankModelMap).map(
          ([groupName, llmList]) => ({
            groupName,
            llmList,
          }),
        );

        // 处理ocrModel数据
        const ocrModelMap = res.data.ocrModel || {};
        ocrModelListData.value = Object.entries(ocrModelMap).map(
          ([groupName, llmList]) => ({
            groupName,
            llmList,
          }),
        );
      }
    });
};

const getLlmProviderListData = () => {
  providerLoading.value = true;
  getLlmProviderList()
    .then((res) => {
      brandListData.value = res.data;
      checkAndFillDefaultIcon(brandListData.value);
      if (defaultSelectProviderId.value) {
        // 保持当前选中的供应商配置，只更新列表数据
        const currentProvider = res.data.find(
          (item) => item.id === defaultSelectProviderId.value,
        );
        if (currentProvider) {
          // 只更新列表中的基础信息，保留用户正在编辑的表单值
          const {
            apiKey,
            endpoint,
            chatPath,
            embedPath,
            rerankPath,
            providerWebsite,
            apiKeyObtainAddress,
          } = llmProviderForm.value;
          llmProviderForm.value = {
            ...currentProvider,
            // 如果表单中有值，保留表单值；否则使用服务器返回的值
            apiKey: apiKey || currentProvider.apiKey,
            endpoint: endpoint || currentProvider.endpoint,
            chatPath: chatPath || currentProvider.chatPath,
            embedPath: embedPath || currentProvider.embedPath,
            rerankPath: rerankPath || currentProvider.rerankPath,
            providerWebsite: providerWebsite || currentProvider.providerWebsite,
            apiKeyObtainAddress:
              apiKeyObtainAddress || currentProvider.apiKeyObtainAddress,
          };
        }
      } else {
        defaultSelectProviderId.value = res.data[0].id;
        defaultIcon.value = res.data[0].icon;
        llmProviderForm.value = {
          ...res.data[0],
        };
      }
      getLlmDetailList(defaultSelectProviderId.value);
    })
    .finally(() => {
      providerLoading.value = false;
    });
};
const selectCategory = ref({
  providerName: '',
  provider: '',
});
const handleCategoryClick = (category) => {
  selectCategory.value.providerName = category.providerName;
  selectCategory.value.provider = category.provider;
  defaultSelectProviderId.value = category.id;
  defaultIcon.value = category.icon;
  llmProviderForm.value = {
    ...category,
  };
  getLlmDetailList(category.id);
};

// 添加模型供应商
const addLlmProviderRef = ref();
// 模型管理ref
const manageLlmRef = ref();
// 模型验证配置ref
const llmVerifyConfigRef = ref();
// 添加模型
const addLlmRef = ref();
const handleDeleteProvider = (row) => {
  ElMessageBox.confirm($t('message.deleteAlert'), $t('message.noticeTitle'), {
    confirmButtonText: $t('message.ok'),
    cancelButtonText: $t('message.cancel'),
    type: 'warning',
  }).then(() => {
    api
      .post('/api/v1/modelProvider/remove', {
        id: row.id,
      })
      .then((res) => {
        if (res.errorCode === 0) {
          ElMessage.success(res.message);
          getLlmProviderListData();
        }
      });
  });
};
const llmProviderForm = ref({});
const llmProviderFormRef = ref();
const isEdit = ref(false);
const dialogAddProviderVisible = ref(false);
const controlBtns = [
  {
    icon: Edit,
    label: $t('button.edit'),
    onClick(row) {
      isEdit.value = true;
      dialogAddProviderVisible.value = true;

      const tempRow = {
        ...row,
      };
      if (isSvgString(tempRow.icon)) {
        tempRow.icon = '';
      }
      addLlmProviderRef.value.openEditDialog(tempRow);
    },
  },
  {
    type: 'danger',
    icon: Delete,
    label: $t('button.delete'),
    onClick(row) {
      handleDeleteProvider(row);
    },
  },
];
const footerButton = {
  icon: Plus,
  label: $t('button.add'),
  onClick() {
    dialogAddProviderVisible.value = true;
    addLlmProviderRef.value.openAddDialog();
    isEdit.value = false;
  },
};
const handleAddLlm = (modelType) => {
  addLlmRef.value.openAddDialog(modelType);
};
const handleManageLlm = (clickModelType) => {
  manageLlmRef.value.openDialog(defaultSelectProviderId.value, clickModelType);
};
const handleDeleteLlm = (id) => {
  ElMessageBox.confirm($t('message.deleteAlert'), $t('message.noticeTitle'), {
    confirmButtonText: $t('message.ok'),
    cancelButtonText: $t('message.cancel'),
    type: 'warning',
  }).then(() => {
    api.post('/api/v1/model/remove', { id }).then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.deleteOkMessage'));
        getLlmDetailList(defaultSelectProviderId.value);
      }
    });
  });
};

const handleEditLlm = (id) => {
  api.get(`/api/v1/model/detail?id=${id}`).then((res) => {
    if (res.errorCode === 0) {
      addLlmRef.value.openEditDialog(res.data);
    }
  });
};

const handleGroupNameUpdateModel = (groupName) => {
  api
    .post('/api/v1/model/updateByEntity', {
      providerId: defaultSelectProviderId.value,
      groupName,
      withUsed: false,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        getLlmDetailList(defaultSelectProviderId.value);
      }
    });
};

// 输入框失去焦点时更新配置
const handleFormBlur = async () => {
  if (!defaultSelectProviderId.value) return;

  try {
    const res = await api.post('/api/v1/modelProvider/update', {
      id: defaultSelectProviderId.value,
      apiKey: llmProviderForm.value.apiKey,
      endpoint: llmProviderForm.value.endpoint,
      chatPath: llmProviderForm.value.chatPath,
      embedPath: llmProviderForm.value.embedPath,
      rerankPath: llmProviderForm.value.rerankPath,
    });

    if (res.errorCode === 0) {
      getLlmProviderList().then((res) => {
        brandListData.value = res.data;
        checkAndFillDefaultIcon(res.data);
        brandListData.value.forEach((item) => {
          if (item.id === defaultSelectProviderId.value) {
            llmProviderForm.value = { ...item };
          }
        });
      });
    } else {
      ElMessage.error(res.message || $t('message.updateFail'));
    }
  } catch (error) {
    ElMessage.error($t('message.networkError'));
    console.error('更新失败:', error);
  }
};
const handleTest = () => {
  llmVerifyConfigRef.value.openDialog(defaultSelectProviderId.value);
};

const handleUpdateLlm = (id) => {
  api.post('/api/v1/model/update', { id, withUsed: false }).then((res) => {
    if (res.errorCode === 0) {
      getLlmDetailList(defaultSelectProviderId.value);
    }
  });
};

const handlePublishChange = () => {
  getLlmDetailList(defaultSelectProviderId.value);
};

const isBatchPublishing = (modelType, groupName) => {
  return batchPublishingKeys.value.has(`${modelType}:${groupName}`);
};
</script>

<template>
  <div class="llm-container">
    <div>
      <PageSide
        :title="$t('llm.addProvider')"
        label-key="providerName"
        value-key="id"
        :menus="brandListData"
        :control-btns="controlBtns"
        :footer-button="footerButton"
        :loading="providerLoading"
        @change="handleCategoryClick"
        :default-selected="defaultSelectProviderId"
        :icon-size="21"
      />
    </div>
    <div class="llm-table-container">
      <div class="llm-form-container">
        <div class="title">{{ selectCategory.providerName }}</div>
        <div
          v-if="llmProviderForm.providerWebsite?.trim()"
          class="provider-link-row website-top-row"
        >
          <span class="provider-link-label">{{
            $t('llmProvider.providerWebsite')
          }}</span>
          <a
            :href="llmProviderForm.providerWebsite"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ llmProviderForm.providerWebsite }}
          </a>
        </div>
        <div
          v-if="llmProviderForm.apiKeyObtainAddress?.trim()"
          class="provider-link-row"
        >
          <span class="provider-link-label">{{
            $t('llmProvider.apiKeyObtainAddress')
          }}</span>
          <a
            :href="llmProviderForm.apiKeyObtainAddress"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ llmProviderForm.apiKeyObtainAddress }}
          </a>
        </div>
        <ElForm
          ref="llmProviderFormRef"
          :model="llmProviderForm"
          status-icon
          label-position="top"
        >
          <ElFormItem prop="apiKey" :label="$t('llmProvider.apiKey')">
            <ElInput
              v-model="llmProviderForm.apiKey"
              @blur="handleFormBlur"
              type="password"
              show-password
            >
              <template #append>
                <ElButton
                  @click="handleTest"
                  style="
                    background-color: var(--el-bg-color);
                    width: 80px;
                    border: 1px solid #f0f0f0;
                    border-radius: 0 8px 8px 0;
                  "
                >
                  {{ $t('llm.button.test') }}
                </ElButton>
              </template>
            </ElInput>
          </ElFormItem>
          <ElFormItem prop="endpoint" :label="$t('llmProvider.endpoint')">
            <ElInput
              v-model.trim="llmProviderForm.endpoint"
              @blur="handleFormBlur"
            />
          </ElFormItem>
          <ElFormItem prop="chatPath" :label="$t('llmProvider.chatPath')">
            <ElInput
              v-model.trim="llmProviderForm.chatPath"
              @blur="handleFormBlur"
            />
          </ElFormItem>
          <ElFormItem prop="embedPath" :label="$t('llmProvider.embedPath')">
            <ElInput
              v-model.trim="llmProviderForm.embedPath"
              @blur="handleFormBlur"
            />
          </ElFormItem>
          <ElFormItem prop="rerankPath" :label="$t('llmProvider.rerankPath')">
            <ElInput
              v-model.trim="llmProviderForm.rerankPath"
              @blur="handleFormBlur"
            />
          </ElFormItem>
        </ElForm>
        <div class="llm-manage-container">
          <div
            v-for="(model, index) in modelTypes"
            :key="model.value"
            class="model-container"
          >
            <div
              class="model-common-title"
              :class="[index === 0 ? 'first-model-title' : '']"
            >
              {{ model.label }}
            </div>

            <!-- 对话模型（chatModel）遍历 -->
            <div
              v-if="model.value === 'chatModel' && chatModelListData.length > 0"
            >
              <ElCollapse expand-icon-position="left">
                <ElCollapseItem
                  v-for="group in chatModelListData"
                  :key="group.groupName"
                  :title="group.groupName"
                  :name="group.groupName"
                >
                  <template #title>
                    <div class="flex items-center justify-between pr-2">
                      <span class="flex items-center gap-1.5">
                        <span>{{ group.groupName }}</span>
                        <span class="publish-count">
                          {{ $t('llm.publishCount') }}
                          {{ countPublishedModels(group.llmList) }}
                        </span>
                      </span>
                      <span class="flex items-center gap-2">
                        <ElButton
                          link
                          type="primary"
                          size="small"
                          :loading="isBatchPublishing(model.value, group.groupName)"
                          :disabled="isBatchPublishing(model.value, group.groupName)"
                          @click.stop="handleBatchPublish(group, model.value)"
                        >
                          {{ $t('llm.batchPublish') }}
                        </ElButton>
                        <ElIcon
                          @click.stop="
                            handleGroupNameUpdateModel(group.groupName)
                          "
                        >
                          <Minus />
                        </ElIcon>
                      </span>
                    </div>
                  </template>
                  <ModelViewItemOperation
                    :llm-list="group.llmList"
                    :icon="defaultIcon"
                    @delete-llm="handleDeleteLlm"
                    @edit-llm="handleEditLlm"
                    @update-with-used="handleUpdateLlm"
                    @publish-change="handlePublishChange"
                  />
                </ElCollapseItem>
              </ElCollapse>
            </div>

            <!-- 嵌入模型（embeddingModel）遍历-->
            <div
              v-if="
                model.value === 'embeddingModel' &&
                embeddingModelListData.length > 0
              "
            >
              <ElCollapse expand-icon-position="left">
                <ElCollapseItem
                  v-for="group in embeddingModelListData"
                  :key="group.groupName"
                  :title="group.groupName"
                  :name="group.groupName"
                >
                  <template #title>
                    <div class="flex items-center justify-between pr-2">
                      <span class="flex items-center gap-1.5">
                        <span>{{ group.groupName }}</span>
                        <span class="publish-count">
                          {{ $t('llm.publishCount') }}
                          {{ countPublishedModels(group.llmList) }}
                        </span>
                      </span>
                      <span class="flex items-center gap-2">
                        <ElButton
                          link
                          type="primary"
                          size="small"
                          :loading="isBatchPublishing(model.value, group.groupName)"
                          :disabled="isBatchPublishing(model.value, group.groupName)"
                          @click.stop="handleBatchPublish(group, model.value)"
                        >
                          {{ $t('llm.batchPublish') }}
                        </ElButton>
                        <ElIcon
                          @click.stop="
                            handleGroupNameUpdateModel(group.groupName)
                          "
                        >
                          <Minus />
                        </ElIcon>
                      </span>
                    </div>
                  </template>
                  <ModelViewItemOperation
                    :llm-list="group.llmList"
                    :icon="defaultIcon"
                    @delete-llm="handleDeleteLlm"
                    @edit-llm="handleEditLlm"
                    @update-with-used="handleUpdateLlm"
                    @publish-change="handlePublishChange"
                  />
                </ElCollapseItem>
              </ElCollapse>
            </div>

            <!-- 重排模型（rerankModel）遍历-->
            <div
              v-if="
                model.value === 'rerankModel' && rerankModelListData.length > 0
              "
            >
              <ElCollapse expand-icon-position="left">
                <ElCollapseItem
                  v-for="group in rerankModelListData"
                  :key="group.groupName"
                  :title="group.groupName"
                  :name="group.groupName"
                >
                  <template #title>
                    <div class="flex items-center justify-between pr-2">
                      <span class="flex items-center gap-1.5">
                        <span>{{ group.groupName }}</span>
                        <span class="publish-count">
                          {{ $t('llm.publishCount') }}
                          {{ countPublishedModels(group.llmList) }}
                        </span>
                      </span>
                      <span class="flex items-center gap-2">
                        <ElButton
                          link
                          type="primary"
                          size="small"
                          :loading="isBatchPublishing(model.value, group.groupName)"
                          :disabled="isBatchPublishing(model.value, group.groupName)"
                          @click.stop="handleBatchPublish(group, model.value)"
                        >
                          {{ $t('llm.batchPublish') }}
                        </ElButton>
                        <ElIcon
                          @click.stop="
                            handleGroupNameUpdateModel(group.groupName)
                          "
                        >
                          <Minus />
                        </ElIcon>
                      </span>
                    </div>
                  </template>
                  <ModelViewItemOperation
                    :llm-list="group.llmList"
                    :icon="defaultIcon"
                    @delete-llm="handleDeleteLlm"
                    @edit-llm="handleEditLlm"
                    @update-with-used="handleUpdateLlm"
                    @publish-change="handlePublishChange"
                  />
                </ElCollapseItem>
              </ElCollapse>
            </div>

            <!-- ocr模型（ocrModel）遍历-->
            <div
              v-if="model.value === 'ocrModel' && ocrModelListData.length > 0"
            >
              <ElCollapse expand-icon-position="left">
                <ElCollapseItem
                  v-for="group in ocrModelListData"
                  :key="group.groupName"
                  :title="group.groupName"
                  :name="group.groupName"
                >
                  <template #title>
                    <div class="flex items-center justify-between pr-2">
                      <span class="flex items-center gap-1.5">
                        <span>{{ group.groupName }}</span>
                        <span class="publish-count">
                          {{ $t('llm.publishCount') }}
                          {{ countPublishedModels(group.llmList) }}
                        </span>
                      </span>
                      <span class="flex items-center gap-2">
                        <ElButton
                          link
                          type="primary"
                          size="small"
                          :loading="isBatchPublishing(model.value, group.groupName)"
                          :disabled="isBatchPublishing(model.value, group.groupName)"
                          @click.stop="handleBatchPublish(group, model.value)"
                        >
                          {{ $t('llm.batchPublish') }}
                        </ElButton>
                        <ElIcon
                          @click.stop="
                            handleGroupNameUpdateModel(group.groupName)
                          "
                        >
                          <Minus />
                        </ElIcon>
                      </span>
                    </div>
                  </template>
                  <ModelViewItemOperation
                    :llm-list="group.llmList"
                    :icon="defaultIcon"
                    @delete-llm="handleDeleteLlm"
                    @edit-llm="handleEditLlm"
                    @update-with-used="handleUpdateLlm"
                    @publish-change="handlePublishChange"
                  />
                </ElCollapseItem>
              </ElCollapse>
            </div>

            <div class="model-operation-container">
              <ElButton
                type="primary"
                @click="handleManageLlm(model.value)"
                :icon="ManageIcon"
              >
                {{ $t('llm.button.management') }}
              </ElButton>
              <ElButton :icon="Plus" @click="handleAddLlm(model.value)">
                {{ $t('button.add') }}
              </ElButton>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!--添加模型供应商模态框-->
    <AddModelProviderModal
      ref="addLlmProviderRef"
      @reload="getLlmProviderListData()"
    />
    <!--添加模型模态框-->
    <AddModelModal
      ref="addLlmRef"
      @reload="getLlmProviderListData()"
      :provider-id="defaultSelectProviderId"
    />
    <!--模型管理模态框-->
    <ManageModelModal ref="manageLlmRef" @reload="getLlmProviderListData()" />

    <!--模型检测配置模态框-->
    <ModelVerifyConfig ref="llmVerifyConfigRef" />
  </div>
</template>

<style scoped>
.llm-container {
  display: flex;
  flex-direction: row;
  padding: 20px;
  height: calc(100vh - 90px);
  gap: 20px;
}

.title {
  font-weight: 500;
  font-size: 16px;
  color: #333333;
  line-height: 22px;
  text-align: left;
  font-style: normal;
  margin-bottom: 20px;
}

.llm-table-container {
  flex: 1;
  padding: 24px;
  background-color: var(--el-bg-color);
  border-radius: 8px;
  overflow: auto;
  border: 1px solid #f0f0f0;
}

.llm-form-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.provider-link-row {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #333333;
  line-height: 22px;
}

.website-top-row {
  margin-bottom: 8px;
}

.provider-link-label {
  color: #606266;
  margin-right: 8px;
}

.provider-link-label::after {
  content: ':';
}

.provider-link-row a {
  color: var(--el-color-primary);
  text-decoration: none;
}

.provider-link-row a:hover {
  text-decoration: underline;
}

.provider-link-placeholder {
  color: #999999;
}

.model-common-title {
  font-weight: 500;
  font-size: 14px;
  color: #333333;
  line-height: 20px;
  text-align: left;
  font-style: normal;
  margin: 24px 0 12px 0;
}

.first-model-title {
  margin: 0 0 12px 0;
}

/* 折叠面板容器 */
:deep(.el-collapse) {
  border: none;
  border-radius: 8px !important;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.el-collapse-item) {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 0;
}

:deep(.el-collapse-item__header) {
  background-color: #f9fafc;
  padding: 0 9px 0 17px;
  border-radius: 8px 8px 0 0;
  border: 1px solid #f0f0f0;
  height: 20px !important;
  line-height: 20px !important;
  font-size: 14px;
  color: #333333;
}

:deep(.el-collapse-item__arrow) {
  line-height: 38px;
  margin-right: 8px;
}

:deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

:deep(.el-collapse-item__content) {
  border: 1px solid #f0f0f0;
  background: #ffffff;
  border-radius: 0 0 8px 8px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
  box-sizing: border-box;
  border-top: none;
}

:deep(.el-collapse-item:last-child) {
  margin-bottom: 0;
}

.model-operation-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.flex.items-center.justify-between.pr-2 {
  height: 100%;
  width: 100%;
}

.publish-count {
  padding: 2px 8px;
  font-size: 10px;
  line-height: 18px;
  /* color: var(--el-color-primary); */
  color: var(--el-text-color-regular); /* #606266 深灰色 */
  background-color: var(--el-color-primary-light-9);
  border-radius: 10px;
  vertical-align: bottom;
  position: relative;
  top: 4px; /* 正数向下，负数向上，按需调 1px~3px */
}
</style>
