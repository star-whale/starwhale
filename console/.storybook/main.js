const { loadConfigFromFile, mergeConfig } = require('vite')
const path = require('path')
const viteTsconfig = require('vite-tsconfig-paths')
const tsconfigPaths = viteTsconfig.default

const alias = {
    'baseui': path.resolve(__dirname, '../node_modules/baseui'),
    'styletron-react': path.resolve(__dirname, '../node_modules/styletron-react'),
    'styletron-engine-atomic': path.resolve(__dirname, '../node_modules/styletron-engine-atomic'),
    'styletron-standard': path.resolve(__dirname, '../node_modules/styletron-standard'),
    '@': path.resolve(__dirname, '../src'),
    '@user': path.resolve(__dirname, '../src/domain/user'),
    '@project': path.resolve(__dirname, '../src/domain/project'),
    '@model': path.resolve(__dirname, '../src/domain/model'),
    '@job': path.resolve(__dirname, '../src/domain/job'),
    '@dataset': path.resolve(__dirname, '../src/domain/dataset'),
    '@runtime': path.resolve(__dirname, '../src/domain/runtime'),
    '@base': path.resolve(__dirname, '../src/domain/base'),
    '@starwhale/ui': path.resolve(__dirname, '../packages/starwhale-ui/src'),
    '@starwhale/core': path.resolve(__dirname, '../packages/starwhale-core/src'),
    '@starwhale/widgets': path.resolve(__dirname, '../packages/starwhale-widgets/src'),
}

module.exports = {
    stories: ['../src/**/*.stories.mdx', '../src/**/*.stories.@(js|jsx|ts|tsx)'],
    addons: ['@storybook/addon-links', '@storybook/addon-essentials', '@storybook/addon-interactions'],
    framework: '@storybook/react',
    core: {
        builder: '@storybook/builder-vite',
    },
    features: {
        storyStoreV7: true,
        previewMdx2: true,
    },
    staticDirs: ['../src/assets', '../public'],
    async viteFinal(config, { configType }) {
        const { config: userConfig } = await loadConfigFromFile(path.resolve(__dirname, '../vite.config.ts'))
        userConfig.resolve.dedupe = ['@storybook/client-api']

        return mergeConfig(config, {
            resolve: { alias },
            plugins: [tsconfigPaths()],
        })
    },
}
