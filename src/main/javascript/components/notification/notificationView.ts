import NotificationCenter from "./NotificationCenter.svelte";
import type { SvelteComponent } from "svelte";

type Arguments = {
  target: HTMLElement;
};

export function initNotificationView({ target }: Arguments): SvelteComponent {
  return new NotificationCenter({ target });
}
