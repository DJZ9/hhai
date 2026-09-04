import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    name: 'BotRun',
    path: '/ai/bots/run/:botId/:sessionId?',
    component: () => import('#/views/ai/bots/pages/Run.vue'),
    meta: {
      title: $t('menus.ai.bots'),
      noBasicLayout: true,
      openInNewWindow: true,
      hideInMenu: true,
      hideInBreadcrumb: true,
      hideInTab: true,
    },
  },
  {
    name: 'BotSetting',
    path: '/ai/bots/setting/:id',
    component: () => import('#/views/ai/bots/pages/setting/index.vue'),
    meta: {
      title: $t('menus.ai.bots'),
      openInNewWindow: true,
      hideInMenu: true,
      hideInBreadcrumb: true,
      hideInTab: true,
      activePath: '/ai/bots',
    },
  },
  {
    name: 'ConversationHistory',
    path: '/ai/conversationAnalysis/:id',
    component: () => import('#/views/ai/conversationAnalysis/history.vue'),
    meta: {
      title: $t('conversationAnalysis.conversationRecord'),
      openInNewWindow: true,
      hideInMenu: true,
      hideInBreadcrumb: true,
      hideInTab: true,
      activePath: '/ai/conversationAnalysis',
    },
  },
  {
    name: 'BotToolCallRecords',
    path: '/ai/bots/toolRecords/:botId',
    component: () => import('#/views/ai/bots/pages/toolRecords/index.vue'),
    meta: {
      title: $t('toolCallRecord.title'),
      openInNewWindow: true,
      hideInMenu: true,
      hideInBreadcrumb: true,
      hideInTab: true,
      activePath: '/ai/bots',
    },
  },
];

export default routes;
