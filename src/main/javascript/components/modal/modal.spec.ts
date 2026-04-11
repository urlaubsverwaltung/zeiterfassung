import { enhanceModal } from "./modal";
import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  it,
  vi,
  vitest,
} from "vitest";
import { preserveScrollOnce } from "./modal-preserve-scroll-once";
import {
  FrameElement,
  TurboBeforeFrameRenderEvent,
  TurboFrameRenderEvent,
} from "@hotwired/turbo";

vi.mock("./modal-preserve-scroll-once");

describe("modal", () => {
  type Listener<E> = (event: Partial<E>) => void;
  type NoArgumentListener = () => void;

  let popstateListener: NoArgumentListener;
  let clickListener: Listener<MouseEvent>;
  let keyupListener: Listener<KeyboardEvent>;
  let turboBeforeFrameRenderListener: Listener<TurboBeforeFrameRenderEvent>;
  let turboFrameRenderListener: Listener<TurboFrameRenderEvent>;

  let activeElement: HTMLElement;

  beforeAll(() => {
    Object.defineProperty(document, "activeElement", {
      get() {
        return activeElement;
      },
    });

    vitest.spyOn(history, "back");

    vitest
      .spyOn(globalThis, "addEventListener")
      .mockImplementation((event, handler) => {
        if (event === "popstate") {
          popstateListener = handler as NoArgumentListener;
        }
      });

    vitest
      .spyOn(document, "addEventListener")
      .mockImplementation((event, handler) => {
        if (event === "click") {
          clickListener = handler as Listener<MouseEvent>;
        }
        if (event === "keyup") {
          keyupListener = handler as Listener<KeyboardEvent>;
        }
        if (event === "turbo:before-frame-render") {
          turboBeforeFrameRenderListener =
            handler as Listener<TurboBeforeFrameRenderEvent>;
        }
        if (event === "turbo:frame-render") {
          turboFrameRenderListener = handler as Listener<TurboFrameRenderEvent>;
        }
      });
  });

  beforeEach(() => {
    enhanceModal();
  });

  afterEach(() => {
    while (document.body.firstChild) {
      document.body.firstChild.remove();
    }
    vitest.clearAllMocks();
    activeElement = undefined as unknown as HTMLElement;
    popstateListener = undefined as unknown as () => void;
    clickListener = undefined as unknown as () => void;
    keyupListener = undefined as unknown as () => void;
    turboBeforeFrameRenderListener = undefined as unknown as () => void;
  });

  it("adds event listeners", () => {
    expect(popstateListener).toBeDefined();
    expect(clickListener).toBeDefined();
    expect(keyupListener).toBeDefined();
    expect(turboBeforeFrameRenderListener).toBeDefined();
  });

  describe("popstate", () => {
    it("does not call turbo preserveScrollOnce when opened dialog is not present", () => {
      document.body.innerHTML = "<dialog>dialog content</dialog>";

      popstateListener();
      expect(preserveScrollOnce).not.toHaveBeenCalled();
    });

    it("calls turbo preserveScrollOnce when dialog is opened", () => {
      document.body.innerHTML = "<dialog open>dialog content</dialog>";

      popstateListener();
      expect(preserveScrollOnce).toHaveBeenCalled();
    });
  });

  describe("click", () => {
    it("is not handled when there is no dialog", () => {
      const target = document.createElement("div");
      document.body.append(target);

      const event = {
        preventDefault: vi.fn(),
        target,
      };
      clickListener(event);

      expect(event.preventDefault).not.toHaveBeenCalled();
      expectCloseModalNotToHaveBeenCalled();
    });

    it("is not handled when 'data-modal-close-button' has not been clicked", () => {
      const button = document.createElement("button");
      const dialog = document.createElement("dialog");
      dialog.append(button);

      document.body.append(dialog);

      const event = {
        preventDefault: vi.fn(),
        target: button,
      };
      clickListener(event);

      expect(event.preventDefault).not.toHaveBeenCalled();
      expectCloseModalNotToHaveBeenCalled();
    });

    it("closes the dialog when modal close button is clicked", () => {
      const buttonInner = document.createElement("span");
      buttonInner.textContent = "close me";

      const button = document.createElement("button");
      button.dataset.modalCloseButton = "";
      button.append(buttonInner);

      const dialog = document.createElement("dialog");
      dialog.append(button);

      document.body.append(dialog);

      const event = {
        preventDefault: vi.fn(),
        target: buttonInner,
      };
      clickListener(event);

      expect(event.preventDefault).toHaveBeenCalled();
      expectCloseModalHaveBeenCalled();
    });
  });

  describe("keyup", () => {
    it.each(["A", "b", "Enter", "Tab", " ", "ArrowUp", "ArrowDown"])(
      "is ignored for everything else but ESC (pressed: '%s')",
      (pressedKey) => {
        const event = {
          key: pressedKey,
        };
        keyupListener(event);
        expectCloseModalNotToHaveBeenCalled();
      },
    );

    it("is ignored when there is a focused input currently", () => {
      activeElement = document.createElement("input");

      const event = {
        key: "Escape",
      };
      keyupListener(event);

      expectCloseModalNotToHaveBeenCalled();
    });

    it("is ignored when there is no dialog element", () => {
      const event = {
        key: "Escape",
      };
      keyupListener(event);

      expectCloseModalNotToHaveBeenCalled();
    });

    it("closes the modal when Escape is pressed", () => {
      const dialog = document.createElement("dialog");
      document.body.append(dialog);

      const event = {
        key: "Escape",
      };
      keyupListener(event);

      expectCloseModalHaveBeenCalled();
    });
  });

  describe("turbo:before-frame-render", () => {
    it("is ignored when new frame does not match the modal frame", () => {
      const event = beforeFrameRenderEvent({});
      turboBeforeFrameRenderListener(event);
      expect(event.detail.render).toBe(undefined);
    });

    it("sets render function when modal frame is about to be rendered", () => {
      const event = beforeFrameRenderEvent({
        newFrame: createModalFrame(),
        render: undefined,
      });
      turboBeforeFrameRenderListener(event);
      expect(event.detail.render).toBeDefined();
    });

    it("attached render function does nothing when renderMethod != replace", () => {
      const newFrame = createModalFrame(`some <strong>new</strong> content`);

      const event = beforeFrameRenderEvent({
        newFrame,
        renderMethod: "not-replace",
      });
      turboBeforeFrameRenderListener(event);

      const currentFrame = document.createElement("turbo-frame");

      const modalContainer = createModalFrame();
      modalContainer.innerHTML = "content";
      document.body.append(modalContainer);

      event.detail.render(currentFrame, newFrame);

      // content should not be touched
      expect(modalContainer.innerHTML).toBe("content");
    });

    it("attached render function wraps modal with dialog when renderMethod = 'replace'", () => {
      const newFrame = createModalFrame(`some <strong>new</strong> content`);

      const event = beforeFrameRenderEvent({
        newFrame,
        render: () => {},
        renderMethod: "replace",
      });
      turboBeforeFrameRenderListener(event);

      const previousFrame = createModalFrame(`some content`);
      document.body.append(previousFrame);

      event.detail.render(previousFrame, newFrame);

      // already attached node is updated
      expect(previousFrame.innerHTML).toBe(
        `<dialog>some <strong>new</strong> content</dialog>`,
      );
    });

    it("opens dialog on frame-render", () => {
      const dialog = document.createElement("dialog");
      // jsdom(?) has no showModal on dialogs, therefore add function instead of spy
      dialog.showModal = vi.fn();

      const newFrame = createModalFrame();
      newFrame.append(dialog);

      const event = frameRenderEvent({
        target: newFrame,
      });
      turboFrameRenderListener(event);

      expect(dialog.showModal).toHaveBeenCalled();
    });

    it("does not open dialog on frame-render when target frame does not match", () => {
      const dialog = document.createElement("dialog");
      // jsdom(?) has no showModal on dialogs, therefore add function instead of spy
      dialog.showModal = vi.fn();

      const newFrame = document.createElement("turbo-frame");
      newFrame.append(dialog);

      const event = frameRenderEvent({
        target: newFrame,
      });
      turboFrameRenderListener(event);

      expect(dialog.showModal).not.toHaveBeenCalled();
    });

    it("updates history after dialog has been closed", () => {
      // closing the dialog results in empty frame-modal response
      const newFrame = createModalFrame();

      const event = beforeFrameRenderEvent({
        newFrame,
        render: undefined,
        renderMethod: "replace",
      });
      turboBeforeFrameRenderListener(event);

      const previousFrame = createModalFrame(`some content`);
      document.body.append(previousFrame);

      event.detail.render(previousFrame, newFrame);

      expect(history.back).toHaveBeenCalledOnce();
    });

    it("does not update history when next frame has content (e.g. editing with validation constraint errors)", () => {
      // closing the dialog results in empty frame-modal response
      const newFrame = createModalFrame(`next content`);

      const event = beforeFrameRenderEvent({
        newFrame,
        render: undefined,
        renderMethod: "replace",
      });
      turboBeforeFrameRenderListener(event);

      const previousFrame = createModalFrame(`some content`);
      document.body.append(previousFrame);

      event.detail.render(previousFrame, newFrame);

      expect(history.back).not.toHaveBeenCalled();
    });
  });
});

function beforeFrameRenderEvent(
  detail: Partial<
    TurboBeforeFrameRenderEvent["detail"] & { renderMethod: string }
  >,
) {
  return {
    detail: {
      newFrame: document.createElement("turbo-frame"),
      ...detail,
    },
  } as TurboBeforeFrameRenderEvent;
}

function frameRenderEvent(event: Partial<TurboFrameRenderEvent>) {
  return {
    target: document.createElement("turbo-frame"),
    ...event,
  } as TurboFrameRenderEvent;
}

function createModalFrame(innerHtml?: string): FrameElement {
  const frame = document.createElement("turbo-frame");
  frame.setAttribute("id", "frame-modal");
  frame.innerHTML = innerHtml ?? "";
  return frame;
}

function expectCloseModalNotToHaveBeenCalled() {
  expect(preserveScrollOnce).not.toHaveBeenCalled();
  expect(history.back).not.toHaveBeenCalled();
}

function expectCloseModalHaveBeenCalled() {
  expect(preserveScrollOnce).toHaveBeenCalled();
  expect(history.back).toHaveBeenCalled();
}
