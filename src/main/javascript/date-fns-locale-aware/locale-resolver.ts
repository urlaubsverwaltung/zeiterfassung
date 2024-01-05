import { enUS as en } from "date-fns/locale/en-US";
import type { Locale } from "date-fns/locale/types";

globalThis.__datefnsLocale__ = globalThis.__datefnsLocale__ || en;

export function setLocale(locale: Locale): void {
  globalThis.__datefnsLocale__ = locale;
}

export default function resolveLocale(): Locale {
  return globalThis.__datefnsLocale__;
}
