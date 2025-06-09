/**
 * Adds a global `submit` listener and disables the submitter.
 */
export function initPreventDoubleClickSubmit() {
  globalThis.addEventListener("submit", function (event) {
    if (!event.defaultPrevented) {
      // delay setting disabled attribute
      // otherwise the request does not contain the name attribute of the submitter
      setTimeout(() => {
        event.submitter?.setAttribute("disabled", "");
      });
      // full page reload renders the form again with enabled submitter.
      // maybe @hotwired/turbo is enabled somewhere. in this case, however, turbo
      // handles the disabled attribute of the submitter.
    }
  });
}
