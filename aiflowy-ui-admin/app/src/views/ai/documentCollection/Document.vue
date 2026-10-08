<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { $t } from '@aiflowy/locales';

import { ArrowLeft, Plus, Refresh } from '@element-plus/icons-vue';
import { ElIcon, ElImage } from 'element-plus';

import { api } from '#/api/request';
import bookIcon from '#/assets/ai/knowledge/book.svg';
import HeaderSearch from '#/components/headerSearch/HeaderSearch.vue';
import PageSide from '#/components/page/PageSide.vue';
import ChunkDocumentTable from '#/views/ai/documentCollection/ChunkDocumentTable.vue';
import DocumentCollectionDataConfig from '#/views/ai/documentCollection/DocumentCollectionDataConfig.vue';
import DocumentTable from '#/views/ai/documentCollection/DocumentTable.vue';
import ImportKnowledgeDocFile from '#/views/ai/documentCollection/ImportKnowledgeDocFile.vue';
import KnowledgeSearch from '#/views/ai/documentCollection/KnowledgeSearch.vue';
import KnowledgeSearchConfig from '#/views/ai/documentCollection/KnowledgeSearchConfig.vue';

const route = useRoute();
const router = useRouter();

const knowledgeId = ref<string>((route.query.id as string) || '');
const activeMenu = ref<string>((route.query.activeMenu as string) || '');
const knowledgeInfo = ref<any>({});
const getKnowledge = () => {
  api
    .get('/api/v1/documentCollection/detail', {
      params: { id: knowledgeId.value },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        knowledgeInfo.value = res.data;
      }
    });
};
onMounted(() => {
  if (activeMenu.value) {
    defaultSelectedMenu.value = activeMenu.value;
  }
  getKnowledge();
});
const back = () => {
  router.push({ path: '/ai/documentCollection' });
};
const categoryData = [
  { key: 'documentList', name: $t('documentCollection.documentList') },
  { key: 'knowledgeSearch', name: $t('documentCollection.knowledgeRetrieval') },
  { key: 'config', name: $t('documentCollection.config') },
];
const headerButtons = [
  {
    key: 'importFile',
    text: $t('button.importFile'),
    icon: Plus,
    type: 'primary',
    data: { action: 'importFile' },
  },
  {
    key: 'refresh',
    text: $t('button.refresh'),
    icon: Refresh,
    type: 'default',
    data: { action: 'refresh' },
  },
];
const isImportFileVisible = ref(false);
const selectedCategory = ref('documentList');
const documentTableRef = ref();
const handleSearch = (searchParams: string) => {
  documentTableRef.value.search(searchParams);
};
const handleButtonClick = (event: any) => {
  // 根据按钮 key 执行不同操作
  switch (event.key) {
    case 'back': {
      router.push({ path: '/ai/knowledge' });
      break;
    }
    case 'importFile': {
      isImportFileVisible.value = true;
      break;
    }
    case 'refresh': {
      documentTableRef.value?.reload();
      break;
    }
  }
};
const handleCategoryClick = (category: any) => {
  selectedCategory.value = category.key;
  viewDocVisible.value = false;
};
const viewDocVisible = ref(false);
const documentId = ref('');
// 子组件传递事件，显示查看文档详情
const viewDoc = (docId: string) => {
  viewDocVisible.value = true;
  documentId.value = docId;
};
const backDoc = async () => {
  isImportFileVisible.value = false;
  // 返回列表后刷新，便于看到新提交文档的解析状态
  await nextTick();
  documentTableRef.value?.reload();
};
const defaultSelectedMenu = ref('documentList');
</script>

<template>
  <div class="document-container">
    <div v-if="!isImportFileVisible" class="doc-header-container">
      <div class="doc-knowledge-container">
        <div @click="back()" style="cursor: pointer">
          <ElIcon><ArrowLeft /></ElIcon>
        </div>
        <div>
          <ElImage :src="bookIcon" style="width: 36px; height: 36px" />
        </div>
        <div class="knowledge-info-container">
          <div class="title">{{ knowledgeInfo.title || '' }}</div>
          <div class="description">
            {{ knowledgeInfo.description || '' }}
          </div>
        </div>
      </div>
      <div class="doc-content">
        <div>
          <PageSide
            label-key="name"
            value-key="key"
            :menus="categoryData"
            :default-selected="defaultSelectedMenu"
            @change="handleCategoryClick"
          />
        </div>
        <div
          class="doc-table-content menu-container border border-[var(--el-border-color)]"
        >
          <div v-if="selectedCategory === 'documentList'" class="doc-table">
            <div class="doc-header" v-if="!viewDocVisible">
              <HeaderSearch
                :buttons="headerButtons"
                @search="handleSearch"
                @button-click="handleButtonClick"
              />
            </div>
            <DocumentTable
              ref="documentTableRef"
              :knowledge-id="knowledgeId"
              @view-doc="viewDoc"
              v-if="!viewDocVisible"
            />

            <ChunkDocumentTable
              v-else
              :document-id="documentId"
              :default-summary-prompt="knowledgeInfo.summaryPrompt"
            />
          </div>
          <!--知识检索-->
          <div
            v-if="selectedCategory === 'knowledgeSearch'"
            class="doc-search-container"
          >
            <KnowledgeSearchConfig :document-collection-id="knowledgeId" />
            <KnowledgeSearch :knowledge-id="knowledgeId" />
          </div>
          <!--配置-->
          <div v-if="selectedCategory === 'config'">
            <DocumentCollectionDataConfig
              :detail-data="knowledgeInfo"
              @reload="getKnowledge"
            />
          </div>
        </div>
      </div>
    </div>
    <div v-else class="doc-imp-container">
      <ImportKnowledgeDocFile @import-back="backDoc" />
    </div>
  </div>
</template>

<style scoped>
.document-container {
  display: flex;
  width: 100%;
  height: 100%;
  padding: 24px 24px 30px;
}

.doc-container {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

.doc-table-content {
  box-sizing: border-box;
  flex: 1;
  width: 100%;
  padding: 20px 14px 0;
  background-color: var(--el-bg-color);
  border-radius: 8px;
}

.doc-header {
  width: 100%;
  padding-bottom: 21px;
  margin: 0 auto;
}

.doc-content {
  display: flex;
  flex-direction: row;
  gap: 12px;
  width: 100%;
  height: 100%;
}

.doc-table {
  background-color: var(--el-bg-color);
}

.doc-imp-container {
  box-sizing: border-box;
  flex: 1;
  width: 100%;
}

.doc-header-container {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.doc-knowledge-container {
  display: flex;
  flex-direction: row;
  gap: 8px;
  align-items: center;
  margin-bottom: 20px;
}

.knowledge-info-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title {
  font-size: 16px;
  font-style: normal;
  font-weight: 500;
  line-height: 24px;
  text-align: left;
  text-transform: none;
}

.description {
  font-size: 14px;
  font-style: normal;
  font-weight: 400;
  line-height: 22px;
  color: #75808d;
  text-align: left;
  text-transform: none;
}

.doc-search-container {
  display: flex;
  width: 100%;
  height: 100%;
}

.menu-container {
  flex: 1;
}
</style>
