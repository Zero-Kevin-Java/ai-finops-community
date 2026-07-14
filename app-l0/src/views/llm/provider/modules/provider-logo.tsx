import type { SelectOption } from 'naive-ui';
import type { VNodeChild } from 'vue';

export type ProviderLogoLike = Pick<Api.Llm.Provider, 'providerName' | 'logoSlug'>;

type LobeIconConfig = {
  componentName: string;
  variant: string;
};

const lobeStaticBases = [
  'https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/light',
  'https://unpkg.com/@lobehub/icons-static-png@latest/light',
  'https://raw.githubusercontent.com/lobehub/lobe-icons/refs/heads/master/packages/static-png/light'
];

const lobeIconNameAliases: Record<string, string> = {
  alibabacloud: 'alibaba-cloud',
  anthropic: 'anthropic',
  azureopenai: 'azure-openai',
  deepseek: 'deepseek',
  huggingface: 'huggingface',
  minimax: 'minimax',
  openai: 'openai',
  siliconflow: 'siliconflow',
  tencentcloud: 'tencent-cloud',
  zhipuai: 'zhipu'
};

export function getProviderLogoText(provider?: Partial<ProviderLogoLike> | null) {
  const text = provider?.providerName || provider?.logoSlug || '?';
  return text.slice(0, 1).toUpperCase();
}

function toKebabCase(value: string) {
  return value
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/[\s_.]+/g, '-')
    .toLowerCase();
}

function normalizeLobeIconName(value: string) {
  const text = value.trim();
  const compactKey = text.replace(/[\s_.-]+/g, '').toLowerCase();

  return lobeIconNameAliases[compactKey] || toKebabCase(text);
}

function parseLobeIconConfig(logoSlug?: string | null): LobeIconConfig | null {
  const value = logoSlug?.trim();
  if (!value) {
    return null;
  }

  const jsxMatch = value.match(/^<\s*([A-Za-z][\w]*)\.([A-Za-z][\w]*)\b[^>]*\/\s*>$/);
  if (jsxMatch) {
    return {
      componentName: jsxMatch[1],
      variant: jsxMatch[2]
    };
  }

  const keyMatch = value.match(/^([A-Za-z][\w]*)\.([A-Za-z][\w]*)$/);
  if (keyMatch) {
    return {
      componentName: keyMatch[1],
      variant: keyMatch[2]
    };
  }

  return {
    componentName: value,
    variant: 'Color'
  };
}

function getLobeStaticPngUrls(logoSlug?: string | null) {
  const value = logoSlug?.trim();
  if (!value) {
    return [];
  }

  if (/^https?:\/\//i.test(value)) {
    return [value];
  }

  if (/\.(png|jpe?g|webp|svg)$/i.test(value)) {
    const fileName = value.replace(/^\/+/, '');
    return lobeStaticBases.map(base => `${base}/${fileName}`);
  }

  const config = parseLobeIconConfig(logoSlug);
  if (!config) {
    return [];
  }

  const icon = normalizeLobeIconName(config.componentName);
  const variant = config.variant.toLowerCase();
  const suffix = variant === 'color' ? '-color' : '';
  const fileNames = [`${icon}${suffix}.png`, `${icon}.png`].filter(
    (fileName, index, list) => list.indexOf(fileName) === index
  );

  return fileNames.flatMap(fileName => lobeStaticBases.map(base => `${base}/${fileName}`));
}

function getProviderLogoSource(provider?: Partial<ProviderLogoLike> | null) {
  return provider?.logoSlug?.trim() || provider?.providerName?.trim() || '';
}

export function renderProviderLogo(provider?: Partial<ProviderLogoLike> | null, size = 28) {
  const srcList = getLobeStaticPngUrls(getProviderLogoSource(provider));
  const src = srcList[0];
  const label = getProviderLogoText(provider).slice(0, 2).toUpperCase();

  if (src) {
    return (
      <span
        class="inline-flex items-center justify-center overflow-hidden rounded-full bg-transparent"
        style={{ height: `${size}px`, width: `${size}px` }}
      >
        <img
          src={src}
          alt={provider?.providerName || ''}
          class="block h-full w-full object-contain"
          onError={event => {
            const image = event.target as HTMLImageElement;
            const currentIndex = Number(image.dataset.srcIndex || '0');
            const nextSrc = srcList[currentIndex + 1];

            if (nextSrc) {
              image.dataset.srcIndex = String(currentIndex + 1);
              image.src = nextSrc;
              return;
            }

            image.style.display = 'none';
            const fallback = image.nextElementSibling as HTMLElement | null;
            if (fallback) {
              fallback.style.display = 'flex';
            }
          }}
        />
        <span
          class="hidden h-full w-full items-center justify-center rounded-full bg-primary text-white font-medium"
          style={{ fontSize: `${Math.max(10, Math.round(size * 0.38))}px` }}
        >
          {label}
        </span>
      </span>
    );
  }

  return (
    <span
      class="inline-flex items-center justify-center rounded-full bg-primary text-white font-medium"
      style={{ fontSize: `${Math.max(10, Math.round(size * 0.38))}px`, height: `${size}px`, width: `${size}px` }}
    >
      {label}
    </span>
  );
}

function getProviderFromOption(option: SelectOption) {
  return (option.raw ?? option) as Partial<ProviderLogoLike>;
}

export function renderProviderSelectLabel(option: SelectOption): VNodeChild {
  const provider = getProviderFromOption(option);

  return (
    <div class="flex-y-center gap-8px">
      {renderProviderLogo(provider, 22)}
      <span>{String(option.label ?? provider.providerName ?? '')}</span>
    </div>
  );
}

export function renderProviderSelectTag({ option }: { option: SelectOption; handleClose?: () => void }): VNodeChild {
  return renderProviderSelectLabel(option);
}
