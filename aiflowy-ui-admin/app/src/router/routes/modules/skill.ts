import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      title: $t('skill.actions.skillMarket'),
      hideInMenu: true,
      hideInTab: true,
      hideInBreadcrumb: true,
      fullPathKey: true,
    },
    name: 'IframeSkillMarket',
    path: '/ai/skill/iframe',
    component: () => import('#/views/ai/skill/IframeSkillMarket.vue'),
  },
  {
    meta: {
      title: $t('mcp.mcpMarket'),
      hideInMenu: true,
      hideInTab: true,
      hideInBreadcrumb: true,
      fullPathKey: true,
    },
    name: 'IframeMcpMarket',
    path: '/ai/mcp/iframe',
    component: () => import('#/views/ai/mcp/IframeMcpMarket.vue'),
  }
];

export default routes;
