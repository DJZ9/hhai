<script setup lang="ts">
import type { FormRules } from 'element-plus';

import { onMounted, ref, useTemplateRef } from 'vue';

import { $t } from '@aiflowy/locales';

import { Delete, Plus } from '@element-plus/icons-vue';
import {
  ElAlert,
  ElButton,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
  ElTabPane,
  ElTabs,
} from 'element-plus';

import { api } from '#/api/request.js';
import providerList from '#/views/ai/model/modelUtils/providerList.json';

const providerOptions =
  ref<Array<{ label: string; options: any; value: string }>>(providerList);
const brands = ref([]);
const llmOptions = ref<any>([]);

// 获取品牌接口数据
function getBrands() {
  api.get('/api/v1/modelProvider/list').then((res) => {
    if (res.errorCode === 0) {
      brands.value = res.data;
      llmOptions.value = formatLlmList(res.data);
    }
  });
}
function getOptions() {
  api
    .get(
      '/api/v1/sysOption/list?keys=model_of_chat&keys=chatgpt_endpoint&keys=chatgpt_chatPath&keys=chatgpt_api_key&keys=chatgpt_model_name&keys=user_center_domain&keys=env_variables&keys=skill_market_domain',
    )
    .then((res) => {
      if (res.errorCode === 0) {
        const { user_center_domain, env_variables, ...rest } = res.data;
        entity.value = rest;
        userCenter.value.user_center_domain = user_center_domain;
        // 解析env_variables，如果不存在或解析失败则使用空对象
        try {
          const parsed = env_variables ? JSON.parse(env_variables) : {};
          envVariables.value = Object.entries(parsed).map(([key, value]) => ({
            key,
            value: String(value),
          }));
        } catch {
          envVariables.value = [];
        }
      }
    });
}
onMounted(() => {
  getOptions();
  getBrands();
});

const entity = ref({
  model_of_chat: '',
  chatgpt_api_key: '',
  chatgpt_chatPath: '',
  chatgpt_endpoint: '',
  chatgpt_model_name: '',
});

// 技能功能配置
const skillMarket = ref({
  skill_market_domain: 'https://skill.aiflowy.tech',
});
function formatLlmList(data: any[]) {
  return data.map((item: any) => {
    const extra = new Map([
      ['chatPath', item.options?.chatPath],
      ['llmEndpoint', item.options?.llmEndpoint],
    ]);
    return {
      label: item.title,
      value: item.key,
      extra,
    };
  });
}
function handleChangeModel(value: any) {
  const extra: any = providerList.find((item: any) => item.value === value);
  entity.value.chatgpt_chatPath = extra.options.chatPath;
  entity.value.chatgpt_endpoint = extra.options.llmEndpoint;
}
function handleSave(data?: Record<string, any>) {
  api.post('/api/v1/sysOption/save', data ?? entity.value).then((res) => {
    if (res.errorCode === 0) {
      ElMessage.success($t('message.saveOkMessage'));
    }
  });
}

// 用户中心设置
const activeName = ref<'ai' | 'envvariables' | 'usercenter' | 'skillmarket'>('ai');

// 系统变量设置
interface EnvVariable {
  key: string;
  value: string;
}
const envVariables = ref<EnvVariable[]>([]);

function addEnvVariable() {
  envVariables.value.push({ key: '', value: '' });
}

function removeEnvVariable(index: number) {
  envVariables.value.splice(index, 1);
}

function handleEnvVariablesSave() {
  // 过滤掉key为空的项，并转换为对象格式
  const validVariables = envVariables.value.filter(
    (item) => item.key.trim() !== '',
  );
  const envVariablesObj: Record<string, string> = {};
  validVariables.forEach((item) => {
    envVariablesObj[item.key.trim()] = item.value;
  });

  handleSave({ env_variables: JSON.stringify(envVariablesObj) });
}

function handleSkillMarketSave() {
  handleSave(skillMarket.value);
}

const userCenterFormEl = useTemplateRef('userCenterForm');
const userCenter = ref({
  user_center_domain: '',
});

const userCenterRules: FormRules = {
  user_center_domain: [
    {
      required: true,
      message: $t('settingsConfig.message.userCenterDomain'),
      trigger: 'blur',
    },
    {
      pattern:
        /^(https?:\/\/)?(?=.{1,255}$)((.{1,63}\.){1,127}(?!\d*$)[a-z0-9-]+\.?)$/i,
      message: $t('settingsConfig.message.userCenterDomainError'),
      trigger: 'blur',
    },
  ],
};

async function handleUserCenterSave() {
  await userCenterFormEl.value?.validate((valid) => {
    if (valid) {
      handleSave(userCenter.value);
    }
  });
}
</script>

<template>
  <div class="settings-container">
    <div class="settings-config-container border-border border">
      <ElTabs v-model="activeName">
        <ElTabPane
          :label="$t('settingsConfig.systemAIFunctionSettings')"
          name="ai"
        >
          <ElAlert
            class="!mb-5"
            :title="$t('settingsConfig.note')"
            type="warning"
          />
          <ElForm :model="entity" class="demo-form-inline" label-width="150px">
            <ElFormItem :label="$t('settingsConfig.modelOfChat')">
              <ElSelect
                v-model="entity.model_of_chat"
                clearable
                @change="handleChangeModel"
              >
                <ElOption
                  v-for="item in providerOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </ElSelect>
            </ElFormItem>
            <ElFormItem :label="$t('settingsConfig.modelName')">
              <ElInput v-model="entity.chatgpt_model_name" clearable />
            </ElFormItem>
            <ElFormItem label="Endpoint">
              <ElInput v-model="entity.chatgpt_endpoint" clearable />
            </ElFormItem>
            <ElFormItem label="ChatPath">
              <ElInput v-model="entity.chatgpt_chatPath" clearable />
            </ElFormItem>
            <ElFormItem label="ApiKey">
              <ElInput v-model="entity.chatgpt_api_key" clearable />
            </ElFormItem>
          </ElForm>
          <div class="settings-button-container">
            <ElButton type="primary" @click="handleSave">
              {{ $t('button.save') }}
            </ElButton>
          </div>
        </ElTabPane>

        <ElTabPane
          :label="$t('settingsConfig.userCenterSettings')"
          name="usercenter"
        >
          <ElForm
            ref="userCenterForm"
            :model="userCenter"
            :rules="userCenterRules"
            class="demo-form-inline"
            label-position="top"
          >
            <ElFormItem
              :label="$t('settingsConfig.userCenterDomain')"
              prop="user_center_domain"
            >
              <ElInput
                clearable
                placeholder="e.g., http://aiflowy.tech"
                v-model="userCenter.user_center_domain"
              />
            </ElFormItem>
          </ElForm>
          <div class="settings-button-container">
            <ElButton type="primary" @click="handleUserCenterSave">
              {{ $t('button.save') }}
            </ElButton>
          </div>
        </ElTabPane>

        <ElTabPane label="系统变量" name="envvariables">
          <ElForm class="demo-form-inline" label-position="top">
            <div
              v-for="(item, index) in envVariables"
              :key="index"
              class="env-variable-row"
            >
              <ElFormItem label="Key" class="env-variable-item">
                <ElInput v-model="item.key" clearable placeholder="变量名" />
              </ElFormItem>
              <ElFormItem label="Value" class="env-variable-item">
                <ElInput v-model="item.value" clearable placeholder="变量值" />
              </ElFormItem>
              <ElButton
                class="env-variable-delete"
                type="danger"
                text
                @click="removeEnvVariable(index)"
              >
                <ElIcon><Delete /></ElIcon>
              </ElButton>
            </div>
            <ElButton type="primary" text @click="addEnvVariable">
              <ElIcon class="mr-1"><Plus /></ElIcon>
              添加变量
            </ElButton>
          </ElForm>
          <div class="settings-button-container">
            <ElButton type="primary" @click="handleEnvVariablesSave">
              {{ $t('button.save') }}
            </ElButton>
          </div>
        </ElTabPane>
         <ElTabPane
          :label="$t('settingsConfig.skillMarketSettings')"
          name="skillmarket"
        >
          <ElAlert
            class="!mb-5"
            :title="$t('settingsConfig.skillMarketNote')"
            type="info"
            :closable="false"
          />
          <ElForm
            :model="skillMarket"
            class="demo-form-inline"
            label-width="150px"
          >
            <ElFormItem :label="$t('settingsConfig.skillMarketDomain')">
              <ElInput
                clearable
                :placeholder="$t('settingsConfig.skillMarketDomainPlaceholder')"
                v-model="skillMarket.skill_market_domain"
              />
            </ElFormItem>
          </ElForm>
          <div class="settings-button-container">
            <ElButton type="primary" @click="handleSkillMarketSave">
              {{ $t('button.save') }}
            </ElButton>
          </div>
        </ElTabPane>
      </ElTabs>
    </div>
  </div>
</template>

<style scoped>
.settings-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 30px 143px;
}

.settings-config-container {
  width: 100%;
  padding: 20px;
  background-color: var(--el-bg-color);
  border-radius: 10px;
}

:deep(.el-form-item) {
  margin-bottom: 25px;
}

.settings-notice {
  margin-bottom: 20px;
  color: var(--el-color-danger);
}

.settings-button-container {
  display: flex;
  justify-content: flex-end;
}

.env-variable-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 16px;
}

.env-variable-item {
  flex: 1;
  margin-bottom: 0;
}

.env-variable-item :deep(.el-form-item__label) {
  line-height: 22px;
}

.env-variable-delete {
  height: 32px;
  margin-bottom: 0;
}
</style>
