type TurboClickEventDetail = {
  url: string;
  originalEvent: MouseEvent;
};

export function onTurboClick(
  callback: (event: CustomEvent<TurboClickEventDetail>) => void,
  options?: EventListenerOptions,
) {
  document.addEventListener("turbo:click", callback, options);
}

type TurboRenderEventDetail = {
  renderMethod: "replace" | "morph";
};

export function onTurboRender(
  callback: (event: CustomEvent<TurboRenderEventDetail>) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:render", callback, options);
}
