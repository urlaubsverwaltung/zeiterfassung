import dateFnsFormat from "date-fns/format";
import resolveDateFnsLocale from "./locale-resolver";

type DateFnsOptions = {
  locale?: Locale;
  weekStartsOn?: 0 | 1 | 2 | 3 | 4 | 5 | 6;
  firstWeekContainsDate?: number;
  useAdditionalWeekYearTokens?: boolean;
  useAdditionalDayOfYearTokens?: boolean;
};

export default function format(
  date: Date,
  formatString: string,
  options: DateFnsOptions = {},
) {
  options.locale = resolveDateFnsLocale();
  return dateFnsFormat(date, formatString, options);
}
