const { loadConfigFromFile, mergeConfig } = require('vite')
const path = require('path')

module.exports = {
    stories: ['../src/**/*.stories.mdx', '../src/**/*.stories.@(js|jsx|ts|tsx)'],
    addons: [
        '@storybook/addon-links',
        '@storybook/addon-essentials',
        // '@storybook/addon-interactions',
    ],
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
        return mergeConfig(config, {
            resolve: userConfig.resolve,
        })
    },
}
