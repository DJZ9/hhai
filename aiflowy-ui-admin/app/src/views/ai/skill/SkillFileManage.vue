<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { $t } from '@aiflowy/locales';

import {
  ArrowLeft,
  Delete,
  DocumentAdd,
  Edit,
  Folder,
  View,
} from '@element-plus/icons-vue';
import {
  ElButton,
  ElEmpty,
  ElIcon,
  ElMessage,
  ElMessageBox,
  ElRadioButton,
  ElRadioGroup,
  ElScrollbar,
  ElSkeleton,
  ElSkeletonItem,
  ElTag,
  ElTree,
} from 'element-plus';

import { api } from '#/api/request';
import MarkDownRenderer from '#/components/render/MarkDownRenderer.vue';

type TreeNode = {
  children?: TreeNode[];
  isFile: boolean;
  label: string;
  path: string;
  size?: number;
};

const route = useRoute();
const router = useRouter();
const skillId = route.params.skillId as string;

const title = ref(String(route.query.title || '') || $t('skill.fileManage'));

const treeData = ref<TreeNode[]>([]);
const treeLoading = ref(false);
const treeRef = ref();

const selectedPath = ref('');
const fileContent = ref<any>(null);
const contentLoading = ref(false);
const saving = ref(false);

const editorMode = ref<'edit' | 'preview'>('preview');
// 编辑模式下的原始文本（不做 markdown 渲染，保证 front-matter 等原样保留）
const rawContent = ref('');

function goBack() {
  router.back();
}

onMounted(() => {
  loadTree();
});

/** 递归查找 SKILL.md 节点及其祖先目录路径（默认收起时需展开祖先） */
function findSkillMd(
  nodes: TreeNode[],
  ancestors: string[],
): null | { dirs: string[]; path: string } {
  for (const node of nodes) {
    if (node.isFile) {
      if (node.label.toLowerCase() === 'skill.md') {
        return { path: node.path, dirs: ancestors };
      }
    } else if (node.children?.length) {
      const found = findSkillMd(node.children, [...ancestors, node.path]);
      if (found) {
        return found;
      }
    }
  }
  return null;
}

/** 树默认全部收起，仅展开 SKILL.md 的祖先目录并默认选中该文件 */
function selectSkillMdAfterLoad(data: TreeNode[]) {
  const target = findSkillMd(data ?? [], []);
  if (!target || !treeRef.value) {
    return;
  }
  for (const dir of target.dirs) {
    treeRef.value.setExpanded(dir, true);
  }
  nextTick(() => {
    treeRef.value?.setCurrentKey(target.path);
    selectedPath.value = target.path;
    loadContent(target.path);
  });
}

function loadTree() {
  if (!skillId) {
    return;
  }
  treeLoading.value = true;
  api
    .get(`/api/v1/skill/file/tree/${skillId}`)
    .then((res: any) => {
      if (res.errorCode === 0) {
        treeData.value = res.data || [];
      } else {
        ElMessage.error(res.message || $t('skill.fileTreeLoadFailed'));
      }
    })
    .finally(() => {
      treeLoading.value = false;
      // ElTree 依赖 v-else 在 treeLoading=false 后才渲染，须等其挂载完成再默认选中 SKILL.md
      nextTick(() => selectSkillMdAfterLoad(treeData.value));
    });
}

function onNodeClick(data: TreeNode) {
  if (!data || !data.isFile) {
    return;
  }
  selectedPath.value = data.path;
  loadContent(data.path);
}

function loadContent(path: string) {
  if (!path || !skillId) {
    return;
  }
  contentLoading.value = true;
  api
    .get('/api/v1/skill/file/content', {
      params: { skillId, path },
    })
    .then((res: any) => {
      if (res.errorCode === 0) {
        fileContent.value = res.data;
        rawContent.value = res.data?.content ?? '';
      } else {
        ElMessage.error(res.message || $t('skill.fileReadFailed'));
      }
    })
    .finally(() => {
      contentLoading.value = false;
    });
}

async function saveContent() {
  if (!skillId || !selectedPath.value) {
    return;
  }
  saving.value = true;
  try {
    const res: any = await api.post('/api/v1/skill/file/content', {
      skillId,
      path: selectedPath.value,
      content: rawContent.value,
    });
    if (res.errorCode === 0) {
      ElMessage.success($t('skill.saveSuccess'));
      // 同步预览内容为刚保存的原文
      fileContent.value = { ...fileContent.value, content: rawContent.value };
    } else {
      ElMessage.error(res.message || $t('skill.saveFailed'));
    }
  } finally {
    saving.value = false;
  }
}

function newFile() {
  ElMessageBox.prompt($t('skill.inputFilePath'), $t('skill.newFile'), {
    inputPlaceholder: 'config.yaml 或 sub/config.yaml',
    confirmButtonText: $t('button.confirm'),
    cancelButtonText: $t('button.cancel'),
  })
    .then(async ({ value }) => {
      const path = (value || '').trim();
      if (!path || !skillId) {
        ElMessage.warning($t('skill.inputFilePath'));
        return;
      }
      const res: any = await api.post('/api/v1/skill/file/content', {
        skillId,
        path,
        content: '',
      });
      if (res.errorCode === 0) {
        ElMessage.success($t('skill.saveSuccess'));
        loadTree();
      } else {
        ElMessage.error(res.message || $t('skill.saveFailed'));
      }
    })
    .catch(() => {});
}

function deleteNode() {
  if (!skillId || !selectedPath.value) {
    ElMessage.warning($t('skill.pleaseSelectFile'));
    return;
  }
  ElMessageBox.confirm(
    $t('skill.deleteFileConfirm'),
    $t('message.noticeTitle'),
    {
      confirmButtonText: $t('button.confirm'),
      cancelButtonText: $t('button.cancel'),
      type: 'warning',
    },
  )
    .then(async () => {
      const res: any = await api.delete('/api/v1/skill/file/content', {
        params: { skillId, path: selectedPath.value },
      });
      if (res.errorCode === 0) {
        ElMessage.success($t('skill.deleteOk'));
        selectedPath.value = '';
        fileContent.value = null;
        loadTree();
      } else {
        ElMessage.error(res.message || $t('skill.deleteFailed'));
      }
    })
    .catch(() => {});
}
</script>

<template>
  <div class="skill-file-page">
    <!-- 顶部标题栏 -->
    <div class="skill-file-header">
      <ElButton text :icon="ArrowLeft" @click="goBack">
        {{ $t('button.back') }}
      </ElButton>
      <span class="skill-file-title"
        >{{ $t('skill.fileManage') }} - {{ title }}</span
      >
      <span class="skill-file-header-spacer"></span>
    </div>

    <!-- 主体：左树右内容 -->
    <div class="skill-file-body">
      <!-- 左侧文件树 -->
      <div class="file-tree">
        <div class="file-tree-header">
          <span class="file-tree-title">{{ $t('skill.fileTree') }}</span>
          <div class="file-tree-actions">
            <ElButton
              type="danger"
              size="small"
              :icon="Delete"
              :disabled="!selectedPath"
              @click="deleteNode"
            >
              {{ $t('skill.deleteFile') }}
            </ElButton>
            <ElButton
              size="small"
              type="primary"
              :icon="DocumentAdd"
              @click="newFile"
            >
              {{ $t('skill.newFile') }}
            </ElButton>
          </div>
        </div>
        <ElScrollbar class="file-tree-body">
          <div
            v-if="treeLoading"
            class="file-tree-skeleton flex flex-col gap-2 p-1"
          >
            <ElSkeleton v-for="i in 6" :key="i" :rows="0" animated>
              <template #template>
                <div class="flex items-center gap-2 py-1">
                  <ElSkeletonItem variant="circle" :size="18" />
                  <ElSkeletonItem variant="text" style="width: 55%" />
                </div>
              </template>
            </ElSkeleton>
          </div>
          <ElTree
            v-else
            ref="treeRef"
            :data="treeData"
            :props="{ children: 'children', label: 'label' }"
            node-key="path"
            highlight-current
            @node-click="onNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <ElIcon>
                  <Folder v-if="!data.isFile" />
                  <DocumentAdd v-else />
                </ElIcon>
                <span class="tree-node-label">{{ data.label }}</span>
              </span>
            </template>
          </ElTree>
        </ElScrollbar>
      </div>

      <!-- 右侧内容区 -->
      <div class="file-content">
        <div class="file-content-toolbar">
          <div class="mode-switch">
            <ElRadioGroup v-model="editorMode" size="small">
              <ElRadioButton value="preview">
                <ElIcon><View /></ElIcon>
                {{ $t('skill.preview') }}
              </ElRadioButton>
              <ElRadioButton value="edit">
                <ElIcon><Edit /></ElIcon>
                {{ $t('skill.edit') }}
              </ElRadioButton>
            </ElRadioGroup>
          </div>
          <div class="toolbar-actions">
            <ElTag v-if="selectedPath" size="small" type="info">
              {{ selectedPath }}
            </ElTag>
            <ElButton
              v-if="
                editorMode === 'edit' &&
                fileContent &&
                !fileContent.isBinary &&
                selectedPath
              "
              :loading="saving"
              type="primary"
              size="small"
              @click="saveContent"
            >
              {{ $t('skill.saveContent') }}
            </ElButton>
          </div>
        </div>

        <div v-loading="contentLoading" class="file-content-body">
          <ElEmpty
            v-if="!selectedPath"
            :description="$t('skill.pleaseSelectFile')"
          />
          <ElEmpty
            v-else-if="fileContent?.isBinary"
            :description="$t('skill.binaryNotPreviewable')"
          />
          <div
            v-else-if="fileContent && editorMode === 'preview'"
            class="markdown-preview"
          >
            <MarkDownRenderer :content="fileContent.content" />
          </div>
          <textarea
            v-else-if="fileContent"
            v-model="rawContent"
            class="raw-editor"
            spellcheck="false"
          ></textarea>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.skill-file-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  padding: 16px;
  box-sizing: border-box;
  overflow: hidden;
}

.skill-file-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.skill-file-header-spacer {
  flex: 1;
}

.skill-file-title {
  font-size: 16px;
  font-weight: 600;
}

.skill-file-body {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.file-tree {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-self: stretch;
  background: var(--app-bg-color, #fff);
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 6px;
  overflow: hidden;
}

.file-tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-bottom: 1px solid var(--el-border-color, #dcdfe6);
}

.file-tree-title {
  font-weight: 600;
  font-size: 13px;
}

.file-tree-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-tree-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 6px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.tree-node-label {
  margin-left: 2px;
}

.file-tree-skeleton {
  padding: 4px;
}

.file-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 6px;
  overflow: hidden;
}

.file-content-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-bottom: 1px solid var(--el-border-color, #dcdfe6);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-content-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

.markdown-preview {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  padding: 12px 16px;
  overflow: auto;
}

/* 覆盖共享渲染器里 h2 的蓝色竖条，避免本页预览出现突兀色块 */
.markdown-preview :deep(h2) {
  border-left: none;
  padding-left: 0;
}

.raw-editor {
  display: block;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border: none;
  outline: none;
  resize: none;
  overflow: auto;
  tab-size: 2;
  background: transparent;
  color: var(--el-text-color-primary, #303133);
  font-family: 'JetBrains Mono', Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.7;
}
</style>
