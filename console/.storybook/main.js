const path = require('path');
const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin');

module.exports = {
  "stories": [
    "../src/**/*.stories.mdx",
    "../src/**/*.stories.@(js|jsx|ts|tsx)"
  ],
  "addons": [
    "@storybook/addon-links",
    "@storybook/addon-essentials",
    "@storybook/addon-interactions",
    "@storybook/preset-create-react-app"
  ],
  "framework": "@storybook/react",
  webpackFinal: async (config, {configType}) => {
    // `configType` has a value of 'DEVELOPMENT' or 'PRODUCTION'
    // You can change the configuration based on that.
    // 'PRODUCTION' is used when building the static version of storybook.

    // config.resolve.modules = [
    //   ...(config.resolve.modules || []),
    //   path.resolve(__dirname, "../"),
    // ];
    // config.resolve.alias = {
    //   ...config.resolve.alias,
    //   '@user': path.resolve(__dirname, '../src/domain/user'),
    //   '@model': path.resolve(__dirname, '../src/domain/model'),
    //   '@project': path.resolve(__dirname, '../src/domain/project'),
    //   '@dataset': path.resolve(__dirname, '../src/domain/dataset'),
    //   '@job': path.resolve(__dirname, '../src/domain/job'),
    //   '@': path.resolve(__dirname, '../src/'),
    // };
    // config.resolve.extensions.push(".ts", ".tsx");
    [].push.apply(config.resolve.plugins, [
      new TsconfigPathsPlugin({
        extensions: config.resolve.extensions,
        configFile: path.resolve(__dirname, '../tsconfig.json')
      })
    ]);
    return config;
  },
}