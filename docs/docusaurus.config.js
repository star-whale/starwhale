const path = require('path');


module.exports = {
    title: 'Starwhale',
    tagline: 'A MLOps Tool for your model evaluation',
    url: 'https://starwhale.ai',
    baseUrl: '/starwhale/',
    onBrokenLinks: 'warn',
    onBrokenMarkdownLinks: 'warn',
    favicon: 'img/favicon.ico',
    organizationName: 'star-whale.github.io',
    projectName: 'starwhale',
    i18n: {
        defaultLocale: 'en',
        locales: ['en', 'zh'],
    },
    themeConfig: {
        prism: {
            theme: require('prism-react-renderer/themes/dracula'),
            darkTheme: require('prism-react-renderer/themes/dracula'),
        },
        zoomSelector: '.markdown :not(em) > img',
        announcementBar: {
            id: 'supportus',
            content: 'If you like <b>Starwhale</b>, <a target="_blank" rel="noopener noreferrer" href="https://github.com/star-whale/starwhale">give us a star on GitHub!</a> 🌺',
            isCloseable: true,
        },
        navbar: {
            style: 'dark',
            hideOnScroll: true,
            title: ' ',
            logo: {
                alt: 'Starwhale',
                src: 'img/logo_72x72px.png',
            },
            items: [
                {
                    to: '/docs/getting_started',
                    label: 'Docs',
                    position: 'left',
                },
                {
                    to: '/docs/examples/mnist',
                    label: 'Examples',
                    position: 'left',
                },
                {
                    label: 'References',
                    position: 'left',
                    items: [
                        {
                            label: 'Python SDK',
                            to: '/docs/references/sdk',
                        },
                        {
                            label: 'Server API',
                            to: '/docs/references/api',
                        },
                        {
                            label: 'CLI',
                            to: '/docs/client_user_guide',
                        },
                    ],
                },
                {
                    to: '/docs/faq',
                    label: 'FAQ',
                    position: 'left',
                },
                {
                    href: 'https://starwhale.slack.com',
                    position: 'right',
                    className: 'header-ico header-ico--slack',
                    'aria-label': 'Slack Channel',
                },
                {
                    href: 'https://github.com/star-whale/starwhale',
                    position: 'right',
                    className: 'header-ico header-ico--github',
                    'aria-label': 'GitHub repository',
                },
                {
                    type: 'localeDropdown',
                    position: 'right',
                },
            ],
        },
        footer: {
            style: 'light',
            copyright: `Copyright © ${new Date().getFullYear()} starwhale.ai. Built with Docusaurus.`,
        },
    },
    presets: [
        [
            '@docusaurus/preset-classic',
            {
                docs: {
                    sidebarPath: require.resolve('./sidebars.js'),
                    editUrl:
                        'https://github.com/star-whale/starwhale/edit/main/',
                },
                theme: {
                    customCss: require.resolve('./src/css/custom.css'),
                },
                gtag: {
                    trackingID: 'none',
                    anonymizeIP: true,
                },
            },
        ],
    ],
    plugins: [
        [
            require.resolve("@easyops-cn/docusaurus-search-local"),
            {
                hashed: true,
                indexDocs: true,
                docsRouteBasePath: '/docs',
                searchResultLimits: 8,
                searchResultContextMaxLength: 50,
                indexBlog: false,
                indexPages: false,
                language: "en",
            }],
        path.resolve(__dirname, 'src/zoom-plugin')
    ],
};