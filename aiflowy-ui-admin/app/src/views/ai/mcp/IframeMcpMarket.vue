<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { ElButton, ElMessage } from 'element-plus';

import { api } from '#/api/request';

const router = useRouter();
const mcpMarketUrl = ref('');
const mcpMarketOrigin = ref('');
const installedMcps = new Set<string>();

function getMcpMarketUrl() {
  api.get('/api/v1/sysOption/list?keys=skill_market_domain').then((res) => {
    const domain = res.errorCode === 0 ? res.data?.skill_market_domain : '';
    const finalDomain = domain || 'http://skill.aiflowy.tech';
    mcpMarketUrl.value = `${finalDomain}/#/market/mcp/list`;
    console.log('finalDomain', mcpMarketUrl.value);
    try {
      mcpMarketOrigin.value = new URL(finalDomain).origin;
    } catch {
      mcpMarketOrigin.value = finalDomain;
    }
  });
}

const handleInstallMcp = async (event: MessageEvent) => {
  if (event.data?.type !== 'INSTALL_MCP') return;
  if (mcpMarketOrigin.value && event.origin !== mcpMarketOrigin.value) {
    return;
  }
  const { mcp } = event.data;
  if (!mcp?.configJson) {
    ElMessage.error('MCP 配置为空，无法安装');
    return;
  }
  const mcpKey = `${mcp.title}_${mcp.configJson}`;

  if (installedMcps.has(mcpKey)) {
    ElMessage.info(`正在安装 ${mcp.title}，请稍候...`);
    return;
  }
  installedMcps.add(mcpKey);
  const mcpEntity = {
    title: mcp.title || '',
    description: mcp.description || '',
    configJson: mcp.configJson,
    status: mcp.status !== undefined ? mcp.status : true,
  };

  ElMessage.info(`正在安装 MCP「${mcp.title}」到本地实例...`);

  try {
    const result: any = await api.post('api/v1/mcp/save', mcpEntity);
    if (result.errorCode === 0) {
      ElMessage.success(`MCP「${mcp.title}」安装成功`);
      event.source?.postMessage(
        {
          type: 'INSTALL_MCP_RESULT',
          success: true,
          message: `MCP「${mcp.title}」已成功安装到本地实例`,
        },
        event.origin as any,
      );
    } else {
      const failMsg = result.message || '安装失败';
      ElMessage.error(failMsg);
      event.source?.postMessage(
        {
          type: 'INSTALL_MCP_RESULT',
          success: false,
          message: failMsg,
        },
        event.origin as any,
      );
    }
  } catch (error: any) {
    const errMsg = error?.message || '网络错误，安装失败';
    ElMessage.error(errMsg);
    event.source?.postMessage(
      {
        type: 'INSTALL_MCP_RESULT',
        success: false,
        message: errMsg,
      },
      event.origin as any,
    );
  } finally {
    installedMcps.delete(mcpKey);
  }
};

const goBack = () => {
  router.push('/ai/mcp');
};

onMounted(() => {
  getMcpMarketUrl();
  window.addEventListener('message', handleInstallMcp);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handleInstallMcp);
});
</script>

<template>
  <div class="mcp-market-page">
    <div class="mcp-market-header">
      <h3 class="mcp-market-title">MCP 市场</h3>
      <span class="mcp-market-install-hint">
        浏览市场中的 MCP，点击「安装到本地实例」可将 MCP 一键添加到当前系统
      </span>
      <ElButton class="mcp-market-close" @click="goBack">
        返回 MCP 管理
      </ElButton>
    </div>
    <iframe class="mcp-market-iframe" :src="mcpMarketUrl" frameborder="0" allow="clipboard-write" />
  </div>
</template>

<style scoped>
.mcp-market-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mcp-market-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #3d6fff 0%, #5b8def 100%);
  color: #fff;
  flex-shrink: 0;
}

.mcp-market-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  white-space: nowrap;
}

.mcp-market-install-hint {
  flex: 1;
  font-size: 13px;
  opacity: 0.85;
  text-align: center;
}

.mcp-market-close {
  flex-shrink: 0;
  border-radius: 8px !important;
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #fff !important;
  font-weight: 600;
}

.mcp-market-close:hover {
  background: rgba(255, 255, 255, 0.3) !important;
}

.mcp-market-iframe {
  flex: 1;
  width: 100%;
  border: none;
}
</style>
