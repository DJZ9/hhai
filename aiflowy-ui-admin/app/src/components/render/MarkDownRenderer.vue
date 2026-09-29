<script setup lang="ts">
import { computed, defineProps } from 'vue';

import hljs from 'highlight.js';
import markdownit from 'markdown-it';

import 'highlight.js/styles/github.css';

const props = defineProps({
  // 必传：Markdown格式字符串（支持空值，无内容时渲染空）
  content: {
    type: String,
    default: '',
    required: false,
  },
  // 可选：是否允许解析Markdown中的HTML标签（默认关闭，防止XSS攻击）
  allowHtml: {
    type: Boolean,
    default: false,
  },
  // 可选：是否自动识别纯文本链接（如https://xxx转<a>标签，默认开启）
  linkify: {
    type: Boolean,
    default: true,
  },
  // 可选：是否将换行符转为<br>标签（默认开启，适配纯文本换行）
  breaks: {
    type: Boolean,
    default: true,
  },
});

// 初始化markdown-it实例，配置核心解析规则
const md = markdownit({
  html: props.allowHtml, // 由入参控制是否解析HTML
  linkify: props.linkify, // 自动识别链接
  typographer: true, // 优化排版（英文引号转中文、破折号优化）
  breaks: props.breaks, // 换行符转<br>
  // 代码高亮核心配置
  highlight: (str: string, lang: string) => {
    // 若指定语言且hljs支持，进行代码高亮
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${
          hljs.highlight(str, {
            language: lang,
            ignoreIllegals: true,
          }).value
        }</code></pre>`;
      } catch (error) {
        console.warn('【Markdown渲染】代码高亮失败：', error);
      }
    }
    // 无指定语言/高亮失败，直接转义文本展示
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`;
  },
});

// 计算属性：实时渲染Markdown为HTML，content变化时自动重新渲染
const renderedHtml = computed(() => {
  if (!props.content) return ''; // 空值处理，避免渲染报错
  return md.render(props.content);
});
</script>

<template>
  <!-- Markdown渲染容器，统一做样式隔离 -->
  <div class="markdown-renderer-container" v-html="renderedHtml"></div>
</template>

<style scoped>
/* 基础容器样式：适配大多数页面，统一行高、字体 */
.markdown-renderer-container {
  line-height: 1.8;
  font-size: 14px;
  color: #333;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue',
    Arial, sans-serif;
  width: 100%;
  box-sizing: border-box;
}

/* 标题样式：逐级缩小，控制间距，避免突兀 */
:deep(h1) {
  font-size: 20px;
  font-weight: 600;
  margin: 16px 0 8px;
  color: #1a1a1a;
  line-height: 1.5;
}
:deep(h2) {
  font-size: 18px;
  font-weight: 600;
  margin: 14px 0 6px;
  color: #1a1a1a;
  line-height: 1.5;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}
:deep(h3),
:deep(h4),
:deep(h5),
:deep(h6) {
  font-size: 16px;
  font-weight: 600;
  margin: 12px 0 4px;
  color: #1a1a1a;
  line-height: 1.5;
}

/* 列表样式：控制缩进和间距 */
:deep(ul),
:deep(ol) {
  padding-left: 24px;
  margin: 6px 0;
}
:deep(li) {
  margin: 4px 0;
}

:deep(a) {
  color: #409eff;
  text-decoration: none;
}
:deep(a:hover) {
  text-decoration: underline;
}

:deep(strong) {
  font-weight: 600;
  color: #1a1a1a;
}
:deep(em) {
  color: #666;
  font-style: italic;
}

:deep(code) {
  padding: 2px 4px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #e64343;
  font-family: Consolas, Monaco, 'Courier New', monospace;
}
:deep(pre) {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin: 8px 0;
  overflow-x: auto; /* 代码块横向滚动 */
}
:deep(pre code) {
  padding: 0;
  background: transparent;
  color: #333;
}

:deep(blockquote) {
  border-left: 3px solid #409eff;
  padding: 0 12px;
  margin: 10px 0;
  color: #666;
  background: #f8f9fa;
}

:deep(hr) {
  border: 0;
  height: 1px;
  background: #e5e7eb;
  margin: 16px 0;
}

:deep(img) {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 10px auto;
  border-radius: 4px;
}

:deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
}
:deep(th),
:deep(td) {
  border: 1px solid #e5e7eb;
  padding: 8px 12px;
  text-align: left;
}
:deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}
</style>
