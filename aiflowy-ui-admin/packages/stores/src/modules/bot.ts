import { acceptHMRUpdate, defineStore } from 'pinia';

// 定义预设问题类型
export type PresetQuestionsType = {
  /** 部门ID列表 */
  deptIds?: string[];
  description: string;
  key: string;
  /** 范围：all=所有人，dept=指定部门 */
  scope?: 'all' | 'dept';
};

// Bot options配置类型
export type BotOptionsType = {
  /** 预设问题列表 */
  presetQuestions?: PresetQuestionsType[];
  /** 附件功能是否启用 */
  attachmentEnabled?: boolean;
  /** VL模型ID */
  vlModelId?: string;
  /** 其他配置 */
  [key: string]: any;
};

// Bot状态接口
interface BotStoreState {
  /** 预设问题列表 */
  presetQuestions: Map<string, PresetQuestionsType[]>;
  /** Bot options配置 */
  botOptions: Map<string, BotOptionsType>;
}

/**
 * @zh_CN Bot相关 store
 */
export const useBotStore = defineStore('bot-preset', {
  state: (): BotStoreState => ({
    presetQuestions: new Map(),
    botOptions: new Map(),
  }),
  actions: {
    getPresetQuestions(botId?: string): PresetQuestionsType[] {
      if (botId) {
        return this.presetQuestions.get(botId) || [];
      }
      return [];
    },
    setPresetQuestions(botId: string, questions: PresetQuestionsType[]) {
      this.presetQuestions.set(botId, questions);
    },
    getBotOptions(botId?: string): BotOptionsType | undefined {
      if (botId) {
        return this.botOptions.get(botId);
      }
      return undefined;
    },
    setBotOptions(botId: string, options: BotOptionsType) {
      this.botOptions.set(botId, options);
    },
    updateBotOption(botId: string, key: string, value: any) {
      const currentOptions = this.botOptions.get(botId) || {};
      this.botOptions.set(botId, { ...currentOptions, [key]: value });
    },
  },
});

// 解决热更新问题
if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useBotStore, import.meta.hot));
}
