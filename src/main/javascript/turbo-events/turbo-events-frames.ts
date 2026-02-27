import type {
  TurboBeforeFrameRenderEvent,
  TurboFrameRenderEvent,
} from "@hotwired/turbo";

export function onTurboBeforeFrameRender(
  callback: (event: TurboBeforeFrameRenderEvent) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:before-frame-render", callback, options);
}

export function onTurboFrameRender(
  callback: (event: TurboFrameRenderEvent) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:frame-render", callback, options);
}
