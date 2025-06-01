import type { TurboFetchResponse } from "./turbo-events-http-requests";

type BeforeFrameRenderEventDetail = {
  newFrame: HTMLElement;
  resume(): void;
  render(currentFrame: HTMLElement, newFrame: HTMLElement): void;
};

export function onTurboBeforeFrameRender(
  callback: (event: CustomEvent<BeforeFrameRenderEventDetail>) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:before-frame-render", callback, options);
}

type FrameRenderEventDetail = {
  fetchResponse: TurboFetchResponse;
};

export function onTurboFrameRender(
  callback: (event: CustomEvent<FrameRenderEventDetail>) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:frame-render", callback, options);
}
