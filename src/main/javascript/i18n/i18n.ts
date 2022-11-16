export function i18n(key: string): string {
  let value = globalThis.zeiterfassung.i18n[key];
  if (value === null || value === undefined || value === "") {
    console.log(`could not find i18n value for '${key}'. using key as value.`);
    value = key;
  }
  return value;
}
