<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import { IconifyIcon } from '@aiflowy/icons';
import { $t } from '@aiflowy/locales';

import {
  ElButton,
  ElDialog,
  ElIcon,
  ElInput,
  ElLink,
  ElMessage,
  ElSpace,
  ElSwitch,
} from 'element-plus';
import { Close, TopRight } from '@element-plus/icons-vue';
import { tryit } from 'radash';

import { updateBotApi } from '#/api';
import { api } from '#/api/request';

const props = defineProps<{
  botId: string;
}>();

type DetailType = 'iframe' | 'sdk' | 'api' | null;

const userCenterDomain = ref('');
const publishToUserCenter = ref(0);
const publishToUserCenterLoading = ref(false);
const activeDetail = ref<DetailType>(null);
const detailVisible = ref(false);

const detailTitle = computed(() => {
  switch (activeDetail.value) {
    case 'iframe':
      return $t('bot.iframeEmbedding');
    case 'sdk':
      return $t('bot.sdkEmbedding');
    case 'api':
      return $t('bot.apiEmbedding');
    default:
      return '';
  }
});

const iframeCode = computed(
  () => `<iframe
  src="${userCenterDomain.value}/#/iframe/chat?botId=${props.botId}"
  width="100%"
  height="100%"
></iframe>`,
);

const iframeFullCode = computed(
  () => `<!doctype html>
<html>
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>智能体对话</title>
    <style>
      * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
      }
      html,
      body {
        width: 100%;
        height: 100%;
        overflow: hidden;
      }
    </style>
  </head>
  <body>
    <iframe
      src="${userCenterDomain.value}/#/iframe/chat?botId=${props.botId}"
      width="100%"
      height="100%"
    ></iframe>
  </body>
</html>`,
);

const sdkCode = computed(
  () => `<script src="https://cdn.jsdelivr.net/npm/@aiflowy/websdk/dist/index.js"><\/script>
<script>
  AiflowySDK.initAiflowy({
    botId: "${props.botId}",
    endpoint: "${userCenterDomain.value}"
  });
<\/script>`,
);

function getUserCenterDomain() {
  api.get('/api/v1/sysOption/list?keys=user_center_domain').then((res) => {
    if (res.errorCode === 0 && res.data?.user_center_domain) {
      userCenterDomain.value = res.data.user_center_domain;
    }
  });
}

function getBotStatus() {
  api
    .get('/api/v1/bot/detail', {
      params: {
        id: props.botId,
      },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        publishToUserCenter.value = res.data?.status ?? 0;
      }
    });
}

async function togglePublishToUserCenter(status: number) {
  publishToUserCenterLoading.value = true;

  const [err, res] = await tryit(updateBotApi)({
    id: props.botId,
    status,
  });

  if (!err && res.errorCode === 0) {
    ElMessage.success($t('message.updateOkMessage'));
    publishToUserCenter.value = status;
  }
  publishToUserCenterLoading.value = false;
}

function openUserCenter() {
  if (userCenterDomain.value.length > 0) {
    window.open(
      `${userCenterDomain.value}/#/chatAssistant?botId=${props.botId}`,
      '_blank',
    );
  } else {
    ElMessage.error($t('message.userCenterDomainNotSet'));
  }
}

function copyCode(code: string) {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success($t('bot.copiedToClipboard'));
  });
}

function openDetail(type: Exclude<DetailType, null>) {
  activeDetail.value = type;
  detailVisible.value = true;
}

onMounted(() => {
  getUserCenterDomain();
  getBotStatus();
});
</script>

<template>
  <div class="publish-container">
    <!-- 发布到用户中心：行内展示开关，不弹窗 -->
    <div class="publish-item">
      <span class="publish-item-title">{{ $t('bot.publishToUserCenter') }}</span>
      <div class="publish-item-right">
        <span class="publish-is-published">{{ $t('bot.isPublished') }}</span>
        <ElSwitch :active-value="1" :inactive-value="0" :loading="publishToUserCenterLoading"
          :model-value="publishToUserCenter" @change="(val) => togglePublishToUserCenter(val as number)" />
      </div>
    </div>

    <!-- iframe 嵌入 -->
    <div class="publish-item publish-item-clickable" @click="openDetail('iframe')">
      <span class="publish-item-title">{{ $t('bot.iframeEmbedding') }}</span>
      <ElIcon class="publish-item-arrow">
        <TopRight />
      </ElIcon>
    </div>

    <!-- SDK 嵌入 -->
    <div class="publish-item publish-item-clickable" @click="openDetail('sdk')">
      <span class="publish-item-title">{{ $t('bot.sdkEmbedding') }}</span>
      <ElIcon class="publish-item-arrow">
        <TopRight />
      </ElIcon>
    </div>

    <!-- API 接入 -->
    <div class="publish-item publish-item-clickable" @click="openDetail('api')">
      <span class="publish-item-title">{{ $t('bot.apiEmbedding') }}</span>
      <ElIcon class="publish-item-arrow">
        <TopRight />
      </ElIcon>
    </div>

    <!-- 详情弹窗 -->
    <ElDialog v-model="detailVisible" width="640px" align-center :show-close="false" class="publish-detail-dialog">
      <template #header>
        <div class="detail-dialog-header">
          <span class="detail-dialog-title">{{ detailTitle }}</span>
          <ElButton class="detail-dialog-close" :icon="Close" circle text @click="detailVisible = false" />
        </div>
      </template>

      <div v-if="activeDetail === 'iframe'" class="detail-content">
        <p class="detail-desc">{{ $t('bot.iframeEmbeddingTooltip') }}</p>
        <div class="detail-code-group">
          <div class="detail-code-label">{{ $t('bot.baseEmbeddingCode') }}</div>
          <ElInput type="textarea" :rows="4" readonly :model-value="iframeCode" />
          <ElButton type="primary" size="small" @click="copyCode(iframeCode)">
            {{ $t('bot.copyCode') }}
          </ElButton>
        </div>
        <div class="detail-code-group">
          <div class="detail-code-label">{{ $t('bot.fullEmbeddingCode') }}</div>
          <ElInput type="textarea" :rows="6" readonly :model-value="iframeFullCode" />
        </div>
      </div>

      <div v-if="activeDetail === 'sdk'" class="detail-content">
        <p class="detail-desc">{{ $t('bot.sdkEmbeddingTooltip') }}</p>
        <div class="detail-code-group">
          <div class="detail-code-label">{{ $t('bot.sdkEmbeddingCode') }}</div>
          <ElInput type="textarea" :rows="8" readonly :model-value="sdkCode" />
          <ElButton type="primary" size="small" @click="copyCode(sdkCode)">
            {{ $t('bot.copyCode') }}
          </ElButton>
        </div>
      </div>

      <div v-if="activeDetail === 'api'" class="detail-content">
        <p class="detail-desc">{{ $t('bot.apiEmbeddingTooltip') }}</p>
        <ElSpace size="small" wrap>
          <span class="detail-desc">{{ $t('bot.viewApiDoc') }}</span>
          <ElLink href="https://api.aiflowy.tech/bot/chat" target="_blank" type="primary">
            <ElSpace :size="4">
              https://api.aiflowy.tech/bot/chat
              <ElIcon>
                <IconifyIcon icon="svg:target" />
              </ElIcon>
            </ElSpace>
          </ElLink>
        </ElSpace>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
.publish-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.publish-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 40px;
  padding: 0 12px;
  background-color: #f6f9fb;
  border: 1px solid #e8ebf0;
  border-radius: 6px;
}

.dark .publish-item {
  background-color: hsl(var(--card));
  border-color: hsl(var(--border));
}

.publish-item-clickable {
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.publish-item-clickable:hover {
  border-color: var(--el-color-primary);
  background-color: var(--el-fill-color-light);
}

.publish-item-title {
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--el-text-color-primary);
}

.publish-item-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.publish-is-published {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.publish-item-arrow {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

/* 详情弹窗内容 */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-desc {
  margin: 0;
  font-size: 13px;
  line-height: 20px;
  color: var(--el-text-color-secondary);
}

.detail-code-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-code-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
</style>

<style>
.publish-detail-dialog {
  border-radius: 12px;
  overflow: hidden;
}

.publish-detail-dialog .el-dialog__header {
  margin-right: 0;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color);
}

.publish-detail-dialog .el-dialog__body {
  padding: 20px;
}

.detail-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.detail-dialog-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
  color: var(--el-text-color-primary);
}

.detail-dialog-close {
  color: var(--el-text-color-secondary);
}

.detail-dialog-close:hover {
  color: var(--el-color-danger);
  background-color: var(--el-fill-color-light);
}
</style>