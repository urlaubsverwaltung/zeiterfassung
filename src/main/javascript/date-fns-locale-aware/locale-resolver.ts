import { type Locale } from "date-fns";
import { enUS as en } from "date-fns/locale/en-US";

const state: { locale: Locale } = { locale: en };

export function setLocale(locale: Locale): void {
  state.locale = locale;
}

export default function resolveLocale(): Locale {
  return state.locale;
}
