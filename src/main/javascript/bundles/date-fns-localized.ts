import { setLocale } from "../date-fns-locale-aware/locale-resolver";
// we have to find a way for bundle optimizations as soon as there are more locales
// dynamic import is not an option because `setLocale` must happen before the first `date-fn/format` call!
// otherwise the default (en) is used to format a date.
import DE from "date-fns/locale/de";

if (window.navigator.language.slice(0, 2) === "de") {
  setLocale(DE);
}
