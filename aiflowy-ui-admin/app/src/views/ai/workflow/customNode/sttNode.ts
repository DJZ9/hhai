import { $t } from '#/locales';

export const SpeechToTextNode = () => {
  return {
    title: $t('aiWorkflow.speechToText'),
    group: 'base',
    description: $t('aiWorkflow.descriptions.speechToText'),
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3Zm-7 9a1 1 0 1 1 2 0 5 5 0 0 0 10 0 1 1 0 1 1 2 0 7 7 0 0 1-6 6.93V22a1 1 0 1 1-2 0v-3.07A7 7 0 0 1 5 12Z"></path></svg>',
    sortNo: 803,
    parametersAddEnable: false,
    outputDefsAddEnable: false,
    parameters: [
      {
        name: 'audioUrl',
        title: $t('aiWorkflow.sttAudioUrl'),
        dataType: 'String',
        required: true,
        parametersAddEnable: false,
        description: $t('aiWorkflow.descriptions.sttAudioUrl'),
        deleteDisabled: true,
        nameDisabled: true,
      },
    ],
    outputDefs: [
      {
        name: 'text',
        title: $t('aiWorkflow.sttText'),
        dataType: 'String',
        dataTypeDisabled: true,
        required: true,
        parametersAddEnable: false,
        description: $t('aiWorkflow.descriptions.sttText'),
        deleteDisabled: true,
        nameDisabled: true,
      },
    ],
    forms: [
      {
        type: 'heading',
        label: $t('aiWorkflow.sttSettings'),
      },
      {
        type: 'select',
        label: $t('aiWorkflow.sttFormat'),
        description: $t('aiWorkflow.descriptions.sttFormat'),
        name: 'format',
        defaultValue: 'mp3',
        options: [
          { label: 'mp3', value: 'mp3' },
          { label: 'wav', value: 'wav' },
          { label: 'pcm', value: 'pcm' },
        ],
      },
    ],
  };
};
