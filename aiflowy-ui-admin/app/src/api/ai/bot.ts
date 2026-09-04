import type {
  AiLlm,
  BotInfo,
  ChatMessage,
  RequestResult,
  Session,
} from '@aiflowy/types';

import { api } from '#/api/request.js';

/** 获取bot详情 */
export const getBotDetails = (id: string) => {
  return api.get<RequestResult<BotInfo>>('/api/v1/bot/getDetail', {
    params: { id },
  });
};

export interface GetSessionListParams {
  botId: string;
  tempUserId: string;
}
/** 获取bot对话列表 */
export const getSessionList = (params: GetSessionListParams) => {
  return api.get<RequestResult<{ cons: Session[] }>>(
    '/api/v1/conversation/externalList',
    { params },
  );
};

export interface SaveBotParams {
  icon: string;
  title: string;
  alias: string;
  description: string;
  categoryId: any;
  status: number;
}
/** 创建Bot */
export const saveBot = (params: SaveBotParams) => {
  return api.post<RequestResult>('/api/v1/bot/save', { ...params });
};

export interface UpdateBotParams extends Partial<SaveBotParams> {
  id: string;
}
/** 修改Bot */
export const updateBotApi = (params: UpdateBotParams) => {
  return api.post<RequestResult>('/api/v1/bot/update', { ...params });
};

/** 发布Bot */
export const publishBotApi = (params: UpdateBotParams) => {
  return api.post<RequestResult>('/api/v1/bot/publish', { ...params });
};

/** 删除Bot */
export const removeBotFromId = (id: string) => {
  return api.post<RequestResult>('/api/v1/bot/remove', { id });
};

export interface GetMessageListParams {
  conversationId: string;
  botId: string;
  tempUserId: string;
}
/** 获取单个对话的信息列表 */
export const getMessageList = (params: GetMessageListParams) => {
  return api.get<RequestResult<ChatMessage[]>>(
    '/api/v1/botMessage/messageList',
    {
      params,
    },
  );
};

/** 更新Bot的LLM配置 */
export interface UpdateLlmOptionsParams {
  id: string;
  llmOptions: {
    [key: string]: any;
  };
}
export interface UpdateBotOptionsParams {
  id: string;
  options: {
    [key: string]: any;
  };
}

export const updateLlmOptions = (params: UpdateLlmOptionsParams) => {
  return api.post<RequestResult>('/api/v1/bot/updateLlmOptions', {
    ...params,
  });
};

export const updateBotOptions = (params: UpdateBotOptionsParams) => {
  return api.post<RequestResult>('/api/v1/bot/updateOptions', {
    ...params,
  });
};

/** 更新Bot的LLM配置 */
export interface GetAiLlmListParams {
  [key: string]: any;
}
export const getAiLlmList = (params: GetAiLlmListParams) => {
  return api.get<RequestResult<AiLlm[]>>('/api/v1/model/list', {
    params,
  });
};

/** 更新modelId */
export interface UpdateLlmIdParams {
  id: string;
  modelId: string;
}
export const updateLlmId = (params: UpdateLlmIdParams) => {
  return api.post<RequestResult>('/api/v1/bot/updateLlmId', {
    ...params,
  });
};

export const doPostBotPluginTools = (botId: string) => {
  return api.post<RequestResult<any[]>>('/api/v1/pluginItem/tool/list', {
    id: botId,
  });
};

export const getPerQuestions = (presetQuestions: any[]) => {
  if (!presetQuestions) {
    return [];
  }
  return presetQuestions
    .filter((item: any) => {
      return (
        typeof item.description === 'string' && item.description.trim() !== ''
      );
    })
    .map((item: any) => ({
      key: item.key,
      description: item.description,
    }));
};
