type DeepRequired<T, K extends keyof T = never> = T extends object
  ? T extends Function // 先排除函数
    ? T
    : T extends Array<infer U> // 处理数组
      ? Array<DeepRequired<U, Extract<keyof U, keyof any>>>
      : {
          [P in keyof T]-?: P extends K
            ? T[P]
            : DeepRequired<T[P], Extract<keyof T[P], keyof any>>;
        }
  : T;

export interface AiflowySDKOptions {
  /**
   * 智能体 ID
   */
  botId: string;
  /**
   * AIFlowy 服务地址
   */
  endpoint: string;
  /**
   * 登录令牌（可选：第三方系统传入的现成 token，拼到 iframe URL 供 usercenter 免登录；不传则不注入登录态）
   */
  token?: string;
  /**
   * 浮动入口配置
   */
  launcher?: {
    placement?: "bottom-right" | "bottom-left" | "top-right" | "top-left";
    label?: string;
    icon?: string;
    size?: number;
    backgroundColor?: string;
    textColor?: string;
  };
  /**
   * 聊天窗口配置
   */
  panel?: {
    title?: string;
    width?: number;
    height?: number;
    zIndex?: number;
  };
  /**
   * 主题配置
   */
  theme?: {
    borderRadius?: number;
    dark?: boolean;
    primaryColor?: string;
  };
  /**
   * 生命周期事件
   */
  callbacks?: {
    onOpen?: () => void;
    onClose?: () => void;
    onReady?: () => void;
  };
}

type NormalizedOptions = Omit<
  DeepRequired<AiflowySDKOptions, "callbacks">,
  "botId" | "endpoint" | "token"
>;

const DEFAULTS: NormalizedOptions = {
  launcher: {
    placement: "bottom-right",
    label: "智能客服",
    icon: "",
    size: 60,
    backgroundColor: "#0066ff",
    textColor: "#FFFFFF",
  },
  panel: {
    title: "智能体对话",
    width: 1200,
    height: 800,
    zIndex: 999999,
  },
  theme: {
    borderRadius: 12,
    dark: false,
    primaryColor: "#0066ff",
  },
  callbacks: {},
};

function deepMerge(target: NormalizedOptions, source: AiflowySDKOptions) {
  const result: Record<string, any> = { ...target };
  const src: Record<string, any> = source;
  for (const key of Object.keys(source)) {
    if (src[key] && typeof src[key] === "object" && !Array.isArray(src[key])) {
      result[key] = deepMerge(result[key] || {}, src[key]);
    } else if (src[key] !== undefined) {
      result[key] = src[key];
    }
  }
  return result as DeepRequired<AiflowySDKOptions, "callbacks">;
}

class AiflowySDK {
  private options: DeepRequired<AiflowySDKOptions, "callbacks">;
  private triggerButton: HTMLButtonElement | null = null;
  private popupContainer: HTMLDivElement | null = null;
  private iframe: HTMLIFrameElement | null = null;
  private iframeUrl: string;
  private isVisible: boolean = false;
  private isInitialized: boolean = false;

  constructor(options: AiflowySDKOptions) {
    if (!options.botId) {
      throw new Error("botId is required");
    }
    if (!options.endpoint) {
      throw new Error("endpoint is required");
    }
    this.options = deepMerge(DEFAULTS, options);

    const endpoint = options.endpoint.replace(/\/+$/, "");
    const params = new URLSearchParams({
      botId: options.botId,
      primaryColor: this.options.theme.primaryColor,
      dark: `${this.options.theme.dark}`,
    });
    if (options.token) {
      params.set("token", options.token);
    }
    this.iframeUrl = `${endpoint}/#/iframe/chat?${params.toString()}`;
  }

  init(): void {
    if (typeof document === "undefined") return;
    if (this.isInitialized) return;
    this.injectStyles();
    this.createTriggerButton();
    this.createPopup();
    this.bindEvents();
    this.isInitialized = true;
    this.options.callbacks.onReady?.();
  }

  private injectStyles(): void {
    if (document.getElementById("aiflowy-sdk-styles")) return;

    const style = document.createElement("style");
    style.id = "aiflowy-sdk-styles";
    style.textContent = `
      @keyframes aiflowy-slide-in {
        from {
          opacity: 0;
          transform: translateY(10px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
      @media (max-width: 480px) {
        #aiflowy-popup {
          width: 100vw !important;
          height: 100vh !important;
          bottom: 0 !important;
          right: 0 !important;
          left: 0 !important;
          top: 0 !important;
          border-radius: 0 !important;
        }
      }
    `;
    document.head.appendChild(style);
  }

  private createTriggerButton(): void {
    const button = document.createElement("button");
    button.id = "aiflowy-trigger-btn";
    button.setAttribute("aria-label", this.options.launcher.label);

    const positionStyles = this.getPositionStyles();

    button.style.cssText = `
      position: fixed;
      ${positionStyles.vertical};
      ${positionStyles.horizontal};
      width: ${this.options.launcher.size}px;
      height: ${this.options.launcher.size}px;
      border-radius: 50%;
      background-color: ${this.options.launcher.backgroundColor};
      color: ${this.options.launcher.textColor};
      border: none;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      z-index: ${this.options.panel.zIndex};
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      transition: transform 0.2s, box-shadow 0.2s;
      outline: none;
    `;

    if (this.options.launcher.icon) {
      button.innerHTML = this.options.launcher.icon;
    } else {
      button.innerHTML = `
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
      `;
    }

    button.onmouseover = () => {
      button.style.transform = "scale(1.1)";
      button.style.boxShadow = "0 6px 16px rgba(0, 0, 0, 0.2)";
    };
    button.onmouseout = () => {
      button.style.transform = "scale(1)";
      button.style.boxShadow = "0 4px 12px rgba(0, 0, 0, 0.15)";
    };

    this.triggerButton = button;
    document.body.appendChild(button);
  }

  private getPositionStyles(): { vertical: string; horizontal: string } {
    const { placement } = this.options.launcher;
    const spacing = "24px";

    switch (placement) {
      case "bottom-left":
        return { vertical: `bottom: ${spacing}`, horizontal: "left: 24px" };
      case "top-right":
        return { vertical: `top: ${spacing}`, horizontal: "right: 24px" };
      case "top-left":
        return { vertical: `top: ${spacing}`, horizontal: "left: 24px" };
      default:
        return { vertical: `bottom: ${spacing}`, horizontal: "right: 24px" };
    }
  }

  private createPopup(): void {
    const container = document.createElement("div");
    container.id = "aiflowy-popup";

    const { width, height, zIndex, title } = this.options.panel;
    const { borderRadius } = this.options.theme;

    const positionStyles = this.getPopupPositionStyles();

    container.style.cssText = `
      position: fixed;
      ${positionStyles.vertical};
      ${positionStyles.horizontal};
      width: ${width}px;
      height: ${height}px;
      background: #FFFFFF;
      border-radius: ${borderRadius}px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
      z-index: ${zIndex};
      display: none;
      flex-direction: column;
      overflow: hidden;
      animation: aiflowy-slide-in 0.3s ease-out;
    `;

    const header = document.createElement("div");
    header.style.cssText = `
      padding: 16px 20px;
      background: ${this.options.theme.primaryColor};
      color: ${this.options.launcher.textColor};
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 16px;
      font-weight: 600;
    `;
    header.innerHTML = `<span>${title}</span>`;

    const closeBtn = document.createElement("button");
    closeBtn.innerHTML = `
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="18" y1="6" x2="6" y2="18"></line>
        <line x1="6" y1="6" x2="18" y2="18"></line>
      </svg>
    `;
    closeBtn.style.cssText = `
      background: transparent;
      border: none;
      color: ${this.options.launcher.textColor};
      cursor: pointer;
      padding: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      transition: background 0.2s;
    `;
    closeBtn.onmouseover = () => {
      closeBtn.style.background = "rgba(255, 255, 255, 0.2)";
    };
    closeBtn.onmouseout = () => {
      closeBtn.style.background = "transparent";
    };
    closeBtn.onclick = (e) => {
      e.stopPropagation();
      this.hide();
    };

    header.appendChild(closeBtn);

    const iframe = document.createElement("iframe");
    iframe.src = this.iframeUrl;
    iframe.style.cssText = `
      flex: 1;
      border: none;
      width: 100%;
      height: 100%;
    `;

    container.appendChild(header);
    container.appendChild(iframe);

    this.popupContainer = container;
    this.iframe = iframe;
    document.body.appendChild(container);
  }

  private getPopupPositionStyles(): { vertical: string; horizontal: string } {
    const { placement } = this.options.launcher;
    const spacing = "96px";

    switch (placement) {
      case "bottom-left":
        return {
          vertical: `bottom: ${spacing}`,
          horizontal: `left: 24px`,
        };
      case "top-right":
        return {
          vertical: `top: ${spacing}`,
          horizontal: `right: 24px`,
        };
      case "top-left":
        return {
          vertical: `top: ${spacing}`,
          horizontal: `left: 24px`,
        };
      default:
        return {
          vertical: `bottom: ${spacing}`,
          horizontal: `right: 24px`,
        };
    }
  }

  private bindEvents(): void {
    if (this.triggerButton) {
      this.triggerButton.addEventListener("click", () => {
        this.toggle();
      });
    }

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && this.isVisible) {
        this.hide();
      }
    });

    document.addEventListener("click", (e) => {
      if (
        this.isVisible &&
        this.popupContainer &&
        !this.popupContainer.contains(e.target as Node) &&
        this.triggerButton &&
        !this.triggerButton.contains(e.target as Node)
      ) {
        this.hide();
      }
    });
  }

  toggle(): void {
    if (this.isVisible) {
      this.hide();
    } else {
      this.show();
    }
  }

  show(): void {
    if (this.popupContainer) {
      this.popupContainer.style.display = "flex";
      this.isVisible = true;
      this.options.callbacks.onOpen?.();
    }
  }

  hide(): void {
    if (this.popupContainer) {
      this.popupContainer.style.display = "none";
      this.isVisible = false;
      this.options.callbacks.onClose?.();
    }
  }

  destroy(): void {
    if (this.triggerButton) {
      this.triggerButton.remove();
      this.triggerButton = null;
    }
    if (this.popupContainer) {
      this.popupContainer.remove();
      this.popupContainer = null;
    }
    const styleElement = document.getElementById("aiflowy-sdk-styles");
    if (styleElement) {
      styleElement.remove();
    }
    this.iframe = null;
    this.isVisible = false;
    this.isInitialized = false;
  }
}

function initAiflowy(options: AiflowySDKOptions): AiflowySDK {
  const sdk = new AiflowySDK(options);
  if (typeof document !== "undefined") {
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", () => sdk.init());
    } else {
      sdk.init();
    }
  }
  return sdk;
}

export { AiflowySDK, initAiflowy };
export default AiflowySDK;
