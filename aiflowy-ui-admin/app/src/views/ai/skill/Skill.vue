<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import type { ActionButton } from '#/components/page/CardList.vue';

import { computed, onActivated, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { $t } from '@aiflowy/locales';
import { useAccessStore } from '@aiflowy/stores';
import { useAppConfig } from '@aiflowy/hooks';

import { Delete, Download, Edit, Plus, ShoppingCart } from '@element-plus/icons-vue';
import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
} from 'element-plus';
import { tryit } from 'radash';

import { api } from '#/api/request';
import defaultIcon from '#/assets/ai/plugin/defaultPluginIcon.png';
import HeaderSearch from '#/components/headerSearch/HeaderSearch.vue';
import CardPage from '#/components/page/CardList.vue';
import PageData from '#/components/page/PageData.vue';
import PageSide from '#/components/page/PageSide.vue';
import SkillModal from '#/views/ai/skill/SkillModal.vue';

const accessStore = useAccessStore();
const router = useRouter();
const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

interface FieldDefinition {
  prop: string;
  label: string;
  type?: 'input' | 'number';
  required?: boolean;
  placeholder?: string;
}

// 操作按钮配置
const actions: ActionButton[] = [
  {
    icon: Edit,
    text: $t('button.edit'),
    className: '',
    permission: '/api/v1/skill/save',
    onClick(row) {
      skillModalRef.value.openDialog(row);
    },
  },
  {
    icon: Download,
    text: $t('skill.actions.download'),
    className: '',
    permission: '/api/v1/skill/query',
    onClick(row) {
      handleDownload(row);
    },
  },
  {
    text: $t('button.delete'),
    icon: Delete,
    className: 'item-danger',
    permission: '/api/v1/skill/remove',
    onClick(row) {
      handleDelete(row);
    },
  },
];

onMounted(() => {
  getCategoryList();
  getMarketConfig();
});

// 是否显示技能商城入口，由系统设置 skill_market_enabled 控制，默认不显示
const marketVisible = ref(false);

const getMarketConfig = () => {
  api.get('/api/v1/sysOption/list?keys=skill_market_enabled').then((res) => {
    if (res.errorCode === 0) {
      marketVisible.value = res.data?.skill_market_enabled === 'true';
    }
  });
};

onActivated(() => {
  // 从技能商城返回时刷新列表
  if (pageDataRef.value) {
    pageDataRef.value.setQuery({ categoryId: currentCategoryId.value });
  }
});

const handleDelete = (item: any) => {
  ElMessageBox.confirm($t('message.deleteAlert'), $t('message.noticeTitle'), {
    confirmButtonText: $t('message.ok'),
    cancelButtonText: $t('message.cancel'),
    type: 'warning',
  })
    .then(() => {
      api
        .post('/api/v1/skill/remove', { id: item.id })
        .then((res) => {
          if (res.errorCode === 0) {
            ElMessage.success($t('message.deleteOkMessage'));
            pageDataRef.value.setQuery({ categoryId: currentCategoryId.value });
          } else {
            ElMessage.error(res.message || $t('message.deleteFailMessage'));
          }
        })
        .catch(() => {
          ElMessage.error($t('message.networkError'));
        });
    })
    .catch(() => { });
};

async function handleDownload(row: any) {
  const url = `${apiURL}/api/v1/skill/download/${row.id}`;
  try {
    const response = await fetch(url, {
      headers: { 'aiflowy-token': accessStore.accessToken } as any,
    });
    if (!response.ok) throw new Error('下载失败');
    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = `${row.name || row.title || 'skill'}.zip`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(downloadUrl);
  } catch (error) {
    ElMessage.error('下载失败');
  }
}

const pageDataRef = ref();
const skillModalRef = ref();
const currentCategoryId = ref<string>('');

const headerButtons = computed(() => {
  const buttons = [
    {
      key: 'list',
      text: $t('skill.actions.skillMarket'),
      icon: ShoppingCart,
      type: 'primary',
      data: { action: 'list' },
      permission: '/api/v1/skill/query',
    },
    {
      key: 'add',
      text: $t('skill.actions.addSkill'),
      icon: Plus,
      type: 'primary',
      data: { action: 'add' },
      permission: '/api/v1/skill/save',
    },
  ];
  return marketVisible.value
    ? buttons
    : buttons.filter((item) => item.key !== 'list');
});

const handleButtonClick = (event: any, _item: any) => {
  switch (event.key) {
    case 'list': {
      // 跳转到技能商城路由（商城为独立页面，内部以 iframe 嵌入）
      router.push({ name: 'IframeSkillMarket' });
      break;
    }
    case 'add': {
      skillModalRef.value.openDialog({});
      break;
    }
  }
};

const fieldDefinitions = ref<FieldDefinition[]>([
  {
    prop: 'categoryName',
    label: $t('aiWorkflowCategory.categoryName'),
    type: 'input',
    required: true,
    placeholder: $t('aiWorkflowCategory.categoryName'),
  },
  {
    prop: 'sortNo',
    label: $t('aiWorkflowCategory.sortNo'),
    type: 'number',
    required: false,
    placeholder: $t('aiWorkflowCategory.sortNo'),
  },
]);

const formRules = computed(() => {
  const rules: Record<string, any[]> = {};
  fieldDefinitions.value.forEach((field) => {
    const fieldRules = [];
    if (field.required) {
      fieldRules.push({
        required: true,
        message: `${$t('message.required')}`,
        trigger: 'blur',
      });
    }
    if (fieldRules.length > 0) {
      rules[field.prop] = fieldRules;
    }
  });
  return rules;
});

const handleSearch = (params?: any) => {
  if (params !== undefined && params !== null) {
    pageDataRef.value.setQuery({ title: params, isQueryOr: true, categoryId: currentCategoryId.value });
  } else {
    pageDataRef.value.setQuery({ categoryId: currentCategoryId.value });
  }
};

const formData = ref<any>({});
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();

function showControlDialog(item: any) {
  formRef.value?.resetFields();
  formData.value = { ...item };
  dialogVisible.value = true;
}

const categoryList = ref<any[]>([]);
const categoryLoading = ref(true);

const getCategoryList = async () => {
  categoryLoading.value = true;
  const [, res] = await tryit(api.get)('/api/v1/skillCategory/list', {
    params: { sortKey: 'sortNo', sortType: 'asc' },
  });

  if (res && res.errorCode === 0) {
    categoryList.value = [
      {
        id: '',
        categoryName: $t('common.allCategories'),
      },
      ...res.data,
    ];
  }
  categoryLoading.value = false;
};

function removeCategory(row: any) {
  ElMessageBox.confirm($t('message.deleteAlert'), $t('message.noticeTitle'), {
    confirmButtonText: $t('message.ok'),
    cancelButtonText: $t('message.cancel'),
    type: 'warning',
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        instance.confirmButtonLoading = true;
        api
          .post('/api/v1/skillCategory/remove', { id: row.id })
          .then((res) => {
            instance.confirmButtonLoading = false;
            if (res.errorCode === 0) {
              ElMessage.success(res.message);
              done();
              getCategoryList();
            }
          })
          .catch(() => {
            instance.confirmButtonLoading = false;
          });
      } else {
        done();
      }
    },
  }).catch(() => { });
}

const controlBtns = [
  {
    icon: Edit,
    label: $t('button.edit'),
    onClick(row: any) {
      showControlDialog(row);
    },
  },
  {
    type: 'danger',
    icon: Delete,
    label: $t('button.delete'),
    onClick(row: any) {
      removeCategory(row);
    },
  },
];

const footerButton = {
  icon: Plus,
  label: $t('button.add'),
  onClick() {
    showControlDialog({});
  },
};

const saveLoading = ref(false);

function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (valid) {
      saveLoading.value = true;
      const url = formData.value.id
        ? '/api/v1/skillCategory/update'
        : '/api/v1/skillCategory/save';
      api.post(url, formData.value).then((res) => {
        saveLoading.value = false;
        if (res.errorCode === 0) {
          getCategoryList();
          ElMessage.success(res.message);
          dialogVisible.value = false;
        }
      });
    }
  });
}

function changeCategory(category: any) {
  currentCategoryId.value = category.id;
  pageDataRef.value.setQuery({ categoryId: category.id });
}
</script>

<template>
  <div class="flex h-full flex-col gap-6 p-6">
    <div class="skill-header">
      <HeaderSearch :buttons="headerButtons" @search="handleSearch" @button-click="handleButtonClick" />
    </div>
    <div class="flex max-h-[calc(100vh-191px)] flex-1 gap-6">
      <PageSide label-key="categoryName" value-key="id" :menus="categoryList" :control-btns="controlBtns"
        :footer-button="footerButton" :loading="categoryLoading" @change="changeCategory" />
      <div class="h-full flex-1 overflow-auto">
        <PageData ref="pageDataRef" page-url="/api/v1/skill/page" :page-size="12" :page-sizes="[12, 24, 36, 48]"
          :init-query-params="{ status: 1 }">
          <template #default="{ pageList }">
            <CardPage :default-icon="defaultIcon" title-key="title" avatar-key="icon" description-key="description"
              :data="pageList" :actions="actions" />
          </template>
        </PageData>
      </div>
    </div>
    <ElDialog v-model="dialogVisible" :title="formData.id ? `${$t('button.edit')}` : `${$t('button.add')}`"
      :close-on-click-modal="false">
      <ElForm ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <ElFormItem v-for="field in fieldDefinitions" :key="field.prop" :label="field.label" :prop="field.prop">
          <ElInput v-if="!field.type || field.type === 'input'" v-model="formData[field.prop]"
            :placeholder="field.placeholder" />
          <ElInputNumber v-else-if="field.type === 'number'" v-model="formData[field.prop]"
            :placeholder="field.placeholder" style="width: 100%" />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElButton @click="dialogVisible = false">
          {{ $t('button.cancel') }}
        </ElButton>
        <ElButton type="primary" @click="handleSubmit" :loading="saveLoading">
          {{ $t('button.confirm') }}
        </ElButton>
      </template>
    </ElDialog>

    <!-- Skill 上传模态框 -->
    <SkillModal ref="skillModalRef" @reload="handleSearch" />
  </div>
</template>

<style scoped>
h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
}
</style>
