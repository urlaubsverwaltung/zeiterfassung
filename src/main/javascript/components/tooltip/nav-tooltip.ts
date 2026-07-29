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

const tooltipState: {
  tooltip: HTMLElement | undefined;
  showTimerId: ReturnType<typeof setTimeout> | undefined;
  hideTimerId: ReturnType<typeof setTimeout> | undefined;
  activeAnchor: HTMLElement | undefined;
  pendingAnchor: HTMLElement | undefined;
  slideAnim: Animation | undefined;
  state: State;
} = {
  tooltip: undefined,
  showTimerId: undefined,
  hideTimerId: undefined,
  activeAnchor: undefined,
  pendingAnchor: undefined,
  slideAnim: undefined,
  state: "idle",
};

export function setup(): void {
  if (!tooltipState.tooltip) {
    const tooltip = document.createElement("div");
    tooltip.id = TOOLTIP_ID;
    tooltip.setAttribute("role", "tooltip");
    tooltip.setAttribute("popover", "hint");
    document.body.append(tooltip);
    tooltipState.tooltip = tooltip;
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
  clearTimeout(tooltipState.showTimerId);
  clearTimeout(tooltipState.hideTimerId);
  tooltipState.showTimerId = undefined;
  tooltipState.hideTimerId = undefined;
  tooltipState.pendingAnchor = undefined;
  if (tooltipState.slideAnim) {
    tooltipState.slideAnim.cancel();
    tooltipState.slideAnim = undefined;
  }
  if (tooltipState.activeAnchor) {
    tooltipState.activeAnchor.classList.remove(ANCHOR_ACTIVE_CLASS);
    tooltipState.activeAnchor.removeAttribute("aria-describedby");
    tooltipState.activeAnchor = undefined;
  }
  if (tooltipState.tooltip) {
    tooltipState.tooltip.classList.remove(TOOLTIP_HIDING_CLASS);
    tooltipState.tooltip.remove();
    tooltipState.tooltip = undefined;
  }
  tooltipState.state = "idle";
}

function onPointerEnter(event: MouseEvent | FocusEvent): void {
  const anchor = closestTooltipAnchor(event.target as HTMLElement);
  if (!anchor || isInternalCrossing(event, anchor)) {
    return;
  }
  if (
    anchor === tooltipState.activeAnchor &&
    tooltipState.state !== "pendingHide"
  ) {
    return;
  }
  if (tooltipState.state === "open" || tooltipState.state === "pendingHide") {
    handoffTo(anchor);
    return;
  }
  clearTimeout(tooltipState.showTimerId);
  tooltipState.pendingAnchor = anchor;
  tooltipState.state = "pendingShow";
  if (event.type === "focusin") {
    showOn(anchor);
  } else {
    const delay = anchor.dataset.tooltipDelay;
    tooltipState.showTimerId = setTimeout(
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
  if (anchor === tooltipState.pendingAnchor) {
    clearTimeout(tooltipState.showTimerId);
    tooltipState.showTimerId = undefined;
    tooltipState.pendingAnchor = undefined;
    tooltipState.state = "idle";
    return;
  }
  if (anchor === tooltipState.activeAnchor) {
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
  clearTimeout(tooltipState.hideTimerId);
  tooltipState.hideTimerId = undefined;
  tooltipState.tooltip?.classList.remove(TOOLTIP_HIDING_CLASS);
  retargetTo(anchor);
  tooltipState.state = "open";
}

function beginHide() {
  clearTimeout(tooltipState.showTimerId);
  if (tooltipState.state !== "open") {
    return;
  }
  tooltipState.state = "pendingHide";
  tooltipState.tooltip?.classList.add(TOOLTIP_HIDING_CLASS);
  tooltipState.hideTimerId = setTimeout(finalizeHide, FADE_OUT_MS);
}

function finalizeHide(): void {
  tooltipState.hideTimerId = undefined;
  tooltipState.tooltip?.classList.remove(TOOLTIP_HIDING_CLASS);
  tooltipState.tooltip?.hidePopover();
  if (tooltipState.activeAnchor) {
    tooltipState.activeAnchor.classList.remove(ANCHOR_ACTIVE_CLASS);
    tooltipState.activeAnchor.removeAttribute("aria-describedby");
    tooltipState.activeAnchor = undefined;
  }
  tooltipState.state = "idle";
}

function showOn(anchor: HTMLElement): void {
  tooltipState.pendingAnchor = undefined;
  tooltipState.showTimerId = undefined;
  retargetTo(anchor);
  tooltipState.tooltip?.showPopover();
  tooltipState.state = "open";
}

function retargetTo(anchor: HTMLElement): void {
  if (!tooltipState.tooltip) {
    return;
  }
  const tooltip = tooltipState.tooltip;
  const isHandoff =
    tooltipState.activeAnchor && tooltipState.activeAnchor !== anchor;
  const previousAnchorRect = isHandoff
    ? tooltipState.activeAnchor!.getBoundingClientRect()
    : undefined;

  if (tooltipState.slideAnim) {
    tooltipState.slideAnim.cancel();
    tooltipState.slideAnim = undefined;
  }

  if (isHandoff) {
    tooltipState.activeAnchor!.classList.remove(ANCHOR_ACTIVE_CLASS);
    tooltipState.activeAnchor!.removeAttribute("aria-describedby");
  }
  ensureMigratedTitle(anchor);
  anchor.classList.add(ANCHOR_ACTIVE_CLASS);
  anchor.setAttribute("aria-describedby", TOOLTIP_ID);
  tooltip.textContent = anchor.dataset.title ?? "";
  const placement =
    anchor.dataset.tooltipPlacement === "right" ? "right" : "top";
  tooltip.dataset.placement = placement;
  tooltipState.activeAnchor = anchor;

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
    tooltipState.slideAnim = tooltip.animate(
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
  return matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false;
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
  let current = element;
  while (current) {
    if (
      current.getAttribute &&
      (current.getAttribute("title") || current.dataset?.title)
    ) {
      return current;
    }
    current = current.parentElement;
  }
  return undefined;
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
