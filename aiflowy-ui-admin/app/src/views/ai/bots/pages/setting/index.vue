<script setup lang="ts">
import type { BotInfo } from '@aiflowy/types';
import { ElButton, ElIcon, ElMessage, ElPopover } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { router } from '#/router';
import { api } from '#/api/request';
import { tryit } from 'radash';
import PublishContainer from './PublishContainer.vue';
import { getBotDetails, publishBotApi } from '#/api';
import { hasPermission } from '#/api/common/hasPermission';
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';
import { $t } from '#/locales';
import Config from './config.vue';
import Preview from './preview.vue';
import Prompt from './prompt.vue';

const route = useRoute();
const publishPanelVisible = ref(false);
const bot = ref<BotInfo>();
const latestPublishTime = ref('');
const publishLoading = ref(false);

const showPublish = computed(
  () => bot.value?.version === 1 && !bot.value?.parentId,
);
const hasSavePermission = computed(() =>
  hasPermission(['/api/v1/bot/save', '/api/v1/bot/updateLlmId']),
);
const fetchBotDetail = async (id: string) => {
  const [, res] = await tryit(getBotDetails)(id);

  if (res?.errorCode === 0) {
    bot.value = res.data;
  }
};

// 大模型等配置保存后，重新拉取 Bot 详情，保证各区块展示的配置与后端一致
function handleBotUpdated() {
  if (route.params.id) {
    fetchBotDetail(route.params.id as string);
  }
}

function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return '';
  const time = new Date(dateStr.replace(/-/g, '/')).getTime();
  if (Number.isNaN(time)) return '';
  const diff = Date.now() - time;
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  const month = 30 * day;
  const year = 365 * day;

  if (diff < minute) return '刚刚';
  if (diff < hour) return `${Math.floor(diff / minute)}分钟前`;
  if (diff < day) return `${Math.floor(diff / hour)}小时前`;
  if (diff < month) return `${Math.floor(diff / day)}天前`;
  if (diff < year) return `${Math.floor(diff / month)}个月前`;
  return `${Math.floor(diff / year)}年前`;
}

const latestReleaseText = computed(() => {
  if (!latestPublishTime.value) return '';
  return `发布于${formatRelativeTime(latestPublishTime.value)}`;
});
// 发布更新
async function handlePublish() {
  if (!bot.value?.id) return;

  publishLoading.value = true;
  const [err, res] = await tryit(publishBotApi)({ id: bot.value.id });
  publishLoading.value = false;

  if (!err && res?.errorCode === 0) {
    ElMessage.success(res.message || $t('message.saveOkMessage'));
    getLatestPublishTime();
  } else {
    ElMessage.error(res?.message || $t('message.saveFailMessage'));
  }
}

function getLatestPublishTime() {
  const botId = route.params.id as string;
  if (!botId) return;
  api
    .get('/api/v1/bot/latestPublished', {
      params: { botId: botId },
    })
    .then((res) => {
      if (res.errorCode === 0 && res.data) {
        latestPublishTime.value = res.data?.created ?? '';
      }
    });
}

onMounted(() => {
  if (route.params.id) {
    fetchBotDetail(route.params.id as string);
    getLatestPublishTime();
  }
});
</script>

<template>
  <div class="bot-setting-page">
    <div class="head-div">
      <div class="flex items-center justify-between border-b p-2.5">
        <div>
          <ElButton :icon="ArrowLeft" link @click="router.back()">
            <span class="max-w-[500px] overflow-hidden text-ellipsis text-nowrap text-base" style="font-size: 14px"
              :title="bot?.title">
              {{ bot?.title }}
            </span>
          </ElButton>
        </div>
        <div v-if="showPublish" class="flex items-center">
          <ElPopover v-model:visible="publishPanelVisible" placement="bottom-end" :width="420" trigger="click"
            popper-class="publish-update-popover">
            <template #reference>
              <ElButton type="primary" class="publish-button">
                <span>{{ $t('button.publish') }}</span>
                <ElIcon class="publish-arrow" :class="{ 'is-open': publishPanelVisible }">
                  <ArrowRight />
                </ElIcon>
              </ElButton>
            </template>
            <div class="publish-update-header">
              <div class="publish-release-info">
                <span class="latestRelease">最新发布</span>
                <span class="latestTime">{{ latestReleaseText }}</span>
              </div>
              <div class="publish-update" @click="handlePublish">
                {{ $t('bot.publishUpdate') }}
              </div>
            </div>
            <div class="publish-update-body">
              <PublishContainer :bot-id="bot?.id || ''" />
            </div>
          </ElPopover>
        </div>
      </div>
    </div>
    <div class="settings-container">
      <div class="row-container">
        <div class="row-item">
          <Prompt :bot="bot" :has-save-permission="hasSavePermission" />
        </div>
        <div class="row-item">
          <Config
            :bot="bot"
            :has-save-permission="hasSavePermission"
            @bot-updated="handleBotUpdated"
          />
        </div>
        <div class="row-item">
          <Preview :bot="bot" />
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped>
.bot-setting-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 90px);
  overflow: hidden;
}

.settings-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  gap: 20px;
  padding: 20px;
}

.head-div {
  flex-shrink: 0;
  background-color: var(--el-bg-color);
}

.settings-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.row-container {
  display: flex;
  flex: 1;
  gap: 20px;
  min-height: 0;
}

.row-item {
  flex: 1;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.publish-button {
  min-width: 96px;
}

.publish-arrow {
  margin-left: 4px;
  transition: transform 0.2s ease;
}

.publish-arrow.is-open {
  transform: rotate(90deg);
}
.publish-update-popover {
  padding: 0 !important;
}

.publish-update-popover .el-popper__arrow {
  display: none;
}

.publish-update {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 18px;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  color: #fff;
  cursor: pointer;
  background-color: #0e7fef;
  border-radius: 6px;
  transition: background-color 0.2s ease;
}

.publish-update:hover {
  background-color: #0a6dd6;
}

.publish-update-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color);
}

.publish-release-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.latestRelease {
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
  color: var(--el-text-color-primary);
}

.latestTime {
  font-size: 12px;
  line-height: 16px;
  color: var(--el-text-color-secondary);
}

.publish-update-body {
  padding: 16px 20px 20px;
}
</style>
