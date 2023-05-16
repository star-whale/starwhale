import { defineConfig } from 'vite'
import path from 'path'
import react from '@vitejs/plugin-react'
import inspect from 'vite-plugin-inspect'
import router from './vite-plugin-react-routes'

// import eslint from 'vite-plugin-eslint'
// import mpa from '../../vite-plugin-mpa'
// import { visualizer } from 'rollup-plugin-visualizer'

export const alias = {
    // @FIXME
    'baseui': path.resolve(__dirname, './node_modules/baseui'),
    'react-use': path.resolve(__dirname, './node_modules/react-use'),
    'react': path.resolve(__dirname, './node_modules/react'),
    'react-router-dom': path.resolve(__dirname, './node_modules/react-router-dom'),
    'react-i18next': path.resolve(__dirname, './node_modules/react-i18next'),
    'rc-steps': path.resolve(__dirname, './node_modules/rc-steps'),
    'react-jss': path.resolve(__dirname, './node_modules/react-jss'),
    'react-query': path.resolve(__dirname, './node_modules/react-query'),
    'lodash': path.resolve(__dirname, './node_modules/lodash'),
    'react-qr-code': path.resolve(__dirname, './node_modules/react-qr-code'),
    'js-yaml': path.resolve(__dirname, './node_modules/js-yaml'),
    'qs': path.resolve(__dirname, './node_modules/qs'),
    'axios': path.resolve(__dirname, './node_modules/axios'),
    '@monaco-editor/react': path.resolve(__dirname, './node_modules/@monaco-editor/react'),
    '@': path.resolve(__dirname, './src'),
    '@user': path.resolve(__dirname, './src/domain/user'),
    '@project': path.resolve(__dirname, './src/domain/project'),
    '@model': path.resolve(__dirname, './src/domain/model'),
    '@job': path.resolve(__dirname, './src/domain/job'),
    '@dataset': path.resolve(__dirname, './src/domain/dataset'),
    '@runtime': path.resolve(__dirname, './src/domain/runtime'),
    '@base': path.resolve(__dirname, './src/domain/base'),
    '@starwhale/ui': path.resolve(__dirname, './packages/starwhale-ui/src'),
    '@starwhale/core': path.resolve(__dirname, './packages/starwhale-core/src'),
    '@starwhale/widgets': path.resolve(__dirname, './packages/starwhale-widgets/src'),
}

let extendProxies = {}
if (process.env.VITE_EXTENDS === 'true')
    extendProxies = {
        '/api/v1/system/resourcePool': {
            target: 'http://10.131.0.1:8088/billing/',
            changeOrigin: true,
            secure: false,
        },
        '/billing': {
            target: 'http://10.131.0.1:8088/',
            changeOrigin: true,
            secure: false,
        },
    }

// https://vitejs.dev/config/
export default defineConfig({
    server: {
        proxy: {
            ...extendProxies,
            '/api/v1/log/online': {
                changeOrigin: true,
                target:
                    (process.env.PROXY ?? '').replace('https', 'wss').replace('http', 'ws') || 'ws://127.0.0.1:8082',
                ws: true,
            },
            '^(/api|/swcloud|/gateway)': {
                target: process.env.PROXY || 'http://127.0.0.1:8082',
                changeOrigin: true,
                secure: false,
            },
            '/plugins': {
                target: 'http://127.0.0.1:8080/',
                changeOrigin: true,
                secure: false,
            },
        },
    },
    build: {
        outDir: 'build',
        manifest: true,
        // minify: false,
        // sourcemap: true,
    },
    resolve: { alias },
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
