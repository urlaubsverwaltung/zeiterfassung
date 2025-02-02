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
import { preserveScrollOnce } from "../../turbo";

vi.mock("../../turbo");

describe("modal", () => {
  type Listener<E> = (event: Partial<E>) => void;
  type NoArgumentListener = () => void;

  let popstateListener: NoArgumentListener;
  let clickListener: Listener<MouseEvent>;
  let keyupListener: Listener<KeyboardEvent>;
  let turboBeforeFrameRenderListener: Listener<CustomEvent>;

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
          turboBeforeFrameRenderListener = handler as Listener<CustomEvent>;
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
    activeElement = undefined;
    popstateListener = undefined;
    clickListener = undefined;
    keyupListener = undefined;
    turboBeforeFrameRenderListener = undefined;
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
      const newFrame = document.createElement("div");
      const event = {
        detail: {
          newFrame,
          render: undefined,
        },
      };
      turboBeforeFrameRenderListener(event);
      expect(event.detail.render).toBe(undefined);
    });

    it("sets render function when modal frame is about to be rendered", () => {
      const newFrame = createModalFrame();

      const event = {
        detail: {
          newFrame,
          render: undefined,
        },
      };
      turboBeforeFrameRenderListener(event);
      expect(event.detail.render).toBeDefined();
    });

    it("attached render function does nothing when renderMethod != replace", () => {
      const newFrame = createModalFrame(`some <strong>new</strong> content`);

      const event = {
        detail: {
          newFrame,
          render: undefined,
          renderMethod: "not-replace",
        },
      };
      turboBeforeFrameRenderListener(event);

      const currentFrame = document.createElement("div");

      const modalContainer = createModalFrame();
      modalContainer.innerHTML = "content";
      document.body.append(modalContainer);

      event.detail.render(currentFrame, newFrame);

      // content should not be touched
      expect(modalContainer.innerHTML).toBe("content");
    });

    it("attached render function wraps modal with dialog when renderMethod = 'replace'", () => {
      const newFrame = createModalFrame(`some <strong>new</strong> content`);

      const event = {
        detail: {
          newFrame,
          render: undefined,
          renderMethod: "replace",
        },
      };
      turboBeforeFrameRenderListener(event);

      const previousFrame = createModalFrame(`some content`);
      document.body.append(previousFrame);

      event.detail.render(previousFrame, newFrame);

      // already attached node is updated
      expect(previousFrame.innerHTML).toBe(
        `<dialog open="">some <strong>new</strong> content</dialog>`,
      );
    });

    it("updates history after dialog has been closed", () => {
      // closing the dialog results in empty frame-modal response
      const newFrame = createModalFrame(``);

      const event = {
        detail: {
          newFrame,
          render: undefined,
          renderMethod: "replace",
        },
      };
      turboBeforeFrameRenderListener(event);

      const previousFrame = createModalFrame(`some content`);
      document.body.append(previousFrame);

      event.detail.render(previousFrame, newFrame);

      expect(history.back).toHaveBeenCalledOnce();
    });

    it("does not update history when next frame has content (e.g. editing with validation constraint errors)", () => {
      // closing the dialog results in empty frame-modal response
      const newFrame = createModalFrame(`next content`);

      const event = {
        detail: {
          newFrame,
          render: undefined,
          renderMethod: "replace",
        },
      };
      turboBeforeFrameRenderListener(event);

      const previousFrame = createModalFrame(`some content`);
      document.body.append(previousFrame);

      event.detail.render(previousFrame, newFrame);

      expect(history.back).not.toHaveBeenCalled();
    });
  });
});

function createModalFrame(innerHtml?: string) {
  const frame = document.createElement("div");
  frame.setAttribute("id", "frame-modal");
  frame.innerHTML = innerHtml;
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
