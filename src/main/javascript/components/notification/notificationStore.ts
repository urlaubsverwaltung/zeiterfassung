import type { Readable, Writable } from "svelte/store";
import { readable, writable } from "svelte/store";

export type Notification = {
  title: string;
  bodyHtml: string;
  iconHtml: string;
  onClose?: () => void;
  autoClose?: boolean;
};

const notificationWritable: Writable<Array<Notification>> = writable([]);

/**
 * Adds a new notification.
 *
 * @returns a function to remove the notification
 *
 * @param notification
 */
export function addNotification(notification: Notification): () => void {
  notificationWritable.update((state) => {
    state.push(notification);
    return state;
  });

  return (): void => {
    removeNotification(notification);
  };
}

export function removeNotification(notification: Notification): void {
  notificationWritable.update((state) => {
    const index = state.indexOf(notification);
    if (index >= 0) {
      const nextState = [...state];
      nextState.splice(index, 1);
      return nextState;
    } else {
      return state;
    }
  });
}

/**
 * Observable which holds current notifications.
 */
export const notificationStore: Readable<Array<Notification>> = readable(
  [],
  (update) => {
    const unsubscribe = notificationWritable.subscribe((state) => {
      update(state);
    });

    return () => {
      unsubscribe();
    };
  },
);
