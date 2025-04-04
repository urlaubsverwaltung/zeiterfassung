import ts from "typescript-eslint";
import { globalIgnores } from "eslint/config";
import js from "@eslint/js";
import globals from "globals";
import eslintPluginUnicorn from "eslint-plugin-unicorn";
import importPlugin from "eslint-plugin-import";
import eslintPluginPrettierRecommended from "eslint-plugin-prettier/recommended";
import sveltePlugin from "eslint-plugin-svelte";
import svelteParser from "svelte-eslint-parser";

export default ts.config(
  globalIgnores(["target"]),

  js.configs.recommended,
  ts.configs.recommended,
  eslintPluginUnicorn.configs["flat/recommended"],
  eslintPluginPrettierRecommended,
  importPlugin.flatConfigs.recommended,
  importPlugin.flatConfigs.typescript,

  {
    languageOptions: {
      globals: {
        ...globals.browser,
      },
    },
    rules: {
      radix: "error",
      "require-await": "error",
      eqeqeq: "error",
      "unicorn/filename-case": [
        "error",
        {
          cases: {
            camelCase: true,
            pascalCase: true,
            kebabCase: true,
          },
        },
      ],
    },
  },

  {
    files: ["*config.mjs"],
    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
    rules: {
      // import/no-unresolved fails for eslint config file. I don't know why... therefore disable it :shrug:
      "import/no-unresolved": "off",
    },
  },

  {
    files: ["*config.js"],
    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
    rules: {
      "@typescript-eslint/no-require-imports": "off",
      "unicorn/prefer-module": "off",
    },
  },

  {
    files: ["**/*.svelte"],
    plugins: {
      svelte: sveltePlugin,
    },
    extends: [sveltePlugin.configs["flat/prettier"]],
    languageOptions: {
      parser: svelteParser,
      parserOptions: {
        parser: ts.parser,
        extraFileExtensions: [".svelte"],
      },
    },
  },
);
