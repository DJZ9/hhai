/**
 * 文件类型枚举
 */
export enum FileType {
  EXCEL = 'excel',
  MARKDOWN = 'markdown',
  PDF = 'pdf',
  PPT = 'ppt',
  TXT = 'txt',
  UNKNOWN = 'unknown',
  WORD = 'word',
}

/**
 * 根据文件后缀名判断文件类型
 * @param fileName 文件名或文件路径
 * @returns 文件类型
 */
export function getFileType(fileName: string): FileType {
  if (!fileName) {
    return FileType.UNKNOWN;
  }

  const lastDotIndex = fileName.lastIndexOf('.');
  if (lastDotIndex === -1 || lastDotIndex === fileName.length - 1) {
    return FileType.UNKNOWN;
  }

  const extension = fileName.slice(lastDotIndex + 1).toLowerCase();

  if (['markdown', 'md', 'mdown', 'mdwn', 'mkd', 'mkdn'].includes(extension)) {
    return FileType.MARKDOWN;
  }

  if (['asc', 'log', 'text', 'txt'].includes(extension)) {
    return FileType.TXT;
  }

  if (['csv', 'ods', 'xls', 'xlsb', 'xlsm', 'xlsx'].includes(extension)) {
    return FileType.EXCEL;
  }

  if (
    ['doc', 'docm', 'docx', 'dot', 'dotx', 'odt', 'rtf'].includes(extension)
  ) {
    return FileType.WORD;
  }

  if (extension === 'pdf') {
    return FileType.PDF;
  }

  if (
    ['odp', 'pot', 'potx', 'pps', 'ppsx', 'ppt', 'pptm', 'pptx'].includes(
      extension,
    )
  ) {
    return FileType.PPT;
  }

  return FileType.UNKNOWN;
}

/**
 * 判断文件是否属于指定类型
 * @param fileName 文件名或文件路径
 * @param fileType 期望的文件类型
 * @returns 是否匹配
 */
export function isFileType(fileName: string, fileType: FileType): boolean {
  return getFileType(fileName) === fileType;
}

/**
 * 判断是否为图片文件
 * @param fileName 文件名或文件路径
 * @returns 是否图片
 */
export function isImageFile(fileName: string): boolean {
  const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
  const extension = fileName.split('.').pop()?.toLowerCase() || '';
  return imageExtensions.includes(extension);
}

/**
 * 从 URL 中提取文件名
 * @param url 文件 URL
 * @returns 文件名
 */
export function getFileName(url: string): string {
  const parts = url.split('/');
  return parts[parts.length - 1] || url;
}

/**
 * 从 URL 中提取文件后缀（大写）
 * @param url 文件 URL
 * @returns 大写后缀名
 */
export function getFileExt(url: string): string {
  const name = getFileName(url);
  const parts = name.split('.');
  return parts.length > 1 ? parts[parts.length - 1].toUpperCase() : '';
}

/**
 * 根据文件类型获取图标
 * @param fileName 文件名或文件路径
 * @returns Iconify 图标名
 */
export function getFileIcon(fileName: string): string {
  const iconMap: Record<FileType, string> = {
    [FileType.WORD]: 'mdi:file-word-box',
    [FileType.EXCEL]: 'mdi:file-excel-box',
    [FileType.PPT]: 'mdi:file-powerpoint-box',
    [FileType.PDF]: 'mdi:file-pdf-box',
    [FileType.TXT]: 'mdi:file-document-outline',
    [FileType.MARKDOWN]: 'mdi:file-document-outline',
    [FileType.UNKNOWN]: 'mdi:file-document-outline',
  };
  return iconMap[getFileType(fileName)] || iconMap[FileType.UNKNOWN];
}

/**
 * 根据文件类型获取图标颜色
 * @param fileName 文件名或文件路径
 * @returns 十六进制颜色值
 */
export function getFileIconColor(fileName: string): string {
  const colorMap: Record<FileType, string> = {
    [FileType.WORD]: '#2563eb',
    [FileType.EXCEL]: '#16a34a',
    [FileType.PPT]: '#f97316',
    [FileType.PDF]: '#ef4444',
    [FileType.TXT]: '#6b7280',
    [FileType.MARKDOWN]: '#6b7280',
    [FileType.UNKNOWN]: '#3b82f6',
  };
  return colorMap[getFileType(fileName)] || colorMap[FileType.UNKNOWN];
}

/**
 * 新窗口预览图片
 * @param url 图片 URL
 */
export function previewImage(url: string): void {
  window.open(url, '_blank');
}
