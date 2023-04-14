module.exports = {
  root: true,
  parser: "@typescript-eslint/parser",
  plugins: ["@typescript-eslint"],
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:import/errors",
    "plugin:import/warnings",
    "plugin:import/typescript",
  ],
  env: {
    node: true,
    es2022: true,
  },
  rules: {
    "eol-last": ["error", "always"],
    "@typescript-eslint/no-non-null-assertion": 0,
    "@typescript-eslint/explicit-module-boundary-types": 0,
  },
  settings: {
    "import/resolver": {
      typescript: {
        alwaysTryTypes: true,
      },
    },
  },
};
