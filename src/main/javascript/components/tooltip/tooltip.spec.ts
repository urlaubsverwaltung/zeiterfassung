import { afterEach, describe, expect, test, vi } from "vitest";
import { Tooltip } from "./tooltip";
import type { Instance } from "@popperjs/core";
import * as popper from "@popperjs/core";

vi.spyOn(popper, "createPopper");

describe("tooltip", () => {
  afterEach(() => {
    while (document.body.firstChild) {
      document.body.firstChild.remove();
    }
  });

  test("sets role attribute", () => {
    document.body.innerHTML = '<div id="parent"></div>';

    const paragraph = document.createElement("p");
    paragraph.textContent = "tooltip text";

    const tooltip = new Tooltip();
    expect(tooltip.getAttribute("role")).toBe("tooltip");
  });

  test("initializes tooltip on mount", () => {
    document.body.innerHTML = '<div id="parent"></div>';
    const tooltip = anyTooltip();

    expect(popper.createPopper).not.toHaveBeenCalled();

    document.querySelector("#parent").append(tooltip);
    expect(popper.createPopper).toHaveBeenCalled();
  });

  test.each([["mouseenter"], ["focus"]])("shows tooltip on %s", (eventName) => {
    const popperInstance = {
      update: vi.fn(),
    };
    vi.mocked(popper.createPopper).mockReturnValue(
      popperInstance as unknown as Instance,
    );

    document.body.innerHTML = '<div id="parent"></div>';
    const root = document.querySelector("#parent");
    const tooltip = anyTooltip();
    root.append(tooltip);

    expect(tooltip.dataset.show).not.toBeDefined();

    root.dispatchEvent(new Event(eventName));
    expect(tooltip.dataset.show).toBeDefined();
    expect(popperInstance.update).toHaveBeenCalledOnce();
  });

  test.each([["mouseleave"], ["blur"]])("hides tooltip on %s", (eventName) => {
    document.body.innerHTML = '<div id="parent"></div>';
    const root = document.querySelector("#parent");
    const tooltip = anyTooltip();
    root.append(tooltip);

    tooltip.dataset.show = "";

    root.dispatchEvent(new Event(eventName));
    expect(tooltip.dataset.show).not.toBeDefined();
  });
});

function anyTooltip() {
  const paragraph = document.createElement("p");
  paragraph.textContent = "tooltip text";

  const tooltip = new Tooltip();
  tooltip.append(paragraph);

  return tooltip;
}
