const userSettingsForm = document.querySelector(
  "#user-settings-form",
) as HTMLFormElement;

let isLanguageGroupFocused = false;
let isLanguageGroupFocusedWithKeyboard = false;

const focusManager = createFocusManager();
const languageFieldset = userSettingsForm.querySelector(
  "#fieldset-language",
) as HTMLFieldSetElement;

if (focusManager.shouldFocusAfterReload()) {
  const checkedLocaleInput = languageFieldset.querySelector(
    "input[name='locale']:checked",
  ) as HTMLInputElement;
  checkedLocaleInput!.focus();
}

// `focusin` event listener is called before `keyup`
languageFieldset.addEventListener("focusin", function (event) {
  const target = event.target as HTMLInputElement;
  if (target.matches("[name='locale']")) {
    isLanguageGroupFocused = true;
  }
});

languageFieldset.addEventListener("focusout", function (event) {
  const target = event.target as HTMLInputElement;
  if (target.matches("[name='locale']")) {
    isLanguageGroupFocused = false;
  }
});

userSettingsForm.addEventListener("change", function (event) {
  const target = event.target as HTMLInputElement;
  if (isLanguageGroupFocusedWithKeyboard) {
    focusManager.memoize();
  }
  if (target.name === "locale") {
    userSettingsForm.submit();
  }
});

globalThis.addEventListener("keyup", function (event) {
  if (!isLanguageGroupFocused) {
    focusManager.clean();
  }
  isLanguageGroupFocusedWithKeyboard =
    isLanguageGroupFocused && event.key === "Tab";
});

globalThis.addEventListener("click", function (event) {
  if (!childOfLanguage(event.target as HTMLElement)) {
    focusManager.clean();
  }
});

function childOfLanguage(element: HTMLElement | null | undefined) {
  if (!element) {
    return false;
  }
  if (element.matches("#fieldset-language")) {
    return true;
  }
  return childOfLanguage(element.parentElement);
}

function createFocusManager() {
  const focusSessionKey = "uv--focus-language-after-reload";
  const yep = "true";

  return {
    memoize() {
      sessionStorage.setItem(focusSessionKey, yep);
    },

    shouldFocusAfterReload() {
      return sessionStorage.getItem(focusSessionKey) === yep;
    },

    clean() {
      sessionStorage.removeItem(focusSessionKey);
    },
  };
}
