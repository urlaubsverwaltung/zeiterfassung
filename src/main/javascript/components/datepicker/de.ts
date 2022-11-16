import format from "../../date-fns-locale-aware/format";
import type { DuetDateAdapter } from "@duetds/date-picker/dist/types/components/duet-date-picker/date-adapter";

const DATE_FORMAT = /^(\d{1,2})\.(\d{1,2})\.(\d{4})$/;

/**
 * format used for the visually visible date string of the input element.
 * @type {string}
 */
export const dateFormat = "dd.MM.yyyy";

/**
 * short date format used by duet-date-picker for screen reader description only.
 *
 * note that duet does not use this pattern to format a date! duet has a hard coded one!
 * we're (mis)using this to enhance the date-picker visuals :x
 * therefore this value has to match the <a href="https://github.com/duetds/date-picker/blob/v1.2.0/src/components/duet-date-picker/duet-date-picker.tsx#L219">duet-date-picker implementation</a>!
 *
 *  @type {string}
 */
export const dateFormatShort = "dd. MMMM";

/**
 * duet-date-picker adapter to format and parse dates
 */
export const dateAdapter: DuetDateAdapter = {
  parse(value, createDate) {
    const matches = (value || "").match(DATE_FORMAT);

    if (matches) {
      return createDate(matches[3], matches[2], matches[1]);
    }
  },
  format(date) {
    return date ? format(date, dateFormat) : "";
  },
};

export default { dateFormat, dateFormatShort, dateAdapter };
