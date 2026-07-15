import { FetchResponse } from "@hotwired/turbo";
import { morphWithoutTouchingValueOfActiveElement } from "../../morph";

const frameSelector = "#frame-nav-time-clock-edit";

const state: { fetchResponse?: FetchResponse } = {};

document.addEventListener("turbo:before-fetch-response", function (event) {
  state.fetchResponse = event.detail.fetchResponse;
});

document.addEventListener("turbo:before-frame-render", function (event) {
  if (
    event.detail.newFrame.matches(frameSelector) &&
    state.fetchResponse?.statusCode === 422
  ) {
    // update the popover children only. otherwise there is undefined behavior with the popover element.
    event.detail.render = function (current, next) {
      morphWithoutTouchingValueOfActiveElement(
        current.querySelector("form")!,
        next.querySelector("form")!,
      );
    };
  }
});
