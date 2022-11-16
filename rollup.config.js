import rimraf from "rimraf";
import postcss from "rollup-plugin-postcss";
import resolve from "@rollup/plugin-node-resolve";
import commonjs from "@rollup/plugin-commonjs";
import esbuild from "rollup-plugin-esbuild";
import svelte from "rollup-plugin-svelte";
import sveltePreprocess from "svelte-preprocess";

const paths = {
  src: "src/main/javascript",
  dist: "target/classes/static/assets",
};

export default {
  input: {
    "custom-elements-polyfill": `@ungap/custom-elements`,
    "date-fns-localized": `${paths.src}/bundles/date-fns-localized.ts`,
    reports: `${paths.src}/bundles/reports.ts`,
    "time-entries": `${paths.src}/bundles/time-entries.ts`,
    turbo: `${paths.src}/bundles/turbo.ts`,
    "user-common": `${paths.src}/bundles/user-common.ts`,
  },
  output: {
    dir: paths.dist,
    format: "es",
    sourcemap: true,
    manualChunks: {
      "duet-date-picker": [
        "@duetds/date-picker",
        "@duetds/date-picker/custom-element",
      ],
      "date-fns": ["date-fns"],
    },
  },
  plugins: [
    {
      name: "clean",
      buildStart() {
        rimraf.sync(paths.dist);
      },
    },
    svelte({
      include: `${paths.src}/**/*.svelte`,
      preprocess: sveltePreprocess(),
    }),
    postcss(),
    resolve({
      preferBuiltins: false,
    }),
    commonjs(),
    esbuild({
      sourceMap: true,
      minify: process.env.NODE_ENV === "production",
    }),
  ],
};
