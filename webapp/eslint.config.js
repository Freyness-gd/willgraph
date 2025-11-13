import js from '@eslint/js';
import globals from 'globals';
import pluginVue from 'eslint-plugin-vue';
import pluginQuasar from '@quasar/app-vite/eslint';
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript';
import prettierSkipFormatting from '@vue/eslint-config-prettier/skip-formatting';

export default defineConfigWithVueTs(
	{
		// You may add any `ignores` here if needed
		// ignores: []
	},

	pluginQuasar.configs.recommended(),
	js.configs.recommended,

	pluginVue.configs['flat/essential'],

	{
		files: ['**/*.ts', '**/*.vue'],
		rules: {
			'@typescript-eslint/consistent-type-imports': ['error', { prefer: 'type-imports' }],
		},
	},

	vueTsConfigs.recommendedTypeChecked,

	{
		languageOptions: {
			ecmaVersion: 'latest',
			sourceType: 'module',
			globals: {
				...globals.browser,
				...globals.node,
				process: 'readonly',
				ga: 'readonly',
				cordova: 'readonly',
				Capacitor: 'readonly',
				chrome: 'readonly',
				browser: 'readonly',
			},
		},
		rules: {
			// Disable formatting‐rules overlapping with Prettier
			'indent': 'off',         // Prettier handles indentation
			'quotes': 'off',         // Prettier handles quotes
			'comma-dangle': 'off',   // Prettier handles trailing commas
			'max-len': 'off',        // Prettier handles line length roughly

			// Best practices
			'eqeqeq': ['error', 'always', { null: 'ignore' }],
			'no-var': 'error',
			'prefer-const': ['error', { destructuring: 'all' }],
			'curly': ['error', 'all'],
			'no-duplicate-imports': 'error',
			'no-implicit-coercion': ['error', {
				boolean: true,
				number: true,
				string: true,
				allow: []
			}],
			'object-shorthand': ['error', 'always'],
			'no-empty-function': ['error', {
				allow: ['arrowFunctions', 'functions', 'methods']
			}],
			'no-eval': 'error',
			'prefer-rest-params': 'error',
			'no-return-await': 'error',
			'no-unneeded-ternary': 'error',
			'no-useless-return': 'error',
			'no-throw-literal': 'error',
			'consistent-return': 'error',
			'no-shadow': 'error',
			'no-alert': 'warn',
			'guard-for-in': 'error',
			'no-prototype-builtins': 'error',
			'no-param-reassign': ['error', {
				props: true,
				ignorePropertyModificationsFor: ['state']
			}],
			'yoda': ['error', 'never'],
			'prefer-exponentiation-operator': 'error',
			'no-constant-condition': ['error', { checkLoops: false }],

			'@typescript-eslint/no-unused-vars': ['warn', {
				argsIgnorePattern: '^_',
				varsIgnorePattern: '^_'
			}],

			'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'warn',
			'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
		},
	},

	{
		files: ['src-pwa/custom-service-worker.ts'],
		languageOptions: {
			globals: {
				...globals.serviceworker,
			}
		},
	},

	// Integrate Prettier skipping formatting rules (so ESLint and Prettier play nicely)
	prettierSkipFormatting,
);
