const userSettingsForm = document.querySelector(
  "#user-settings-form",
) as HTMLFormElement;

const state = {
  isLanguageGroupFocused: false,
  isLanguageGroupFocusedWithKeyboard: false,
};

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
    state.isLanguageGroupFocused = true;
  }
});

languageFieldset.addEventListener("focusout", function (event) {
  const target = event.target as HTMLInputElement;
  if (target.matches("[name='locale']")) {
    state.isLanguageGroupFocused = false;
  }
});

userSettingsForm.addEventListener("change", function (event) {
  const target = event.target as HTMLInputElement;
  if (state.isLanguageGroupFocusedWithKeyboard) {
    focusManager.memoize();
  }
  if (target.name === "locale") {
    userSettingsForm.submit();
  }
});

addEventListener("keyup", function (event) {
  if (!state.isLanguageGroupFocused) {
    focusManager.clean();
  }
  state.isLanguageGroupFocusedWithKeyboard =
    state.isLanguageGroupFocused && event.key === "Tab";
});

addEventListener("click", function (event) {
  if (!childOfLanguage(event.target as HTMLElement)) {
    focusManager.clean();
  }
});

function childOfLanguage(element: HTMLElement | null | undefined) {
  let current = element;
  while (current) {
    if (current.matches("#fieldset-language")) {
      return true;
    }
    current = current.parentElement;
  }
  return false;
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
