import FeedbackHeart from "./FeedbackHeart.svelte";
import type { SvelteComponent } from "svelte";

type Arguments = {
  target: HTMLElement;
  props?: {
    showFeedbackKudo?: boolean;
  };
};

export function initFeedbackHeartView({
  target,
  props,
}: Arguments): SvelteComponent {
  return new FeedbackHeart({
    target,
    props: { showInitialHeart: props.showFeedbackKudo },
  });
}
