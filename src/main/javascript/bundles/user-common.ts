import "../components/details-dropdown";
import "../components/feedback-form";
import { enhanceModal } from "../components/modal";
import "../components/navigation";
import "../components/time-clock";
import "../components/tooltip";
import {
  initAutosubmit,
  initPreventDoubleClickSubmit,
} from "../components/form";
import { FeedbackHeart } from "../components/feedback-heart";

enhanceModal();
initPreventDoubleClickSubmit();
initAutosubmit();

const feedbackHeart = new FeedbackHeart();
feedbackHeart.classList.add("fixed", "left-0", "bottom-0", "p-2");
document.body.append(feedbackHeart);

if (globalThis.zeiterfassung.feedback.showFeedbackKudo) {
  feedbackHeart.unleash();
}
