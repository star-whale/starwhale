// @ts-nocheck

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import eslint from 'vite-plugin-eslint'
import { alias } from '../../vite.config'

export default defineConfig({
    define: {
        'process.env.NODE_ENV': `"${process.env.NODE_ENV}"`,
    },
    build: {
        outDir: 'build',
        manifest: true,
        minify: false,
        lib: {
            entry: './src/index.ts',
            formats: ['es'],
        },
    },
    resolve: {
        alias,
    },
    plugins: [eslint(), react()],
    esbuild: {
        logOverride: { 'this-is-undefined-in-esm': 'silent' },
    },
})
