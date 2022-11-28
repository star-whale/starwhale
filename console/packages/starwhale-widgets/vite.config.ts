// @ts-nocheck

import { defineConfig } from 'vite'
import path from 'path'
import react from '@vitejs/plugin-react'
import eslint from 'vite-plugin-eslint'
import dts from 'vite-plugin-dts'
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
    plugins: [
        // eslint(),
        react({
            exclude: /\.stories\.(t|j)sx?$/,
        }),
    ],
    esbuild: {
        logOverride: { 'this-is-undefined-in-esm': 'silent' },
    },
})
