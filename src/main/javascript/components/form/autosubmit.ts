let keyupSubmit;

document.addEventListener("search", function (event) {
  const target = event.target as HTMLElement;
  if (
    target.matches("input") &&
    target.matches("[data-auto-submit]") &&
    (target as HTMLInputElement).value === ""
  ) {
    autosubmitForm(target);
  }
});

document.addEventListener("keyup", function (event) {
  if (
    event.defaultPrevented ||
    event.metaKey ||
    whitespaceKeys.has(event.key) ||
    modifierKeys.has(event.key) ||
    navigationKeys.has(event.key) ||
    uiKeys.has(event.key) ||
    deviceKeys.has(event.key) ||
    functionKeys.has(event.key) ||
    mediaKeys.has(event.key) ||
    audioControlKeys.has(event.key)
  ) {
    return;
  }

  autosubmitForm(event.target as HTMLElement);
});

function autosubmitForm(element: HTMLElement) {
  const { autoSubmit = "", autoSubmitDelay = 0 } = element.dataset;

  if (autoSubmit) {
    const button = document.querySelector(
      "#" + autoSubmit,
    ) as HTMLButtonElement;
    if (button) {
      const submit = () => button.click();
      if (autoSubmitDelay) {
        clearTimeout(keyupSubmit);
        keyupSubmit = setTimeout(submit, Number(autoSubmitDelay));
      } else {
        submit();
      }
    }
  }
}

document.addEventListener("change", function (event) {
  if (event.defaultPrevented) {
    return;
  }

  const target = event.target as HTMLElement;
  const { autoSubmit = "" } = target.dataset;

  if (autoSubmit && !target.matches("input") && !target.matches("textarea")) {
    const button = document.querySelector(
      "#" + autoSubmit,
    ) as HTMLButtonElement;
    if (button) {
      button.closest("form").requestSubmit(button);
    }
  }
});

const whitespaceKeys = new Set(["Enter", "Tab", "Alt"]);

const modifierKeys = new Set([
  "AltGraph",
  "CapsLock",
  "Control",
  "Fn",
  "FnLock",
  "Hyper",
  "Meta",
  "NumLock",
  "ScrollLock",
  "Shift",
  "Super",
  "Symbol",
  "SymbolLock",
]);

const navigationKeys = new Set([
  "ArrowDown",
  "ArrowLeft",
  "ArrowRight",
  "ArrowUp",
  "End",
  "Home",
  "PageDown",
  "PageUp",
  "End",
]);

const uiKeys = new Set([
  "Accept",
  "ContextMenu",
  "Execute",
  "Find",
  "Help",
  "Pause",
  "Play",
  "Props",
  "Select",
  "ZoomIn",
  "ZoomOut",
]);

const deviceKeys = new Set([
  "BrightnessDown",
  "BrightnessUp",
  "Eject",
  "LogOff",
  "Power",
  "PowerOff",
  "PrintScreen",
  "Hibernate",
  "Standby",
  "WakeUp",
]);

const functionKeys = new Set([
  "F1",
  "F2",
  "F3",
  "F4",
  "F5",
  "F6",
  "F7",
  "F8",
  "F9",
  "F10",
  "F11",
  "F12",
  "F13",
  "F14",
  "F15",
  "F16",
  "F17",
  "F18",
  "F19",
  "F20",
  "Soft1",
  "Soft2",
  "Soft3",
  "Soft4",
]);

const mediaKeys = new Set([
  "ChannelDown",
  "ChannelUp",
  "MediaFastForward",
  "MediaPause",
  "MediaPlay",
  "MediaPlayPause",
  "MediaRecord",
  "MediaRewind",
  "MediaStop",
  "MediaTrackNext",
  "MediaTrackPrevious",
]);

const audioControlKeys = new Set([
  "AudioVolumeDown",
  "AudioVolumeMute",
  "AudioVolumeUp",
  "MicrophoneToggle",
  "MicrophoneVolumeDown",
  "MicrophoneVolumeMute",
  "MicrophoneVolumeUp",
]);

export {};
