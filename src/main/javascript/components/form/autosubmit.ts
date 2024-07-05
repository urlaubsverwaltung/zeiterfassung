/**
 * Adds `input` and `change` event listeners and submits forms automatically.
 *
 * <p>Autosubmit can be configured with the `data-autosubmit` attribute on HTML elements.
 * ```html
 * <input type="text" data-autosubmit="submitter" data-auto-submit-delay="100" />
 * <button type="submit" id="submitter">Submit</button>
 * ```
 */
export function initAutosubmit() {
  let keyupSubmit;

  document.addEventListener("input", function (event: InputEvent) {
    if (event.defaultPrevented) {
      return;
    }

    const target = event.target as HTMLElement;
    const autoSubmit = target.dataset.autoSubmit;
    const autoSubmitDelay = Number.parseInt(
      target.dataset.autoSubmitDelay ?? "0",
      10,
    );

    if (autoSubmit) {
      const button: HTMLButtonElement = document.querySelector(
        "#" + autoSubmit,
      );
      if (button) {
        const submit = () => button.click();
        if (autoSubmitDelay) {
          clearTimeout(keyupSubmit);
          keyupSubmit = setTimeout(submit, autoSubmitDelay);
        } else {
          submit();
        }
      }
    }
  });

  document.addEventListener("change", function (event) {
    const target = event.target as HTMLElement;
    if (event.defaultPrevented || noTextInput(target)) {
      // `change` is not of interest for text inputs which are triggered by `keyup`
      return;
    }

    if ("autoSubmit" in target.dataset) {
      const autoSubmit = target.dataset.autoSubmit ?? "";
      const element: HTMLElement = autoSubmit
        ? document.querySelector("#" + autoSubmit)
        : target.closest("form");

      if (element instanceof HTMLFormElement) {
        element.requestSubmit();
      } else {
        element.closest("form").requestSubmit(element);
      }
    }
  });
}

function noTextInput(element: HTMLElement) {
  return [
    "input[type='text']",
    "input[type='mail']",
    "input[type='search']",
    "input[type='password']",
    "textarea",
  ].some((selector) => element.matches(selector));
}
