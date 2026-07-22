import type { DuetLocalizedText } from "@duetds/date-picker/dist/types/components/duet-date-picker/date-localization";

declare global {
  var zeiterfassung: {
    datepicker: {
      localization: DuetLocalizedText;
    };
    feedback: {
      showFeedbackKudo: boolean;
    };
    i18n: Record<string, string>;
  };

  var injectStyle: (style: string) => HTMLElement;
}

export {};
