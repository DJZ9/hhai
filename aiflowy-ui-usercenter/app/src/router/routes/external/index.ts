import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    name: 'IframeChat',
    path: '/iframe/chat',
    component: () => import('#/views/iframe/chat.vue'),
    meta: {
      title: '聊天',
      ignoreAccess: true,
    },
  },
];

export default routes;
