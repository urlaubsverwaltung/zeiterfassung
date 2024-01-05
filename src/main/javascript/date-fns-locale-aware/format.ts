import { format as dateFnsFormat, type FormatOptions } from "date-fns/format";
import resolveDateFnsLocale from "./locale-resolver";

export default function format(
  date: Date,
  formatString: string,
  options: FormatOptions = {},
) {
  options.locale = resolveDateFnsLocale();
  return dateFnsFormat(date, formatString, options);
}
