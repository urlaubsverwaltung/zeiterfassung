/**
 * Adds a global `submit` listener and disables the submitter.
 */
export function initPreventDoubleClickSubmit() {
  globalThis.addEventListener("submit", function (event) {
    if (!event.defaultPrevented) {
      event.submitter?.setAttribute("disabled", "");
      // full page reload renders the form again with enabled submitter.
      // maybe @hotwired/turbo is enabled somewhere. in this case, however, turbo
      // handles the disabled attribute of the submitter.
    }
  });
}
