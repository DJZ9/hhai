import { onUnmounted, ref } from 'vue';

import { ElMessage } from 'element-plus';
import Recorder from 'js-audio-recorder';

import { $t } from '@aiflowy/locales';

import { api } from '#/api/request';

export function useVoiceRecorder() {
  const isRecording = ref(false);
  const isProcessing = ref(false);
  let recorder: any = null;

  // 开始录音
  async function startRecording(): Promise<void> {
    // 检查浏览器是否支持 getUserMedia（需要 HTTPS 或 localhost）
    if (!navigator.mediaDevices?.getUserMedia) {
      ElMessage.error($t('common.voice.httpsRequired'));
      return;
    }
    try {
      recorder = new Recorder({
        sampleBits: 16,
        sampleRate: 16_000,
        numChannels: 1,
      });
      await recorder.start();
      isRecording.value = true;
    } catch (error: any) {
      const msg = error?.message || '';
      if (msg.includes('getUserMedia') || msg.includes('NotAllowedError')) {
        ElMessage.error($t('common.voice.micPermissionDenied'));
      } else {
        ElMessage.error(msg || $t('common.voice.micPermissionFailed'));
      }
      throw error;
    }
  }

  // 停止录音并发送到后端识别
  async function stopRecording(): Promise<string> {
    if (!recorder) return '';
    try {
      recorder.stop();
      isRecording.value = false;
      isProcessing.value = true;

      const blob: Blob = recorder.getWAVBlob();
      const text = await sendToStt(blob);
      return text;
    } catch (error: any) {
      ElMessage.error(error?.message || $t('common.voice.recognitionFailed'));
      return '';
    } finally {
      isProcessing.value = false;
      recorder = null;
    }
  }

  // 发送音频到后端 STT 接口
  async function sendToStt(audioBlob: Blob): Promise<string> {
    const formData = new FormData();
    formData.append('audio', audioBlob, 'recording.wav');

    const response = await api.post('/api/v1/bot/voiceInput', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data ?? '';
  }

  // 清理资源
  onUnmounted(() => {
    if (isRecording.value && recorder) {
      recorder.stop();
    }
    recorder = null;
    isRecording.value = false;
    isProcessing.value = false;
  });

  return {
    isRecording,
    isProcessing,
    startRecording,
    stopRecording,
  };
}
