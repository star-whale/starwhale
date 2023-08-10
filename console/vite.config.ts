import { defineConfig } from 'vite'
import path from 'path'
import react from '@vitejs/plugin-react'
import UnoCSS from 'unocss/vite'

// import inspect from 'vite-plugin-inspect'
// import router from './vite-plugin-react-routes'
// import eslint from 'vite-plugin-eslint'
// import mpa from '../../vite-plugin-mpa'
// import { visualizer } from 'rollup-plugin-visualizer'

const { execSync } = require('child_process')
const commitNumber = execSync('git rev-parse HEAD').toString().trim()

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
    'react-copy-to-clipboard': path.resolve(__dirname, './node_modules/react-copy-to-clipboard'),
    'js-yaml': path.resolve(__dirname, './node_modules/js-yaml'),
    'qs': path.resolve(__dirname, './node_modules/qs'),
    'axios': path.resolve(__dirname, './node_modules/axios'),
    'ahooks': path.resolve(__dirname, './node_modules/ahooks'),
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
    '.*': path.resolve(__dirname, './src'),
    '*': path.resolve(__dirname, './node_modules'),
}
const projectRootDir = path.resolve(__dirname)

let extendProxies = {}
// if (process.env.VITE_EXTENDS === 'true')
//     extendProxies = {
//         '/api/v1/system/resourcePool': {
//             target: 'http://10.131.0.1:8088/billing/',
//             changeOrigin: true,
//             secure: false,
//         },
//         '/billing': {
//             target: 'http://10.131.0.1:8088/',
//             changeOrigin: true,
//             secure: false,
//         },
//     }

const htmlPlugin = (mode) => {
    return {
        name: 'html-transform',
        transformIndexHtml(html) {
            return html.replace(
                /<!--__INJECT__-->/,
                mode === 'extend'
                    ? `<script>
          var _hmt = _hmt || []
          ;(function () {
              var hm = document.createElement('script')
              hm.src = 'https://hm.baidu.com/hm.js?82145850946f2ffce3c1366524ebe861'
              var s = document.getElementsByTagName('script')[0]
              s.parentNode.insertBefore(hm, s)
          })()
      </script>`
                    : ''
            )
        },
    }
}

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
    define: {
        'process.env': {
            GIT_COMMIT_HASH: JSON.stringify(commitNumber),
        },
    },
    server: {
        proxy: {
            ...extendProxies,
            '/api/v1/log/online': {
                changeOrigin: true,
                target:
                    (process.env.PROXY ?? '').replace('https', 'wss').replace('http', 'ws') || 'ws://127.0.0.1:8082',
                ws: true,
            },
            '^(/api|/swcloud|/gateway|/billing)': {
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
        target: 'esnext',
        // minify: false,
        // sourcemap: true,
    },
    resolve: {
        alias,
    },
    plugins: [
        // eslint(),
        react({
            exclude: /\.stories\.(t|j)sx?$/,
        }),
        htmlPlugin(mode),
        UnoCSS(),
    ],
    esbuild: {
        logOverride: { 'this-is-undefined-in-esm': 'silent' },
    },
}))
