import { $t } from '@aiflowy/locales';

export const modelTypes = [
  {
    label: $t('llmProvider.chatModel'),
    value: 'chatModel',
  },
  {
    label: $t('llmProvider.embeddingModel'),
    value: 'embeddingModel',
  },
  {
    label: $t('llmProvider.rerankModel'),
    value: 'rerankModel',
  },
  {
    label: $t('llmProvider.ocrModel'),
    value: 'ocrModel',
  },
  {
    label: $t('llmProvider.imageModel'),
    value: 'imageModel',
  },
  {
    label: $t('llmProvider.videoModel'),
    value: 'videoModel',
  },
];
