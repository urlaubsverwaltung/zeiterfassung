import { defineConfig } from "eslint/config";
import globals from "globals";
import tsParser from "@typescript-eslint/parser";
import { fixupConfigRules } from "@eslint/compat";
import parser from "svelte-eslint-parser";
import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
});

export default defineConfig([
  {
    extends: compat.extends(
      "eslint:recommended",
      "plugin:@typescript-eslint/recommended",
      "plugin:unicorn/recommended",
    ),

    languageOptions: {
      globals: {
        ...globals.browser,
      },

      parser: tsParser,
      ecmaVersion: 2020,
      sourceType: "module",
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

      "unicorn/number-literal-case": "off",
      "unicorn/no-array-reduce": "off",
    },
  },
  {
    files: ["**/*.ts", "**/*.js"],

    extends: fixupConfigRules(
      compat.extends(
        "plugin:import/errors",
        "plugin:import/warnings",
        "plugin:import/typescript",
        "plugin:prettier/recommended",
      ),
    ),

    settings: {
      "import/extensions": [".js", ".ts"],
    },

    rules: {
      "import/no-duplicates": "error",
      "prettier/prettier": "error",
    },
  },
  {
    files: ["**/*.test.ts"],

    rules: {
      "@typescript-eslint/no-explicit-any": "off",
    },
  },
  {
    files: ["**/*.config.js"],

    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
  },
  {
    files: ["**/*.svelte"],
    extends: compat.extends("plugin:svelte/recommended"),

    languageOptions: {
      parser: parser,
      ecmaVersion: 5,
      sourceType: "script",

      parserOptions: {
        parser: {
          ts: "@typescript-eslint/parser",
          typescript: "@typescript-eslint/parser",
        },
      },
    },
  },
]);
