const cssnano = require("cssnano");

const isProduction = process.env.NODE_ENV === "production";

module.exports = {
  plugins: [
    require("tailwindcss"),
    require("autoprefixer"),
    isProduction && cssnano({ preset: "default" }),
  ].filter(Boolean),
};
