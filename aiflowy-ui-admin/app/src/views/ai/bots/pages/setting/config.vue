<!-- eslint-disable no-useless-escape -->
<script setup lang="ts">
import type { AiLlm, BotInfo } from '@aiflowy/types';

import { computed, onMounted, ref, watch } from 'vue';
// import { useRoute } from 'vue-router';
import { useRouter, useRoute } from 'vue-router'

import { IconifyIcon } from '@aiflowy/icons';
import { $t } from '@aiflowy/locales';
import { useBotStore } from '@aiflowy/stores';

import { Delete, Plus, Setting } from '@element-plus/icons-vue';
import { useDebounceFn } from '@vueuse/core';
import {
  ElButton,
  ElCol,
  ElCollapse,
  ElCollapseItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElLink,
  ElOption,
  ElMessage,
  ElRow,
  ElSelect,
  ElSlider,
  ElSpace,
  ElSwitch,
} from 'element-plus';
import { tryit } from 'radash';

import {
  getPerQuestions,
  updateBotApi,
  updateBotOptions,
  updateLlmId,
  updateLlmOptions,
} from '#/api';
import { api } from '#/api/request';
import ProblemPresupposition from '#/components/chat/ProblemPresupposition.vue';
import PublishWxOfficalAccount from '#/components/chat/PublishWxOfficalAccount.vue';
import CollapseViewItem from '#/components/collapseViewItem/CollapseViewItem.vue';
import CommonSelectDataModal from '#/components/commonSelectModal/CommonSelectDataModal.vue';
import WikiSelectModal from '#/components/wikiSelectModal/WikiSelectModal.vue';
import ToolTemplateConfigModal from '#/views/ai/bots/components/ToolTemplateConfigModal.vue';

interface SelectedMcpTool {
  name: string;
  description: string;
}
const props = defineProps<{
  bot?: BotInfo;
  hasSavePermission?: boolean;
}>();
const botStore = useBotStore();
const route = useRoute();
const router = useRouter();
const botId = ref<string>((route.params.id as string) || '');
const options = ref<AiLlm[]>([]);
const vlModelOptions = ref<AiLlm[]>([]);
const selectedId = ref<string>('');
const llmConfig = ref({
  maxMessageCount: 0,
  maxReplyLength: 0,
  maxReplyLengthEnabled: false,
  temperature: 0,
  temperatureEnabled: false,
  topP: 0,
  topPEnabled: false,
});
const dialogueSettings = ref({
  welcomeMessage: '',
  enableDeepThinking: false,
  // 附件功能配置
  attachmentEnabled: true,
  vlModelId: 'current',
});
watch(
  props,
  (newValue) => {
    if (!newValue.hasSavePermission) {
      selectedId.value = $t('bot.noPermission');
    } else if (newValue.bot?.modelId) {
      selectedId.value = newValue.bot.modelId;
    }

    if (newValue.bot) {
       llmConfig.value = {
        ...llmConfig.value,
        ...newValue.bot.modelOptions,
      };
    }
  },
  { immediate: true },
);
const pluginToolIdsData = ref<any>([]);
const knowledgeIdsData = ref<any>([]);
const workflowIdsData = ref<any>([]);
const mcpToolData = ref<any>([]);
const skillIdsData = ref<any>([]);

const workflowData = ref<any[]>([]);
const knowledgeData = ref<any[]>([]);
const pluginToolData = ref<any[]>([]);
const skillData = ref<any[]>([]);
const getAiBotPluginToolList = async () => {
  api
    .post('/api/v1/pluginItem/tool/list', { botId: botId.value })
    .then((res) => {
      pluginToolData.value = res.data;
      pluginToolIdsData.value = res.data.map((item: any) => item.id);
    });
};
const getAiBotMcpToolList = async () => {
  api.get(`/api/v1/botMcp/list?botId=${botId.value}`).then((res) => {
    mcpToolData.value = res.data;
  });
};
const getAiBotKnowledgeList = async () => {
  api
    .get('/api/v1/botKnowledge/list', {
      params: {
        botId: botId.value,
      },
    })
    .then((res) => {
      knowledgeData.value = res.data.map((item: any) => {
        return {
          recordId: item.id,
          ...item.knowledge,
        };
      });
      knowledgeIdsData.value = res.data.map(
        (item: any) => item.documentCollectionId,
      );
    });
};

const getAiBotWorkflowList = async () => {
  api
    .get('/api/v1/botWorkflow/list', {
      params: {
        botId: botId.value,
      },
    })
    .then((res) => {
      workflowData.value = res.data.map((item: any) => {
        return {
          recordId: item.id,
          outputTemplate: item.outputTemplate,
          ...item.workflow,
        };
      });
      workflowIdsData.value = res.data.map((item: any) => item.workflowId);
    });
};
const getAiBotSkillList = async () => {
  api
    .get('/api/v1/botSkill/list', {
      params: {
        botId: botId.value,
      },
    })
    .then((res) => {
      skillData.value = res.data.map((item: any) => {
        return {
          recordId: item.id,
          ...item.skill,
        };
      });
      skillIdsData.value = res.data.map((item: any) => item.skillId);
    });
};
const botInfo = ref<BotInfo>();
const getBotDetail = async () => {
  api
    .get('/api/v1/bot/detail', {
      params: {
        id: botId.value,
      },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        botInfo.value = res.data;
        if (res.data.options) {
          dialogueSettings.value = res.data.options;
           // 设置botOptions到store，实现实时更新
          botStore.setBotOptions(botId.value, res.data.options);
          // 加载其他技能配置
          if (res.data.options.otherSkills) {
            otherSkills.value = {
              ...otherSkills.value,
              ...res.data.options.otherSkills,
            };
          }
        }
        if (res.data.options?.presetQuestions) {
          botStore.setPresetQuestions(
            botId.value,
            res.data?.options?.presetQuestions,
          );
        }
      }
    });
};
const getLlmListData = async () => {
  const url = `/api/v1/model/list?modelType=chatModel&added=true`;
  api.get(url, {}).then((res) => {
    if (res.errorCode === 0) {
      options.value = res.data;
    }
  });
};
// 获取VL模型列表（多模态模型）
const getVlModelListData = async () => {
  const url = `/api/v1/model/list?modelType=chatModel&added=true`;
  api.get(url, {}).then((res) => {
    if (res.errorCode === 0) {
      // 过滤多模态模型作为VL模型选项
      const vlModels = res.data.filter(
        (model: any) => model.supportImage === true,
      );
      // 添加"使用当前对话模型"选项（值为'current'）
      vlModelOptions.value = [
        { id: 'current', title: $t('bot.useCurrentModel') },
        ...vlModels,
      ];
    }
  });
};
onMounted(async () => {
  getAiBotPluginToolList();
  getAiBotKnowledgeList();
  getAiBotWorkflowList();
  getAiBotMcpToolList();
  getAiBotSkillList();
  getBotDetail();
  getVlModelListData();
  getLlmListData();
  getAiBotWikiList();
  getUserCenterDomain();
  getSystemSearchEngineConfig();
});

const handleLlmChange = async (value: string) => {
  if (!props.bot) return;

  const [, res] = await tryit(updateLlmId)({
    id: props.bot?.id || '',
    modelId: value,
  });

  if (res?.errorCode === 0) {
    ElMessage.success($t('message.saveOkMessage'));
  } else {
    ElMessage.error(res?.message || $t('message.saveFailMessage'));
  }
};
const handleLlmOptionsChange = useDebounceFn(
  (key: keyof typeof llmConfig.value, value: number | boolean) => {
    updateLlmOptions({
      id: props.bot?.id || '',
      llmOptions: {
        [key]: value,
      },
    });
  },
  300,
);

const handleDialogOptionsStrChange = useDebounceFn(
  (
    key: keyof typeof dialogueSettings.value,
    value: boolean | number | string,
  ) => {
     // 更新botStore，实现实时更新
    botStore.updateBotOption(botId.value, key, value);
    updateBotOptions({
      id: props.bot?.id || '',
      options: {
        [key]: value,
      },
    });
  },
  300,
);

// 搜索引擎选项
const searchEngineOptions = [
  { label: '百度千帆', value: 'qianfan' },
  { label: '博查 (Bocha)', value: 'bocha' },
  { label: 'Brave', value: 'brave' },
];

// 其他技能配置变更
const handleOtherSkillChange = useDebounceFn(
  (key: keyof typeof otherSkills.value, value: boolean | number | string) => {
    updateBotOptions({
      id: props.bot?.id || '',
      options: {
        otherSkills: {
          ...otherSkills.value,
          [key]: value,
        },
      },
    });
  },
  300,
);

// 其他技能配置
const otherSkills = ref({
  webSearchEnabled: false,
  webSearchProvider: '',
  webFetchEnabled: false,
  todoWriteEnabled: false,
});

// 跳转到系统设置页面
const goToSystemSettings = () => {
  router.push('/sys/settings');
};

// 系统搜索引擎配置状态
const systemSearchEngineConfig = ref({
  provider: '',
  qianfanApiKey: '',
  bochaApiKey: '',
  braveApiKey: '',
});

const pluginToolDataRef = ref();
const knowledgeDataRef = ref();
const workflowDataRef = ref();
const publishWxRef = ref();
const mcpToolDataRef = ref();
const skillDataRef = ref();

const handleAddPlugin = () => {
  pluginToolDataRef.value.openDialog(pluginToolIdsData.value);
};
const handleAddMcpTool = () => {
  const formattedSelectedMcp = formatSelectedMcpData();
  mcpToolDataRef.value.openMcpDialog(formattedSelectedMcp);
};
const handleAddKnowledge = () => {
  knowledgeDataRef.value.openDialog(knowledgeIdsData.value);
};
const handleAddWorkflow = () => {
  workflowDataRef.value.openDialog(workflowIdsData.value);
};
const handleAddSkill = () => {
  skillDataRef.value.openDialog(skillIdsData.value);
};
const confirmUpdateAiBotSkill = (data: any) => {
  api
    .post('/api/v1/botSkill/updateBotSkillIds', {
      botId: botId.value,
      skillIds: data,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.updateOkMessage'));
        getAiBotSkillList();
      } else {
        ElMessage.error(res.message);
      }
    });
};
const deleteSkill = (item: any) => {
  api
    .post('/api/v1/botSkill/remove', {
      id: item.recordId,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.deleteOkMessage'));
        getAiBotSkillList();
      } else {
        ElMessage.error(res.message);
      }
    });
};
const confirmUpdateAiBotPlugin = (data: any) => {
  api
    .post('/api/v1/botPlugins/updateBotPluginToolIds', {
      botId: botId.value,
      pluginToolIds: data,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.updateOkMessage'));
        getAiBotPluginToolList();
      } else {
        ElMessage.error(res.message);
      }
    });
};

const confirmUpdateAiBotKnowledge = (data: any) => {
  api
    .post('/api/v1/botKnowledge/updateBotKnowledgeIds', {
      botId: botId.value,
      knowledgeIds: data,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.updateOkMessage'));
        getAiBotKnowledgeList();
      } else {
        ElMessage.error(res.message);
      }
    });
};

const confirmUpdateAiBotWorkflow = (data: any) => {
  api
    .post('/api/v1/botWorkflow/updateBotWorkflowIds', {
      botId: botId.value,
      workflowIds: data,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.updateOkMessage'));
        getAiBotWorkflowList();
      } else {
        ElMessage.error(res.message);
      }
    });
};
const confirmUpdateAiBotMcp = (data: any) => {
  api
    .post('/api/v1/botMcp/updateBotMcpToolIds', {
      botId: botId.value,
      mcpSelectedData: data,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.updateOkMessage'));
        getAiBotMcpToolList();
      } else {
        ElMessage.error(res.message);
      }
    });
};
const deletePluginTool = (item: any) => {
  api
    .post('/api/v1/botPlugins/doRemove', {
      botId: botId.value,
      pluginToolId: item.id,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.deleteOkMessage'));
        getAiBotPluginToolList();
      } else {
        ElMessage.error(res.message);
      }
    });
};

const deleteBotMcpTool = (item: any) => {
  api
    .post('/api/v1/botMcp/remove', {
      id: item.id,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.deleteOkMessage'));
        getAiBotMcpToolList();
      } else {
        ElMessage.error(res.message);
      }
    });
};

const deleteKnowledge = (item: any) => {
  api
    .post('/api/v1/botKnowledge/remove', {
      id: item.recordId,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.deleteOkMessage'));
        getAiBotKnowledgeList();
      } else {
        ElMessage.error(res.message);
      }
    });
};

const deleteWorkflow = (item: any) => {
  api
    .post('/api/v1/botWorkflow/remove', {
      id: item.recordId,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.deleteOkMessage'));
        getAiBotWorkflowList();
      } else {
        ElMessage.error(res.message);
      }
    });
};

const problemPresuppositionRef = ref();

const handleAddPresetQuestion = () => {
  problemPresuppositionRef.value.openDialog(
    botInfo.value?.options?.presetQuestions,
  );
};

const handleProblemPresuppositionSuccess = (data: any) => {
  api
    .post('/api/v1/bot/updateOptions', {
      id: botId.value,
      options: {
        presetQuestions: data,
      },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.updateOkMessage'));
        getBotDetail();
        botStore.setPresetQuestions(botId.value, data);
      } else {
        ElMessage.error(res.message);
      }
    });
};
const handleDeletePresetQuestion = (item: any) => {
  const tempData = botInfo.value?.options?.presetQuestions?.filter(
    (i: any) => i.key !== item,
  );
  api
    .post('/api/v1/bot/updateOptions', {
      id: botId.value,
      options: {
        presetQuestions: tempData,
      },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        getBotDetail();
      }
    });
};
const handlePublishWx = () => {
  publishWxRef.value.openDialog(botId.value, botInfo.value?.options || {});
};
const handleUpdatePublishWx = () => {
  api
    .post('/api/v1/bot/updateOptions', {
      id: botId.value,
      options: {
        weChatMpAppId: '',
        weChatMpSecret: '',
        weChatMpToken: '',
        EncodingAESKey: '',
      },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.updateOkMessage'));
        getBotDetail();
      } else {
        ElMessage.error(res.message);
      }
    });
};
const formatSelectedMcpData = () => {
  // 定义格式化后的结果，与组件内selectedToolMap结构一致
  const formattedData: Record<number | string, SelectedMcpTool[]> = {};

  // 遍历已有的MCP工具列表（mcpToolData.value）
  mcpToolData.value.forEach((mcpItem: any) => {
    const mcpParentId = mcpItem.mcpId || mcpItem.id;

    const toolInfo: SelectedMcpTool = {
      name: mcpItem.mcpToolName,
      description: mcpItem.mcpToolDescription,
    };

    if (!formattedData[mcpParentId]) {
      formattedData[mcpParentId] = [];
    }
    formattedData[mcpParentId].push(toolInfo);
  });

  return formattedData;
};

// 工具模板配置
const templateConfigVisible = ref(false);
const currentToolConfig = ref<any>({
  type: '',
  toolId: '',
  recordId: '',
  name: '',
  outputTemplate: '',
});
const toolTemplateConfigRef = ref();

const handleConfigWorkflow = (item: any) => {
  currentToolConfig.value = {
    type: 'workflow',
    toolId: item.id,
    recordId: item.recordId,
    name: item.title,
    outputTemplate: item.outputTemplate,
  };
  templateConfigVisible.value = true;
};

const handleConfigPlugin = (item: any) => {
  currentToolConfig.value = {
    type: 'plugin',
    toolId: item.id,
    recordId: item.recordId,
    name: item.name,
    outputTemplate: item.outputTemplate,
  };
  templateConfigVisible.value = true;
};

const handleConfigMcp = (item: any) => {
  currentToolConfig.value = {
    type: 'mcp',
    toolId: item.mcpId,
    recordId: item.id,
    name: item.mcpToolName,
    outputTemplate: item.outputTemplate,
  };
  templateConfigVisible.value = true;
};

const handleTemplateConfigSuccess = () => {
  getAiBotWorkflowList();
  getAiBotPluginToolList();
  getAiBotMcpToolList();
};
// Wiki - start
const wikiData = ref<any[]>([]);
const wikiIdsData = ref<any[]>([]);
const wikiDataRef = ref();
// 获取 Bot 绑定的 Wiki 列表
const getAiBotWikiList = async () => {
  api
    .get('/api/v1/botWiki/list', {
      params: {
        botId: botId.value,
      },
    })
    .then((res) => {
      wikiData.value = res.data.map((item: any) => {
        return {
          recordId: item.id,
          ...item.wiki,
        };
      });
      wikiIdsData.value = res.data.map((item: any) => item.wikiId);
    });
};

// 删除 Wiki 绑定
const deleteWiki = (item: any) => {
  api
    .post('/api/v1/botWiki/remove', {
      id: item.recordId,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.deleteOkMessage'));
        getAiBotWikiList();
      } else {
        ElMessage.error(res.message);
      }
    });
};

// 更新 Bot Wiki 绑定
const confirmUpdateAiBotWiki = (data: any) => {
  api
    .post('/api/v1/botWiki/updateBotWikiIds', {
      botId: botId.value,
      wikiIds: data,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('message.updateOkMessage'));
        getAiBotWikiList();
      } else {
        ElMessage.error(res.message);
      }
    });
};

const handleAddWiki = () => {
  wikiDataRef.value.openDialog(wikiIdsData.value);
};
// Wiki - end

// iframe 嵌入代码
const userCenterDomain = ref('');
const iframeCode = computed(
  () => `<iframe
  src="${userCenterDomain.value}/#/iframe/chat?botId=${botId.value}"
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
      src="${userCenterDomain.value}/#/iframe/chat?botId=${botId.value}"
      width="100%"
      height="100%"
    ></iframe>
  </body>
</html>`,
);

function getUserCenterDomain() {
  api.get('/api/v1/sysOption/list?keys=user_center_domain').then((res) => {
    if (res.errorCode === 0 && res.data?.user_center_domain) {
      userCenterDomain.value = res.data.user_center_domain;
    }
  });
}

// 发布到用户中心
const publishToUserCenter = ref(0);
const publishToUserCenterLoading = ref(false);

async function togglePublishToUserCenter(status: number) {
  publishToUserCenterLoading.value = true;

  const [err, res] = await tryit(updateBotApi)({
    id: botId.value,
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
      `${userCenterDomain.value}/#/chatAssistant?botId=${botId.value}`,
      '_blank',
    );
  } else {
    ElMessage.error($t('message.userCenterDomainNotSet'));
  }
}

// SDK 嵌入代码
const sdkCode = computed(
  () => `<script src="https://cdn.jsdelivr.net/npm/@aiflowy/websdk/dist/index.js"><\/script>
<script>
  AiflowySDK.initAiflowy({
    botId: "${botId.value}",
    endpoint: "${userCenterDomain.value}"
  });
<\/script>`,
);

function copyCode(code: string) {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success($t('bot.copiedToClipboard'));
  });
}

// 获取系统搜索引擎配置
const getSystemSearchEngineConfig = async () => {
  try {
    const res = await api.get(
      '/api/v1/sysOption/list?keys=search_engine_provider&keys=search_engine_qianfan_api_key&keys=search_engine_bocha_api_key&keys=search_engine_brave_api_key',
    );
    if (res.errorCode === 0) {
      systemSearchEngineConfig.value = {
        provider: res.data.search_engine_provider || '',
        qianfanApiKey: res.data.search_engine_qianfan_api_key || '',
        bochaApiKey: res.data.search_engine_bocha_api_key || '',
        braveApiKey: res.data.search_engine_brave_api_key || '',
      };
    }
  } catch (error) {
    console.error('获取系统搜索引擎配置失败', error);
  }
};

// 检查搜索引擎配置是否完整
const checkSearchEngineConfig = (): boolean => {
  let provider = otherSkills.value.webSearchProvider;

  // 如果 Bot 未选择提供商，尝试使用系统默认提供商
  if (!provider && systemSearchEngineConfig.value.provider) {
    provider = systemSearchEngineConfig.value.provider;
    // 自动填充 Bot 的提供商选择
    otherSkills.value.webSearchProvider = provider;
  }

  if (!provider) {
    ElMessage.warning('请先选择搜索引擎提供商');
    return false;
  }

  if (provider === 'qianfan' && !systemSearchEngineConfig.value.qianfanApiKey) {
    ElMessage.warning(
      '系统未配置百度千帆搜索引擎 API Key，请先前往系统设置页面配置',
    );
    return false;
  }

  if (provider === 'bocha' && !systemSearchEngineConfig.value.bochaApiKey) {
    ElMessage.warning(
      '系统未配置博查搜索引擎 API Key，请先前往系统设置页面配置',
    );
    return false;
  }

  if (provider === 'brave' && !systemSearchEngineConfig.value.braveApiKey) {
    ElMessage.warning(
      '系统未配置 Brave 搜索引擎 API Key，请先前往系统设置页面配置',
    );
    return false;
  }

  return true;
};

// 处理搜索引擎开关
const handleWebSearchEnableChange = (value: boolean | number | string) => {
  const boolValue = Boolean(value);
  if (
    boolValue && // 开启时检查配置
    !checkSearchEngineConfig()
  ) {
    // 配置不完整，强制关闭开关（阻止开启）
    otherSkills.value.webSearchEnabled = false;
    return;
  }

  otherSkills.value.webSearchEnabled = boolValue;
  handleOtherSkillChange('webSearchEnabled', boolValue);
};
</script>

<template>
  <div class="config-container flex flex-col gap-3">
    <!-- 大模型 -->
    <div
      class="bg-background dark:border-border flex flex-col gap-3 rounded-lg p-3 dark:border"
    >
      <h1 class="text-base font-medium">{{ $t('bot.llm') }}</h1>
      <div
        class="llm-back-container dark:border-border flex w-full flex-col justify-between gap-1 rounded-lg p-3 dark:border"
      >
        <ElSelect
          v-model="selectedId"
          :options="options"
          :props="{ value: 'id', label: 'title' }"
          :disabled="!hasSavePermission"
          @change="handleLlmChange"
        />
        <!-- 温度 -->
        <ElRow :gutter="12" align="middle" type="flex">
          <ElCol class="options-config-item-left">
            <span class="config-font-style">{{ $t('bot.temperature') }}</span>
          </ElCol>
          <ElCol class="options-config-item-middle">
            <ElSlider
              :min="0.1"
              :max="1"
              :step="0.1"
              :disabled="!hasSavePermission"
              v-model="llmConfig.temperature"
              @change="
                (value) =>
                  handleLlmOptionsChange('temperature', value as number)
              "
            />
          </ElCol>
          <ElCol class="options-config-item-right">
            <ElInputNumber
              :min="0.1"
              :max="1"
              :step="0.1"
              :disabled="!hasSavePermission"
              v-model="llmConfig.temperature"
              @change="
                (value) =>
                  handleLlmOptionsChange('temperature', value as number)
              "
            />
          </ElCol>
          <ElCol class="options-config-item-switch">
            <ElSwitch
              v-model="llmConfig.temperatureEnabled"
              :disabled="!hasSavePermission"
              active-text="启用"
              inactive-text="关闭"
              inline-prompt
              @change="
                (value) =>
                  handleLlmOptionsChange('temperatureEnabled', value as boolean)
              "
              />
          </ElCol>
        </ElRow>
        <ElRow :gutter="12" align="middle" type="flex">
          <ElCol class="options-config-item-left">
            <span class="config-font-style">TopP</span>
          </ElCol>
          <ElCol class="options-config-item-middle">
            <ElSlider
              :min="0.1"
              :max="1"
              :step="0.1"
              :disabled="!hasSavePermission"
              v-model="llmConfig.topP"
              @change="
                (value) => handleLlmOptionsChange('topP', value as number)
              "
            />
          </ElCol>
          <ElCol class="options-config-item-right">
            <ElInputNumber
              :min="0.1"
              :max="1"
              :step="0.1"
              :disabled="!hasSavePermission"
              v-model="llmConfig.topP"
              @change="
                (value) => handleLlmOptionsChange('topP', value as number)
              "
            />
          </ElCol>
          <ElCol class="options-config-item-switch">
            <ElSwitch
              v-model="llmConfig.topPEnabled"
              :disabled="!hasSavePermission"
              active-text="启用"
              inactive-text="关闭"
              inline-prompt
              @change="
                (value) =>
                  handleLlmOptionsChange('topPEnabled', value as boolean)
              "
            />
          </ElCol>
        </ElRow>
        <ElRow :gutter="12" align="middle" type="flex">
          <ElCol class="options-config-item-left">
            <span class="config-font-style">{{
              $t('bot.maxReplyLength')
            }}</span>
          </ElCol>
          <ElCol class="options-config-item-middle">
            <ElSlider
              :min="10000"
              :max="200000"
              :step="1000"
              :disabled="!hasSavePermission"
              v-model="llmConfig.maxReplyLength"
              @change="
                (value) =>
                  handleLlmOptionsChange('maxReplyLength', value as number)
              "
            />
          </ElCol>
          <ElCol class="options-config-item-right">
            <ElInputNumber
              :min="10000"
              :max="200000"
              :step="1000"
              :disabled="!hasSavePermission"
              v-model="llmConfig.maxReplyLength"
              @change="
                (value) =>
                  handleLlmOptionsChange('maxReplyLength', value as number)
              "
            />
          </ElCol>
          <ElCol class="options-config-item-switch">
            <ElSwitch
              v-model="llmConfig.maxReplyLengthEnabled"
              :disabled="!hasSavePermission"
              active-text="启用"
              inactive-text="关闭"
              inline-prompt
              @change="
                (value) =>
                  handleLlmOptionsChange('maxReplyLengthEnabled', value as boolean)
              "
            />
          </ElCol>
        </ElRow>
        <ElRow :gutter="12" align="middle" type="flex">
          <ElCol class="options-config-item-left">
            <span class="config-font-style">{{ $t('bot.historyCount') }}</span>
          </ElCol>
          <ElCol class="options-config-item-middle">
            <ElSlider
              :min="1"
              :max="100"
              :step="10"
              :disabled="!hasSavePermission"
              v-model="llmConfig.maxMessageCount"
              @change="
                (value) =>
                  handleLlmOptionsChange('maxMessageCount', value as number)
              "
            />
          </ElCol>
          <ElCol class="options-config-item-right">
            <ElInputNumber
              :min="1"
              :max="100"
              :step="1"
              :disabled="!hasSavePermission"
              v-model="llmConfig.maxMessageCount"
              @change="
                (value) =>
                  handleLlmOptionsChange('maxMessageCount', value as number)
              "
            />
          </ElCol>
        </ElRow>
      </div>
    </div>

    <!-- 技能 -->
    <div
      class="bg-background dark:border-border flex flex-col gap-3 rounded-lg p-3 dark:border"
    >
      <h1 class="text-base font-medium">
        {{ $t('bot.skill') }}
      </h1>
      <div class="flex w-full flex-col justify-between">
        <ElCollapse expand-icon-position="left">
          <ElCollapseItem>
            <template #title>
              <div class="flex items-center justify-between pr-2">
                <span>{{ $t('menus.ai.workflow') }}</span>
                <div class="collapse-right-container">
                  <span class="badge-circle">
                    {{ workflowData.length }}
                  </span>
                  <span @click="handleAddWorkflow()">
                    <ElIcon>
                      <Plus />
                    </ElIcon>
                  </span>
                </div>
              </div>
            </template>
            <CollapseViewItem
              :data="workflowData"
              show-config
              @delete="deleteWorkflow"
              @config="handleConfigWorkflow"
            />
          </ElCollapseItem>
          <ElCollapseItem>
            <template #title>
              <div class="flex items-center justify-between pr-2">
                <span>{{ $t('menus.ai.documentCollection') }}</span>
                <div class="collapse-right-container">
                  <span class="badge-circle">
                    {{ knowledgeData.length }}
                  </span>
                  <span @click="handleAddKnowledge()">
                    <ElIcon>
                      <Plus />
                    </ElIcon>
                  </span>
                </div>
              </div>
            </template>
            <CollapseViewItem :data="knowledgeData" @delete="deleteKnowledge" />
          </ElCollapseItem>
          <ElCollapseItem>
            <template #title>
              <div class="flex items-center justify-between pr-2">
                <span>{{ $t('menus.ai.plugin') }}</span>
                <div class="collapse-right-container">
                  <span class="badge-circle">
                    {{ pluginToolData.length }}
                  </span>
                  <span @click="handleAddPlugin()">
                    <ElIcon>
                      <Plus />
                    </ElIcon>
                  </span>
                </div>
              </div>
            </template>
            <CollapseViewItem
              :data="pluginToolData"
              title-key="name"
              show-config
              @delete="deletePluginTool"
              @config="handleConfigPlugin"
            />
          </ElCollapseItem>
          <ElCollapseItem title="MCP">
            <template #title>
              <div class="flex items-center justify-between pr-2">
                <span>MCP</span>
                <div class="collapse-right-container">
                  <span class="badge-circle">
                    {{ mcpToolData.length }}
                  </span>
                  <span @click="handleAddMcpTool()">
                    <ElIcon>
                      <Plus />
                    </ElIcon>
                  </span>
                </div>
              </div>
            </template>
            <CollapseViewItem
              :data="mcpToolData"
              title-key="mcpToolName"
              description-key="mcpToolDescription"
              show-config
              @delete="deleteBotMcpTool"
              @config="handleConfigMcp"
            />
          </ElCollapseItem>
          <ElCollapseItem title="Skill">
            <template #title>
              <div class="flex items-center justify-between pr-2">
                <span>Skill</span>
                <div class="collapse-right-container">
                  <span class="badge-circle">
                    {{ skillData.length }}
                  </span>
                  <span @click="handleAddSkill()">
                    <ElIcon>
                      <Plus />
                    </ElIcon>
                  </span>
                </div>
              </div>
            </template>
            <CollapseViewItem
              :data="skillData"
              title-key="title"
              description-key="description"
              @delete="deleteSkill"
            />
          </ElCollapseItem>
          <ElCollapseItem title="Wiki">
            <template #title>
              <div class="flex items-center justify-between pr-2">
                <span>Wiki</span>
                <div class="collapse-right-container">
                  <span class="badge-circle">
                    {{ wikiData.length }}
                  </span>
                  <span @click="handleAddWiki()">
                    <ElIcon>
                      <Plus />
                    </ElIcon>
                  </span>
                </div>
              </div>
            </template>
            <CollapseViewItem
              :data="wikiData"
              title-key="title"
              description-key="description"
              @delete="deleteWiki"
            />
          </ElCollapseItem>

          <!-- 其他技能 -->
          <ElCollapseItem :title="$t('bot.otherSkills')">
            <template #title>
              <div class="flex items-center justify-between pr-2">
                <span>{{ $t('bot.otherSkills') }}</span>
              </div>
            </template>
            <div class="other-skills-container">
              <!-- 搜索引擎 -->
              <div class="other-skill-item search-engine-skill">
                <div class="search-engine-row">
                  <div class="search-engine-title">
                    <span class="skill-label">{{
                      $t('bot.webSearchTool')
                    }}</span>
                    <ElSelect
                      v-model="otherSkills.webSearchProvider"
                      :placeholder="$t('bot.webSearchProvider')"
                      size="small"
                      class="search-engine-select"
                      @change="
                        handleOtherSkillChange('webSearchProvider', $event)
                      "
                    >
                      <ElOption
                        v-for="item in searchEngineOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </ElSelect>
                  </div>
                  <ElSwitch
                    v-model="otherSkills.webSearchEnabled"
                    @change="handleWebSearchEnableChange"
                  />
                </div>
                <div class="search-engine-row">
                  <span class="skill-desc">{{
                    $t('bot.webSearchToolDesc')
                  }}</span>
                  <ElLink
                    type="primary"
                    underline="never"
                    class="settings-link"
                    @click="goToSystemSettings"
                  >
                    {{ $t('bot.goToSettings') }}
                  </ElLink>
                </div>
              </div>
              <!-- 网页抓取 -->
              <div class="other-skill-item">
                <div class="skill-info">
                  <span class="skill-label">{{
                    $t('bot.webCrawlerTool')
                  }}</span>
                  <span class="skill-desc">{{
                    $t('bot.webCrawlerToolDesc')
                  }}</span>
                </div>
                <ElSwitch
                  v-model="otherSkills.webFetchEnabled"
                  @change="handleOtherSkillChange('webFetchEnabled', $event)"
                />
              </div>
              <!-- 任务分解 -->
              <div class="other-skill-item">
                <div class="skill-info">
                  <span class="skill-label">{{ $t('bot.todoWriteTool') }}</span>
                  <span class="skill-desc">{{
                    $t('bot.todoWriteToolDesc')
                  }}</span>
                </div>
                <ElSwitch
                  v-model="otherSkills.todoWriteEnabled"
                  @change="handleOtherSkillChange('todoWriteEnabled', $event)"
                />
              </div> 
            </div>
          </ElCollapseItem>
        </ElCollapse>
      </div>
    </div>

    <!-- 对话设置 -->
    <div
      class="bg-background dark:border-border flex flex-col gap-3 rounded-lg p-3 dark:border"
    >
      <h1 class="text-base font-medium">
        {{ $t('bot.conversationSettings') }}
      </h1>
      <div class="flex w-full flex-col justify-between rounded-lg">
        <ElCollapse expand-icon-position="left">
          <ElCollapseItem>
            <template #title>
              <div class="flex items-center justify-between pr-2">
                <span>{{ $t('bot.problemPresupposition') }}</span>
                <span @click="handleAddPresetQuestion()">
                  <ElIcon>
                    <Plus />
                  </ElIcon>
                </span>
              </div>
            </template>
            <div class="question-container">
              <div
                v-for="item in getPerQuestions(
                  botStore.getPresetQuestions(botId),
                )"
                :key="item.key"
              >
                <div class="presetQues-container" v-if="item.description">
                  <span>{{ item.description }}</span>
                  <ElIcon
                    color="var(--el-color-danger)"
                    size="20px"
                    @click="handleDeletePresetQuestion(item.key)"
                    class="el-list-item-delete-container"
                  >
                    <Delete />
                  </ElIcon>
                </div>
              </div>
            </div>
          </ElCollapseItem>
          <ElCollapseItem :title="$t('bot.welcomeMessage')">
            <div class="bg-[var(--bot-collapse-itme-back)] p-2.5">
              <ElInput
                v-model="dialogueSettings.welcomeMessage"
                :placeholder="$t('bot.placeholder.welcome')"
                type="textarea"
                @change="
                  (value) =>
                    handleDialogOptionsStrChange('welcomeMessage', value)
                "
              />
            </div>
          </ElCollapseItem>
          <ElCollapseItem :title="$t('bot.deepThinking')">
            <div class="enable-think-container">
              <div>{{ $t('bot.enableDeepThinking') }}</div>
              <ElSwitch
                v-model="dialogueSettings.enableDeepThinking"
                @change="
                  (value: string | number | boolean) =>
                    handleDialogOptionsStrChange('enableDeepThinking', value)
                "
              />
            </div>
          </ElCollapseItem>
          <!-- 附件功能配置 -->
          <ElCollapseItem :title="$t('bot.attachmentConfig')">
            <div class="enable-think-container">
              <div>{{ $t('bot.attachmentEnabled') }}</div>
              <ElSwitch
                v-model="dialogueSettings.attachmentEnabled"
                @change="
                  (value: string | number | boolean) =>
                    handleDialogOptionsStrChange('attachmentEnabled', value)
                "
              />
            </div>
            <div class="compression-config">
              <div class="compression-config-item">
                <span class="config-label">
                  {{ $t('bot.vlModel') }}
                  <ElTooltip
                    :content="$t('bot.vlModelTooltip')"
                    placement="top"
                    effect="dark"
                    class="label-tooltip"
                  >
                    <InfoFilled class="info-icon" />
                  </ElTooltip>
                </span>
                <ElSelect
                  v-model="dialogueSettings.vlModelId"
                  :options="vlModelOptions"
                  :props="{ value: 'id', label: 'title' }"
                  :disabled="!hasSavePermission"
                  :placeholder="$t('bot.selectVlModel')"
                  @change="
                    (value: string) =>
                      handleDialogOptionsStrChange('vlModelId', value)
                  "
                />
              </div>
            </div>
          </ElCollapseItem>
        </ElCollapse>
      </div>
    </div>

    <!-- 发布 -->
    <div
      class="publish-container bg-background dark:border-border flex flex-col gap-3 rounded-lg p-3 dark:border"
    >
      <h1 class="text-base font-medium">
        {{ $t('bot.publish') }}
      </h1>
      <div class="flex w-full flex-col justify-between rounded-lg">
        <ElCollapse expand-icon-position="left">
          <ElCollapseItem :title="$t('bot.postToWeChatOfficialAccount')">
            <div class="publish-wx-container">
              <div class="publish-wx">
                <span v-if="botInfo?.options?.weChatMpAppId">{{
                  $t('bot.configured')
                }}</span>
                <span v-else>{{ $t('bot.notConfigured') }}</span>
                <div class="publish-config-operation">
                  <div
                    class="publish-wx-right-container"
                    @click="handlePublishWx"
                  >
                    <ElButton link type="primary">
                      <ElIcon class="mr-1">
                        <Setting />
                      </ElIcon>
                      {{ $t('button.edit') }}
                    </ElButton>
                  </div>
                  <div
                    v-if="botInfo?.options?.weChatMpAppId"
                    class="publish-wx-right-container"
                    @click="handleUpdatePublishWx"
                  >
                    <ElButton link type="danger">
                      <ElIcon class="mr-1">
                        <Delete />
                      </ElIcon>
                      {{ $t('button.delete') }}
                    </ElButton>
                  </div>
                </div>
              </div>
            </div>
          </ElCollapseItem>
          <ElCollapseItem :title="$t('bot.publishToUserCenter')">
            <div class="publish-wx-container">
              <div class="publish-wx">
                <ElSpace>
                  <span>{{ $t('bot.isPublished') }}</span>
                  <ElSwitch
                    :active-value="1"
                    :inactive-value="0"
                    :loading="publishToUserCenterLoading"
                    :model-value="publishToUserCenter"
                    @change="(val) => togglePublishToUserCenter(val as number)"
                  />
                </ElSpace>
                <ElButton
                  v-show="publishToUserCenter"
                  link
                  type="primary"
                  @click="openUserCenter"
                >
                  {{ $t('bot.openUserCenter') }}
                </ElButton>
              </div>
            </div>
          </ElCollapseItem>
          <ElCollapseItem :title="$t('bot.iframeEmbedding')">
            <div class="publish-wx-container">
              <div class="bg-background space-y-3 rounded-lg p-2">
                <p class="text-muted-foreground text-sm">
                  {{ $t('bot.iframeEmbeddingTooltip') }}
                </p>

                <div class="space-y-2">
                  <div class="text-sm font-medium">
                    {{ $t('bot.baseEmbeddingCode') }}
                  </div>
                  <ElInput
                    type="textarea"
                    :rows="4"
                    readonly
                    :model-value="iframeCode"
                  />
                  <ElButton
                    type="primary"
                    size="small"
                    @click="copyCode(iframeCode)"
                  >
                    {{ $t('bot.copyCode') }}
                  </ElButton>
                </div>

                <div class="space-y-2">
                  <div class="text-sm font-medium">
                    {{ $t('bot.fullEmbeddingCode') }}
                  </div>
                  <ElInput
                    type="textarea"
                    :rows="6"
                    readonly
                    :model-value="iframeFullCode"
                  />
                </div>
              </div>
            </div>
          </ElCollapseItem>
          <ElCollapseItem :title="$t('bot.sdkEmbedding')">
            <div class="publish-wx-container">
              <div class="bg-background space-y-3 rounded-lg p-2">
                <p class="text-muted-foreground text-sm">
                  {{ $t('bot.sdkEmbeddingTooltip') }}
                </p>

                <div class="space-y-2">
                  <div class="text-sm font-medium">
                    {{ $t('bot.sdkEmbeddingCode') }}
                  </div>
                  <ElInput
                    type="textarea"
                    :rows="8"
                    readonly
                    :model-value="sdkCode"
                  />
                  <ElButton
                    type="primary"
                    size="small"
                    @click="copyCode(sdkCode)"
                  >
                    {{ $t('bot.copyCode') }}
                  </ElButton>
                </div>
              </div>
            </div>
          </ElCollapseItem>
          <ElCollapseItem :title="$t('bot.apiEmbedding')">
            <div class="publish-wx-container">
              <div class="bg-background space-y-3 rounded-lg p-2">
                <p class="text-muted-foreground text-sm">
                  {{ $t('bot.apiEmbeddingTooltip') }}
                </p>
                <ElSpace size="small">
                  <span class="text-nowrap text-sm">{{
                    $t('bot.viewApiDoc')
                  }}</span>
                  <ElLink
                    href="https://api.aiflowy.tech/bot/chat"
                    target="_blank"
                    type="primary"
                  >
                    <ElSpace :size="4">
                      https://api.aiflowy.tech/bot/chat
                      <ElIcon>
                        <IconifyIcon icon="svg:target" />
                      </ElIcon>
                    </ElSpace>
                  </ElLink>
                </ElSpace>
              </div>
            </div>
          </ElCollapseItem>
        </ElCollapse>
      </div>
    </div>

    <!-- 选择插件-->
    <CommonSelectDataModal
      :title="$t('menus.ai.plugin')"
      width="730"
      ref="pluginToolDataRef"
      page-url="/api/v1/plugin/pageByCategory"
      :has-parent="true"
      :search-params="['name']"
      @get-data="confirmUpdateAiBotPlugin"
      :extra-query-params="{
        category: 0,
      }"
    />

    <!-- 选择知识库-->
    <CommonSelectDataModal
      :title="$t('menus.ai.documentCollection')"
      width="730"
      ref="knowledgeDataRef"
      page-url="/api/v1/documentCollection/page"
      @get-data="confirmUpdateAiBotKnowledge"
    />

    <!-- 选择工作流-->
    <CommonSelectDataModal
      :title="$t('menus.ai.workflow')"
      width="730"
      ref="workflowDataRef"
      page-url="/api/v1/workflow/page"
      @get-data="confirmUpdateAiBotWorkflow"
    />

    <!-- 选择MCP-->
    <CommonSelectDataModal
      :title="$t('menus.ai.mcp')"
      width="730"
      ref="mcpToolDataRef"
      page-url="/api/v1/mcp/pageTools?status=1"
      title-key="title"
      :has-parent="true"
      :is-select-mcp="true"
      @get-data="confirmUpdateAiBotMcp"
    />

    <!-- 选择 Wiki -->
    <WikiSelectModal
      title="Wiki"
      width="500"
      ref="wikiDataRef"
      @get-data="confirmUpdateAiBotWiki"
    />

    <!-- 选择 Skill -->
    <CommonSelectDataModal
      title="Skill"
      width="730"
      ref="skillDataRef"
      page-url="/api/v1/skill/page"
      title-key="title"
      description-key="description"
      :search-params="['title']"
      @get-data="confirmUpdateAiBotSkill"
    />

    <!--预设问题-->
    <ProblemPresupposition
      ref="problemPresuppositionRef"
      @success="handleProblemPresuppositionSuccess"
    />
    <PublishWxOfficalAccount ref="publishWxRef" @reload="getBotDetail()" />

    <!--工具模板配置-->
    <ToolTemplateConfigModal
      ref="toolTemplateConfigRef"
      v-model:visible="templateConfigVisible"
      :bot-id="botId"
      :tool-type="currentToolConfig.type"
      :tool-id="currentToolConfig.toolId"
      :record-id="currentToolConfig.recordId"
      :tool-name="currentToolConfig.name"
      :output-template="currentToolConfig.outputTemplate"
      @success="handleTemplateConfigSuccess"
    />
  </div>
</template>

<style scoped>
.config-container {
  width: 100%;
  height: 100%;
  min-height: 100%;
  overflow-y: auto;
  border-radius: 8px;
}

.collapse-right-container {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
}

.badge-circle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  vertical-align: middle;
  color: var(--el-color-white);
  background-color: var(--el-color-primary);
  border-radius: 50%;
}

.presetQues-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px 12px 12px;
  background-color: var(--el-bg-color);
  border-radius: 8px;
}

.preset-delete {
  cursor: pointer;
}

.config-font-style {
  font-family: PingFangSC, 'PingFang SC';
  font-size: 14px;
  font-weight: 500;
  line-height: 22px;
  color: var(--el-text-color-primary);
}

.options-config-item-left {
  flex: 0 0 auto;
}

:deep(.el-collapse-item__title) {
  font-size: 14px;
  font-weight: 500;
  line-height: 22px;
  color: var(--el-text-color-primary);
}

.options-config-item-middle {
  flex: 1;
  margin: 0 12px;
}

.options-config-item-right {
  flex: 0 0 auto;
  width: auto;
}

.options-config-item-switch {
  flex: 0 0 auto;
  margin-left: 12px;
  min-width: 49px;
}

.switch-placeholder {
  width: 49px;
  height: 22px;
}

:deep(.el-slider__runway) {
  height: 4px;
}

:deep(.el-slider__bar) {
  height: 4px;
}

:deep(.el-slider__button) {
  width: 14px;
  height: 14px;
}

.enable-think-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px 12px;
  background-color: var(--bot-collapse-itme-back);
}
.label-tooltip {
  display: inline-block;
  cursor: help;
}

.info-icon {
  flex: none;
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  font-size: 14px;
  color: #909399;
  cursor: help;
}

.compression-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background-color: var(--bot-collapse-itme-back);
}

.compression-config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background-color: var(--el-bg-color);
  border-radius: 8px;
}

.compression-config-item .config-label {
  display: flex;
  gap: 4px;
  align-items: center;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.compression-config-item .el-select,
.compression-config-item .dict-select {
  width: 200px;
}

.publish-wx-container {
  padding: 12px;
  background-color: var(--bot-collapse-itme-back);
}

.publish-wx {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 45px;
  padding: 0 8px;
  background-color: var(--el-bg-color);
  border-radius: 8px;
}

.publish-wx-right-container {
  display: flex;
  gap: 5px;
  align-items: center;
  justify-content: flex-end;
  cursor: pointer;
}

.publish-config-operation {
  display: flex;
  gap: 5px;
  align-items: center;
  justify-content: flex-end;
}

:deep(.el-collapse) {
  box-sizing: border-box;
  height: 100%;
  overflow: hidden !important;
  border-radius: 8px !important;
}

:deep(.el-collapse-item__header) {
  background-color: var(--bot-collapse-itme-back);
  border-bottom: 1px solid #e4e7ed;
  border-radius: 0;
}

.dark .el-collapse :deep(.el-collapse-item__header) {
  border-bottom: 1px solid hsl(var(--border));
}

:deep(.el-collapse-item:last-child .el-collapse-item__header) {
  border-bottom: none;
}

:deep(.el-collapse-icon-position-left .el-collapse-item__header) {
  padding: 8px;
}

.llm-back-container {
  background-color: var(--bot-collapse-itme-back);
}

.publish-container {
  flex: 1;
}

.el-list-item-delete-container {
  cursor: pointer;
}

:deep(.el-collapse-item__content) {
  height: 100%;
  padding: 0;
}

.question-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  height: auto;
  padding: 10px;
  background-color: var(--bot-collapse-itme-back);
}

/* 其他技能样式 */
.other-skills-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background-color: var(--bot-collapse-itme-back);
}

.other-skill-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background-color: var(--el-bg-color);
  border-radius: 8px;
}

.skill-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.skill-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.skill-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 搜索引擎技能样式 */
.search-engine-skill {
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}

.search-engine-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-engine-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-engine-select {
  width: 150px;
}

.settings-link {
  font-size: 12px;
  white-space: nowrap;
}
</style>
