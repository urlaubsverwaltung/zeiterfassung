import en from "date-fns/locale/en-US";

globalThis.__datefnsLocale__ = globalThis.__datefnsLocale__ || en;

export function setLocale(locale: Locale): void {
  globalThis.__datefnsLocale__ = locale;
}

export default function resolveLocale(): Locale {
  return globalThis.__datefnsLocale__;
}
