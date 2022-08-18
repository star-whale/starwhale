import { defineConfig } from 'vite'
import path from 'path'
import react from '@vitejs/plugin-react'
import eslint from 'vite-plugin-eslint'

// import mpa from '../../vite-plugin-mpa'
// import { visualizer } from 'rollup-plugin-visualizer'

// https://vitejs.dev/config/
export default defineConfig({
    server: {
        proxy: {
            '/api/v1/log/online': {
                changeOrigin: true,
                target:
                    (process.env.PROXY ?? '').replace('https', 'wss').replace('http', 'ws') || 'ws://127.0.0.1:8082',
                ws: true,
            },
            '/api': {
                target: process.env.PROXY || 'http://127.0.0.1:8082',
                changeOrigin: true,
                secure: false,
            },
        },
    },
    build: {
        outDir: 'build',
        manifest: true,
    },
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
            '@user': path.resolve(__dirname, './src/domain/user'),
            '@project': path.resolve(__dirname, './src/domain/project'),
            '@model': path.resolve(__dirname, './src/domain/model'),
            '@job': path.resolve(__dirname, './src/domain/job'),
            '@dataset': path.resolve(__dirname, './src/domain/dataset'),
            '@runtime': path.resolve(__dirname, './src/domain/runtime'),
            '@base': path.resolve(__dirname, './src/domain/base'),
        },
    },
    plugins: [
        eslint(),
        react({
            exclude: /\.stories\.(t|j)sx?$/,
        }),
        // mpa(),
    ],
})
