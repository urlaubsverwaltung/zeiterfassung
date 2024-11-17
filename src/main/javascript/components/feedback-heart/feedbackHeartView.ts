import FeedbackHeart from "./FeedbackHeart.svelte";
import { mount } from "svelte";

type Arguments = {
  target: HTMLElement;
  props?: {
    showFeedbackKudo?: boolean;
  };
};

export function initFeedbackHeartView({ target, props }: Arguments): void {
  mount(FeedbackHeart, {
    target,
    props: {
      showInitialHeart: props.showFeedbackKudo,
    },
  });
}
