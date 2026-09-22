import { $t } from '#/locales';

export const TextToSpeechNode = () => {
  return {
    title: $t('aiWorkflow.textToSpeech'),
    group: 'base',
    description: $t('aiWorkflow.descriptions.textToSpeech'),
    icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M3 10v4a1 1 0 0 0 1 1h3l3.293 3.293a1 1 0 0 0 1.707-.707V6.414a1 1 0 0 0-1.707-.707L7 9H4a1 1 0 0 0-1 1Zm13.5 2a4.5 4.5 0 0 0-2.5-4.03v8.06A4.5 4.5 0 0 0 16.5 12Zm-2.5-7v1.5a6.5 6.5 0 0 1 0 12.5V20a8 8 0 0 0 0-15Z"></path></svg>',
    sortNo: 802,
    parametersAddEnable: false,
    outputDefsAddEnable: false,
    parameters: [
      {
        name: 'text',
        title: $t('aiWorkflow.ttsText'),
        dataType: 'String',
        required: true,
        parametersAddEnable: false,
        description: $t('aiWorkflow.descriptions.ttsText'),
        deleteDisabled: true,
        nameDisabled: true,
      },
    ],
    outputDefs: [
      {
        name: 'url',
        title: $t('aiWorkflow.ttsAudioUrl'),
        dataType: 'String',
        dataTypeDisabled: true,
        required: true,
        parametersAddEnable: false,
        description: $t('aiWorkflow.descriptions.ttsAudioUrl'),
        deleteDisabled: true,
        nameDisabled: true,
      },
    ],
    forms: [
      {
        type: 'heading',
        label: $t('aiWorkflow.ttsSettings'),
      },
      {
        type: 'input',
        label: $t('aiWorkflow.ttsVoice'),
        description: $t('aiWorkflow.descriptions.ttsVoice'),
        name: 'voice',
        defaultValue: '',
        placeholder: 'xiaoyun / zh_female_vv_uranus_bigtts',
      },
      {
        type: 'select',
        label: $t('aiWorkflow.ttsFormat'),
        description: $t('aiWorkflow.descriptions.ttsFormat'),
        name: 'format',
        defaultValue: 'mp3',
        options: [
          { label: 'mp3', value: 'mp3' },
          { label: 'wav', value: 'wav' },
          { label: 'pcm', value: 'pcm' },
        ],
      },
      {
        type: 'input',
        label: $t('aiWorkflow.ttsSpeed'),
        description: $t('aiWorkflow.descriptions.ttsSpeed'),
        name: 'speed',
        defaultValue: '',
        placeholder: '语速，如 1.0',
      },
      {
        type: 'input',
        label: $t('aiWorkflow.ttsVolume'),
        description: $t('aiWorkflow.descriptions.ttsVolume'),
        name: 'volume',
        defaultValue: '',
        placeholder: '音量，如 50',
      },
      {
        type: 'input',
        label: $t('aiWorkflow.ttsSampleRate'),
        description: $t('aiWorkflow.descriptions.ttsSampleRate'),
        name: 'sampleRate',
        defaultValue: '',
        placeholder: '采样率，如 16000',
      },
    ],
  };
};
