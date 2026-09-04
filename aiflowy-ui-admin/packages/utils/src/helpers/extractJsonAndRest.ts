export function extractJsonAndRest(
  input: string,
): [any[], string] | [null, string] {
  // 查找第一个 '[' 的位置
  const startIndex = input.indexOf('[');
  if (startIndex === -1) {
    return [null, input];
  }

  // 使用深度计数器处理嵌套结构
  let depth = 0;
  let inString = false;
  let escape = false;

  for (let i = startIndex; i < input.length; i++) {
    const char = input[i];

    // 处理转义字符
    if (escape) {
      escape = false;
      continue;
    }

    // 处理字符串边界
    if (char === '"' && !inString) {
      inString = true;
      continue;
    }
    if (char === '"' && inString) {
      inString = false;
      continue;
    }

    // 在字符串外处理方括号
    if (!inString) {
      if (char === '[') {
        depth++;
      } else if (char === ']') {
        depth--;
        if (depth === 0) {
          // 找到匹配的闭合括号
          const jsonStr = input.slice(startIndex, i + 1);
          try {
            const parsed = JSON.parse(jsonStr);
            // 校验数据结构...
            if (
              Array.isArray(parsed) &&
              parsed.every(
                (item) =>
                  typeof item === 'object' &&
                  item !== null &&
                  typeof item.attrName === 'string' &&
                  typeof item.type === 'string' &&
                  typeof item.label === 'string' &&
                  (item.options === undefined || Array.isArray(item.options)) &&
                  (item.required === undefined ||
                    typeof item.required === 'boolean'),
              )
            ) {
              const rest = input.slice(0, startIndex) + input.slice(i + 1);
              return [parsed, rest];
            }
          } catch {
            // 解析失败，继续寻找下一个可能的数组
          }
        }
      }
    }

    // 处理转义（仅在字符串内有效）
    if (char === '\\' && inString) {
      escape = true;
    }
  }

  return [null, input];
}
