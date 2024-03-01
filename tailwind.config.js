const plugin = require("tailwindcss/plugin");

module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
    "./src/main/resources/templates/**/*.svg",
    "./src/main/javascript/**/*.ts",
    "./src/main/javascript/**/*.svelte",
  ],
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        logo: ["KaushanScript"],
      },
      spacing: {
        6.5: "1.625rem",
        7.5: "1.875rem",
        8.5: "2.125rem",
        13: "3.25rem",
        18: "4.5rem",
        21: "5.25rem",
      },
      screens: {
        xs: "480px",
        "2xl": "1440px",
        "3xl": "1600px",
      },
      backgroundColor: {
        "timeslot-form-day": "var(--timeslot-form-day-background-color)",
        "timeslot-form-datepicker":
          "var(--timeslot-form-datepicker-background-color)",
      },
      borderColor: {
        "timeslot-form": "var(--timeslot-form-border-color)",
      },
      textColor: {
        "timeslot-form": "var(--timeslot-form-color)",
        "timeslot-form-datepicker-background":
          "var(--timeslot-form-datepicker-background-color)",
        "timeslot-form-day": "var(--timeslot-form-day-color)",
        "timeslot-form-input": "var(--timeslot-form-input-color)",
        "timeslot-form-label": "var(--timeslot-form-label-color)",
      },
    },
  },
  plugins: [
    plugin(function ({ addVariant }) {
      addVariant(
        "supports-backdrop-blur",
        "@supports (backdrop-filter: blur(0)) or (-webkit-backdrop-filter: blur(0))",
      );
    }),
  ],
};
