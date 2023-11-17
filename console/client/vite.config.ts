import { defineConfig } from 'vite'
import path from 'path'
import UnoCSS from 'unocss/vite'
import { partial } from 'lodash'

const resolve = partial(path.resolve, __dirname, '../')

export const alias = {
    'baseui': resolve('./node_modules/baseui'),
    'react-use': resolve('./node_modules/react-use'),
    'react': resolve('./node_modules/react'),
    'react-router-dom': resolve('./node_modules/react-router-dom'),
    'react-i18next': resolve('./node_modules/react-i18next'),
    'rc-steps': resolve('./node_modules/rc-steps'),
    'react-jss': resolve('./node_modules/react-jss'),
    'react-query': resolve('./node_modules/react-query'),
    'lodash': resolve('./node_modules/lodash'),
    'react-qr-code': resolve('./node_modules/react-qr-code'),
    'react-copy-to-clipboard': resolve('./node_modules/react-copy-to-clipboard'),
    'js-yaml': resolve('./node_modules/js-yaml'),
    'qs': resolve('./node_modules/qs'),
    'axios': resolve('./node_modules/axios'),
    'ahooks': resolve('./node_modules/ahooks'),
    '@monaco-editor/react': resolve('./node_modules/@monaco-editor/react'),
    '@': resolve('./src'),
    '@user': resolve('./src/domain/user'),
    '@project': resolve('./src/domain/project'),
    '@model': resolve('./src/domain/model'),
    '@job': resolve('./src/domain/job'),
    '@dataset': resolve('./src/domain/dataset'),
    '@runtime': resolve('./src/domain/runtime'),
    '@base': resolve('./src/domain/base'),
    '@starwhale/ui': resolve('./packages/starwhale-ui/src'),
    '@starwhale/core': resolve('./packages/starwhale-core/src'),
    '@starwhale/widgets': resolve('./packages/starwhale-widgets/src'),
    'node_modules': resolve('./node_modules'),
    '.*': resolve('./src'),
    '*': resolve('./node_modules'),
}

// https://vitejs.dev/config/
export default defineConfig(() => ({
    server: {
        proxy: {
            '/api/': {
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
    },
    resolve: {
        alias,
    },
    plugins: [UnoCSS()],
}))
