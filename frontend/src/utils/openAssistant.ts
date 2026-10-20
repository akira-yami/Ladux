export function openAssistant() {
  window.dispatchEvent(new CustomEvent("ladux:open-assistant"));
}
