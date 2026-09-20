import { getOptions } from '@aiflowy/utils';

import { api } from '#/api/request';
import { $t } from '#/locales';

export const ImageGenerationNode = async () => {
  const res = await api.get('/api/v1/model/list?modelType=imageModel');

  return {
    title: $t('aiWorkflow.imageGeneration'),
    group: 'base',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M3 5C3 3.89543 3.89543 3 5 3H19C20.1046 3 21 3.89543 21 5V19C21 20.1046 20.1046 21 19 21H5C3.89543 21 3 20.1046 3 19V5ZM15.5 8.5C15.5 9.88071 14.3807 11 13 11C11.6193 11 10.5 9.88071 10.5 8.5C10.5 7.11929 11.6193 6 13 6C14.3807 6 15.5 7.11929 15.5 8.5ZM5 17L9 13L13 17L16 14L19 17V5H5V17Z"></path></svg>',
    sortNo: 813,
    parametersAddEnable: true,
    outputDefsAddEnable: false,
    parameters: [
      {
        name: 'prompt',
        title: $t('aiWorkflow.imagePrompt'),
        dataType: 'String',
        dataTypeDisabled: true,
        required: true,
        description: $t('aiWorkflow.descriptions.imagePrompt'),
        deleteDisabled: true,
        nameDisabled: true,
      },
      {
        name: 'negativePrompt',
        title: $t('aiWorkflow.imageNegativePrompt'),
        dataType: 'String',
        dataTypeDisabled: true,
        required: false,
        description: $t('aiWorkflow.descriptions.imageNegativePrompt'),
        deleteDisabled: false,
        nameDisabled: true,
      },
      {
        name: 'size',
        title: $t('aiWorkflow.imageSize'),
        dataType: 'String',
        dataTypeDisabled: true,
        required: false,
        description: $t('aiWorkflow.descriptions.imageSize'),
        deleteDisabled: false,
        nameDisabled: true,
      },
    ],
    outputDefs: [
      {
        name: 'url',
        title: $t('aiWorkflow.imageUrl'),
        dataType: 'String',
        dataTypeDisabled: true,
        required: true,
        parametersAddEnable: false,
        description: $t('aiWorkflow.descriptions.imageUrl'),
        deleteDisabled: true,
        nameDisabled: true,
      },
    ],
    forms: [
      {
        type: 'heading',
        label: $t('aiWorkflow.model'),
      },
      {
        type: 'select',
        label: '',
        description: $t('aiWorkflow.descriptions.model'),
        name: 'modelId',
        defaultValue: '',
        options: getOptions('title', 'id', res.data),
      },
    ],
  };
};
