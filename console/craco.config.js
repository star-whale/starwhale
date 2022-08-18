const path = require('path')
const webpack = require('webpack')
const SimpleProgressWebpackPlugin = require('simple-progress-webpack-plugin')
const CracoEsbuildPlugin = require('craco-esbuild')
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin

module.exports = {
    plugins: [{ plugin: CracoEsbuildPlugin }],
    webpack: {
        alias: {
            '@user': path.resolve(__dirname, 'src/domain/user'),
            '@model': path.resolve(__dirname, 'src/domain/model'),
            '@project': path.resolve(__dirname, 'src/domain/project'),
            '@dataset': path.resolve(__dirname, 'src/domain/dataset'),
            '@job': path.resolve(__dirname, 'src/domain/job'),
            '@base': path.resolve(__dirname, 'src/domain/base'),
            '@': path.resolve(__dirname, 'src/'),
        },
        plugins: [
            new SimpleProgressWebpackPlugin(),
            new webpack.DefinePlugin({
                'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV),
                'process.env.DEBUG': JSON.stringify(process.env.DEBUG),
                'process.env.PROXY': JSON.stringify(process.env.PROXY),
            }),
            // new BundleAnalyzerPlugin(),
        ],
        configure: (webpackConfig, { env, paths }) => {
            // https://github.com/pmndrs/react-spring/issues/1078#issuecomment-752143468
            webpackConfig.module.rules.push({
                test: /react-spring/,
                sideEffects: true,
            })
            webpackConfig.optimization.minimize = false
            webpackConfig.optimization.chunkIds = 'named'
            webpackConfig.optimization.concatenateModules = false
            return webpackConfig
        },
    },
}
