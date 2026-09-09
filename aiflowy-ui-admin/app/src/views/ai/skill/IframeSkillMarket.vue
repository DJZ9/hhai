<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { $t } from '@aiflowy/locales';

import { ElButton, ElMessage } from 'element-plus';

import { api } from '#/api/request';

const router = useRouter();
const skillMarketUrl = ref('');
const skillMarketOrigin = ref('');

// 模块级幂等保护：防止 HMR 导致监听器堆积重复安装
const installedSkills = new Set<string>();

function getSkillMarketUrl() {
  api.get('/api/v1/sysOption/list?keys=skill_market_domain').then((res) => {
    const domain = res.errorCode === 0 ? res.data?.skill_market_domain : '';
    const finalDomain = domain || 'https://skill.aiflowy.tech';
    // 技能商城地址
    skillMarketUrl.value = `${finalDomain}/#/market/skill/list`;
    try {
      skillMarketOrigin.value = new URL(finalDomain).origin;
    } catch {
      skillMarketOrigin.value = finalDomain;
    }
  });
}

const handleInstallSkill = async (event: MessageEvent) => {
  if (event.data?.type !== 'INSTALL_SKILL') return;
  // 安全校验
  if (skillMarketOrigin.value && event.origin !== skillMarketOrigin.value) {
    return;
  }
  const { skill, downloadUrl } = event.data;
  if (!downloadUrl) {
    return;
  }
  const skillKey = `${skill.title || skill.name}_${skill.version}`;

  if (installedSkills.has(skillKey)) {
    ElMessage.info($t('skill.message.installInProgress', { title: skill.title }));
    return;
  }
  installedSkills.add(skillKey);
  try {
    let absoluteDownloadUrl = downloadUrl;
    if (!/^https?:\/\//i.test(absoluteDownloadUrl)) {
      if (!skillMarketOrigin.value) {
        ElMessage.error('技能商城地址未加载完成，请稍后重试');
        return;
      }
      const origin = skillMarketOrigin.value.replace(/\/$/, '');
      const path = absoluteDownloadUrl.startsWith('/') ? absoluteDownloadUrl : `/${absoluteDownloadUrl}`;
      absoluteDownloadUrl = `${origin}${path}`;
    }
    ElMessage.info($t('skill.message.installing', { title: skill.title }));
    const result: any = await api.post(
      `/api/v1/skill/installFromMarket?downloadUrl=${encodeURIComponent(absoluteDownloadUrl)}`,
      skill,
    );
    if (result.errorCode === 0) {
      ElMessage.success($t('skill.message.installSuccess', { title: skill.title }));
      router.back();
    } 
  } catch (error: any) {
  } finally {
    installedSkills.delete(skillKey);
  }
};

const goBack = () => {
  router.push('/skill');
};

onMounted(() => {
  getSkillMarketUrl();
  window.addEventListener('message', handleInstallSkill);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handleInstallSkill);
});
</script>

<template>
  <div class="skill-market-page">
    <div class="skill-market-header">
      <h3 class="skill-market-title">{{ $t('skill.actions.skillMarket') }}</h3>
      <span class="skill-market-install-hint">
        {{ $t('skill.marketInstallHint') }}
      </span>
      <ElButton class="skill-market-close" @click="goBack">
        {{ $t('skill.closeMarket') }}
      </ElButton>
    </div>
    <iframe class="skill-market-iframe" :src="skillMarketUrl" frameborder="0" allow="clipboard-write" />
  </div>
</template>

<style scoped>
.skill-market-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.skill-market-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #3d6fff 0%, #5b8def 100%);
  color: #fff;
  flex-shrink: 0;
}

.skill-market-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  white-space: nowrap;
}

.skill-market-install-hint {
  flex: 1;
  font-size: 13px;
  opacity: 0.85;
  text-align: center;
}

.skill-market-close {
  flex-shrink: 0;
  border-radius: 8px !important;
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #fff !important;
  font-weight: 600;
}

.skill-market-close:hover {
  background: rgba(255, 255, 255, 0.3) !important;
}

.skill-market-iframe {
  flex: 1;
  width: 96%;
  border: none;
  margin: 0 20px;
}
</style>
