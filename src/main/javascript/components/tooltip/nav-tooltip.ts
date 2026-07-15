/**
 * Imperative, delegation-based tooltip used by the collapsed navigation.
 *
 * A single shared popover element is reused for every anchor. Anchors opt in via a `title`
 * (migrated to `data-title` so the browser doesn't render its own tooltip) and an optional
 * `data-tooltip-delay`. This is intentionally separate from the declarative `z-tooltip`
 * web component: the navigation needs to add/remove tooltips imperatively as it collapses
 * and expands, and wants a hover delay so they don't flash while scanning the rail.
 *
 * https://www.nngroup.com/articles/tooltip-guidelines/
 */

const HOVER_SHOW_DELAY_MS = 300;
const FADE_OUT_MS = 100;
const SLIDE_MS = 150;
const TOOLTIP_ID = "nav-tooltip";
const ANCHOR_ACTIVE_CLASS = "nav-tooltip-anchor--active";
const TOOLTIP_HIDING_CLASS = "nav-tooltip--is-hiding";

type State = "idle" | "open" | "pendingShow" | "pendingHide";

type TooltipPlacement = "top" | "right";

type TooltipOptions = {
  text: string;
  placement?: TooltipPlacement;
  delay?: number;
};

let tooltip: HTMLElement | undefined;
let showTimerId: ReturnType<typeof setTimeout> | undefined;
let hideTimerId: ReturnType<typeof setTimeout> | undefined;
let activeAnchor: HTMLElement | undefined;
let pendingAnchor: HTMLElement | undefined;
let slideAnim: Animation | undefined;
let state: State = "idle";

export function setup(): void {
  if (!tooltip) {
    tooltip = document.createElement("div");
    tooltip.id = TOOLTIP_ID;
    tooltip.setAttribute("role", "tooltip");
    tooltip.setAttribute("popover", "hint");
    document.body.append(tooltip);
  }
  document.addEventListener("mouseover", onPointerEnter);
  document.addEventListener("mouseout", onPointerLeave);
  document.addEventListener("focusin", onPointerEnter);
  document.addEventListener("focusout", onPointerLeave);
}

export function teardown(): void {
  document.removeEventListener("mouseover", onPointerEnter);
  document.removeEventListener("mouseout", onPointerLeave);
  document.removeEventListener("focusin", onPointerEnter);
  document.removeEventListener("focusout", onPointerLeave);
  clearTimeout(showTimerId);
  clearTimeout(hideTimerId);
  showTimerId = undefined;
  hideTimerId = undefined;
  pendingAnchor = undefined;
  if (slideAnim) {
    slideAnim.cancel();
    slideAnim = undefined;
  }
  if (activeAnchor) {
    activeAnchor.classList.remove(ANCHOR_ACTIVE_CLASS);
    activeAnchor.removeAttribute("aria-describedby");
    activeAnchor = undefined;
  }
  if (tooltip) {
    tooltip.classList.remove(TOOLTIP_HIDING_CLASS);
    tooltip.remove();
    tooltip = undefined;
  }
  state = "idle";
}

function onPointerEnter(event: MouseEvent | FocusEvent): void {
  const anchor = closestTooltipAnchor(event.target as HTMLElement);
  if (!anchor || isInternalCrossing(event, anchor)) {
    return;
  }
  if (anchor === activeAnchor && state !== "pendingHide") {
    return;
  }
  if (state === "open" || state === "pendingHide") {
    handoffTo(anchor);
    return;
  }
  clearTimeout(showTimerId);
  pendingAnchor = anchor;
  state = "pendingShow";
  if (event.type === "focusin") {
    showOn(anchor);
  } else {
    const delay = anchor.dataset.tooltipDelay;
    showTimerId = setTimeout(
      function () {
        showOn(anchor);
      },
      delay === undefined ? HOVER_SHOW_DELAY_MS : Number(delay),
    );
  }
}

function onPointerLeave(event: MouseEvent | FocusEvent): void {
  const anchor = closestTooltipAnchor(event.target as HTMLElement);
  if (!anchor || isInternalCrossing(event, anchor)) {
    return;
  }
  if (anchor === pendingAnchor) {
    clearTimeout(showTimerId);
    showTimerId = undefined;
    pendingAnchor = undefined;
    state = "idle";
    return;
  }
  if (anchor === activeAnchor) {
    beginHide();
  }
}

function isInternalCrossing(
  event: MouseEvent | FocusEvent,
  anchor: HTMLElement,
): boolean {
  return (
    event.relatedTarget instanceof Node && anchor.contains(event.relatedTarget)
  );
}

function handoffTo(anchor: HTMLElement): void {
  clearTimeout(hideTimerId);
  hideTimerId = undefined;
  tooltip?.classList.remove(TOOLTIP_HIDING_CLASS);
  retargetTo(anchor);
  state = "open";
}

function beginHide() {
  clearTimeout(showTimerId);
  if (state !== "open") {
    return;
  }
  state = "pendingHide";
  tooltip?.classList.add(TOOLTIP_HIDING_CLASS);
  hideTimerId = setTimeout(finalizeHide, FADE_OUT_MS);
}

function finalizeHide(): void {
  hideTimerId = undefined;
  tooltip?.classList.remove(TOOLTIP_HIDING_CLASS);
  tooltip?.hidePopover();
  if (activeAnchor) {
    activeAnchor.classList.remove(ANCHOR_ACTIVE_CLASS);
    activeAnchor.removeAttribute("aria-describedby");
    activeAnchor = undefined;
  }
  state = "idle";
}

function showOn(anchor: HTMLElement): void {
  pendingAnchor = undefined;
  showTimerId = undefined;
  retargetTo(anchor);
  tooltip?.showPopover();
  state = "open";
}

function retargetTo(anchor: HTMLElement): void {
  if (!tooltip) {
    return;
  }
  const isHandoff = activeAnchor && activeAnchor !== anchor;
  const previousAnchorRect = isHandoff
    ? activeAnchor!.getBoundingClientRect()
    : undefined;

  if (slideAnim) {
    slideAnim.cancel();
    slideAnim = undefined;
  }

  if (isHandoff) {
    activeAnchor!.classList.remove(ANCHOR_ACTIVE_CLASS);
    activeAnchor!.removeAttribute("aria-describedby");
  }
  ensureMigratedTitle(anchor);
  anchor.classList.add(ANCHOR_ACTIVE_CLASS);
  anchor.setAttribute("aria-describedby", TOOLTIP_ID);
  tooltip.textContent = anchor.dataset.title ?? "";
  const placement =
    anchor.dataset.tooltipPlacement === "right" ? "right" : "top";
  tooltip.dataset.placement = placement;
  activeAnchor = anchor;

  if (previousAnchorRect && !prefersReducedMotion()) {
    const nextAnchorRect = anchor.getBoundingClientRect();
    // both ends of a handoff share the same placement, so the reference point follows it
    let dx;
    let dy;
    if (placement === "right") {
      // tooltip is left-aligned right of anchor.right and vertically centered on the anchor
      dx = previousAnchorRect.right - nextAnchorRect.right;
      dy =
        previousAnchorRect.top +
        previousAnchorRect.height / 2 -
        (nextAnchorRect.top + nextAnchorRect.height / 2);
    } else {
      // tooltip is centered horizontally over the anchor and bottom-aligned above anchor.top
      dx =
        previousAnchorRect.left +
        previousAnchorRect.width / 2 -
        (nextAnchorRect.left + nextAnchorRect.width / 2);
      dy = previousAnchorRect.top - nextAnchorRect.top;
    }
    slideAnim = tooltip.animate(
      [{ translate: `${dx}px ${dy}px` }, { translate: "0 0" }],
      {
        duration: SLIDE_MS,
        easing: "ease-out",
        fill: "none",
      },
    );
  }
}

function prefersReducedMotion() {
  return (
    globalThis.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false
  );
}

function ensureMigratedTitle(anchor: HTMLElement): void {
  if (!anchor.hasAttribute("title")) {
    return;
  }

  anchor.dataset.title = anchor.getAttribute("title") ?? "";
  anchor.removeAttribute("title");
}

function closestTooltipAnchor(
  element: HTMLElement | null,
): HTMLElement | undefined {
  if (!element) {
    return undefined;
  }
  if (
    element.getAttribute &&
    (element.getAttribute("title") || element.dataset?.title)
  ) {
    return element;
  }
  return closestTooltipAnchor(element.parentElement);
}

/**
 * Prepares the given element to show a tooltip on hover.
 */
export function prepareTooltip(
  element: HTMLElement,
  { text, placement, delay }: TooltipOptions,
) {
  if (element.dataset.title) {
    element.dataset.title = text;
  } else {
    element.setAttribute("title", text);
  }
  if (delay !== undefined) {
    element.dataset.tooltipDelay = String(delay);
  }
  if (placement !== undefined) {
    element.dataset.tooltipPlacement = placement;
  }
}

/**
 * Removes everything tooltip related from the given element.
 */
export function disposeTooltip(element: HTMLElement): void {
  element.removeAttribute("title");
  delete element.dataset.title;
}
