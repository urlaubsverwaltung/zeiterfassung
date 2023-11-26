import OvertimeSankey from "./OvertimeSankey.svelte";
import type { SvelteComponent } from "svelte";

type Arguments = {
  target: HTMLElement;
  props?: {
    //
  };
};

export function initOvertimeSankeyView({
  target,
  props,
}: Arguments): SvelteComponent {
  return new OvertimeSankey({
    target,
    props: {
      ...props,
    },
  });
}
