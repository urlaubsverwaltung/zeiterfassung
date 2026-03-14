import "../components/details-dropdown";
import "../components/feedback-form";
import { enhanceModal } from "../components/modal";
import "../components/navigation";
import "../components/time-clock";
import "../components/tooltip";
import { initPreventDoubleClickSubmit } from "../components/form";
import { FeedbackHeart } from "../components/feedback-heart";

enhanceModal();
initPreventDoubleClickSubmit();

const feedbackHeart = new FeedbackHeart();
feedbackHeart.classList.add("fixed", "left-0", "bottom-0", "p-2");
document.body.append(feedbackHeart);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
if ((globalThis as any).zeiterfassung?.feedback?.showFeedbackKudo) {
  feedbackHeart.unleash();
}
