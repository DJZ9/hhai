<script setup>
import { onMounted, ref, watch } from 'vue';

import {
  ElCard,
  ElCol,
  ElDivider,
  ElForm,
  ElFormItem,
  ElInput,
  ElOption,
  ElRow,
  ElSelect,
} from 'element-plus';

import DictSelect from '#/components/dict/DictSelect.vue';
import UploadAvatar from '#/components/upload/UploadAvatar.vue';
import { $t } from '#/locales';

import DynamicHeaders from './components/DynamicHeaders.vue';
import DynamicParams from './components/DynamicParams.vue';
import FieldMapping from './components/FieldMapping.vue';

const props = defineProps({
  configJson: {
    type: Object,
    required: true,
  },
});
const configForm = ref(null);
const configData = ref(props.configJson);
watch(
  () => props.configJson,
  (newVal) => {
    configData.value = newVal;
  },
  { immediate: true, deep: true },
);
onMounted(() => {});
</script>

<template>
  <div class="oauth-config-container">
    <ElCard class="config-card" shadow="never">
      <ElForm ref="configForm" :model="configData" label-width="150px">
        <!-- 基础信息 -->
        <ElDivider>{{ $t('oauth.basicConfig') }}</ElDivider>
        <ElRow :gutter="20">
          <ElCol :span="24">
            <ElFormItem :label="$t('oauth.platform')" prop="platform">
              <ElInput v-model="configData.platform" placeholder="wxOpen" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="24">
            <ElFormItem :label="$t('oauth.displayName')" prop="displayName">
              <ElInput
                v-model="configData.displayName"
                placeholder="微信登录"
              />
            </ElFormItem>
          </ElCol>
          <ElCol :span="12">
            <ElFormItem :label="$t('oauth.roleId')" prop="roleId">
              <DictSelect v-model="configData.roleId" dict-code="sysRole" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="12">
            <ElFormItem :label="$t('oauth.enabled')" prop="enabled">
              <DictSelect v-model="configData.enabled" dict-code="yesOrNo" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="24">
            <ElFormItem :label="$t('oauth.icon')" prop="icon">
              <UploadAvatar v-model="configData.icon" />
            </ElFormItem>
          </ElCol>
        </ElRow>

        <ElRow :gutter="20">
          <ElCol :span="24">
            <ElFormItem label="Client ID" prop="clientId">
              <ElInput v-model="configData.clientId" placeholder="clientId" />
            </ElFormItem>
          </ElCol>
          <ElCol :span="24">
            <ElFormItem label="Client Secret" prop="clientSecret">
              <ElInput
                v-model="configData.clientSecret"
                type="password"
                show-password
                placeholder="secret"
              />
            </ElFormItem>
          </ElCol>
        </ElRow>

        <ElFormItem :label="$t('oauth.redirectUrl')" prop="redirectUrl">
          <ElInput
            v-model="configData.redirectUrl"
            :placeholder="`https://yourdomain.com/thirdAuth/callback/{${$t('oauth.platform')}}`"
          />
        </ElFormItem>

        <ElFormItem :label="$t('oauth.userCenterRedirectUrl')" prop="userCenterRedirectUrl">
          <ElInput
            v-model="configData.userCenterRedirectUrl"
            :placeholder="`https://yourdomain.com/userCenter/thirdAuth/callback/{${$t('oauth.platform')}}`"
          />
        </ElFormItem>

        <!-- 授权URL配置 -->
        <ElDivider>{{ $t('oauth.authConfig') }}</ElDivider>
        <ElFormItem :label="$t('oauth.authUrl')" prop="authUrl">
          <ElInput
            v-model="configData.authUrl"
            placeholder="https://open.weixin.qq.com/connect/qrconnect"
          />
        </ElFormItem>

        <div class="section-title">
          {{ `${$t('oauth.paramConfig')}` }}
        </div>
        <DynamicParams v-model="configData.authHttpConfig.params" />

        <!-- 令牌URL配置 -->
        <ElDivider>{{ $t('oauth.tokenConfig') }}</ElDivider>
        <ElFormItem :label="$t('oauth.tokenUrl')" prop="tokenUrl">
          <ElInput
            v-model="configData.tokenUrl"
            placeholder="https://api.weixin.qq.com/sns/oauth2/access_token"
          />
        </ElFormItem>

        <ElFormItem :label="$t('oauth.method')" prop="tokenHttpConfig.method">
          <ElSelect v-model="configData.tokenHttpConfig.method">
            <ElOption label="GET" value="get" />
            <ElOption label="POST" value="post" />
          </ElSelect>
        </ElFormItem>

        <div class="section-title">{{ $t('oauth.headerConfig') }}</div>
        <DynamicHeaders v-model="configData.tokenHttpConfig.headers" />

        <div class="section-title">{{ $t('oauth.paramConfig') }}</div>
        <DynamicParams v-model="configData.tokenHttpConfig.params" />

        <!-- 用户信息配置 -->
        <ElDivider>{{ $t('oauth.userInfoConfig') }}</ElDivider>
        <ElFormItem :label="$t('oauth.userInfoUrl')" prop="userInfoUrl">
          <ElInput
            v-model="configData.userInfoUrl"
            placeholder="https://api.weixin.qq.com/sns/userinfo"
          />
        </ElFormItem>

        <ElFormItem
          :label="$t('oauth.method')"
          prop="userInfoHttpConfig.method"
        >
          <ElSelect v-model="configData.userInfoHttpConfig.method">
            <ElOption label="GET" value="get" />
            <ElOption label="POST" value="post" />
          </ElSelect>
        </ElFormItem>

        <div class="section-title">{{ $t('oauth.headerConfig') }}</div>
        <DynamicHeaders v-model="configData.userInfoHttpConfig.headers" />

        <div class="section-title">{{ $t('oauth.paramConfig') }}</div>
        <DynamicParams v-model="configData.userInfoHttpConfig.params" />

        <div class="section-title">{{ $t('oauth.fieldMapping') }}</div>
        <FieldMapping v-model="configData.userInfoHttpConfig.fieldMapping" />
      </ElForm>
    </ElCard>

    <!-- 预览面板 -->
    <ElCard class="preview-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ $t('oauth.configPreview') }}</span>
        </div>
      </template>
      <pre class="config-preview">{{
        JSON.stringify(configData, null, 2)
      }}</pre>
    </ElCard>
  </div>
</template>

<style scoped>
.oauth-config-container {
  max-width: 1400px;
  padding: 20px;
  margin: 0 auto;
}

.config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: bold;
  color: var(--el-text-color-secondary);
}

.preview-card {
  margin-top: 20px;
}

.config-preview {
  max-height: 400px;
  padding: 20px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
  word-wrap: break-word;
  white-space: pre-wrap;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}
</style>
