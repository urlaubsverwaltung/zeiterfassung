import {rimraf} from "rimraf";
import postcss from "rollup-plugin-postcss";
import resolve from "@rollup/plugin-node-resolve";
import commonjs from "@rollup/plugin-commonjs";
import esbuild from "rollup-plugin-esbuild";
import svelte from "rollup-plugin-svelte";
import sveltePreprocess from "svelte-preprocess";
import glob from "fast-glob";

const paths = {
  src: "src/main/javascript",
  dist: "target/classes/static/assets",
};

export default {
  input: {
    "custom-elements-polyfill": `@ungap/custom-elements`,
    ...inputFiles(),
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

function inputFiles() {
  const files = glob.sync(`${paths.src}/bundles/*.ts`);
  return Object.fromEntries(files.map((file) => [entryName(file), file]));
}

function entryName(file) {
  const filename = file.slice(Math.max(0, file.lastIndexOf("/") + 1));
  return filename.slice(0, Math.max(0, filename.lastIndexOf(".")));
}
