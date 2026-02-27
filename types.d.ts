import type { DuetLocalizedText } from "@duetds/date-picker/dist/types/components/duet-date-picker/date-localization";
import type { Locale } from "date-fns/locale/types";

declare global {
  var zeiterfassung: {
    datepicker: {
      localization: DuetLocalizedText;
    };
    i18n: Record<string, string>;
  };

  var __datefnsLocale__: Locale;
}

// eslint-disable-next-line unicorn/require-module-specifiers
export {};
