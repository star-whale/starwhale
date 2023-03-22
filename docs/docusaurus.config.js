const path = require("path");

module.exports = {
    title: "Starwhale",
    tagline: "An MLOps Platform",
    url: "https://doc.starwhale.ai",
    baseUrl: "/",
    onBrokenLinks: "warn",
    onBrokenMarkdownLinks: "warn",
    favicon: "img/favicon.ico",
    organizationName: "doc.starwhale.ai",
    projectName: "starwhale",

    i18n: {
        defaultLocale: 'en',
        locales: ['en', 'zh'],
        localeConfigs: {
            en: {
                label: 'English',
                direction: 'ltr',
                htmlLang: 'en-US',
                calendar: 'gregory',
            },
        },
    },

    themeConfig: {
        prism: {
            theme: require("prism-react-renderer/themes/dracula"),
            darkTheme: require("prism-react-renderer/themes/duotoneDark"),
        },
        colorMode: {
            defaultMode: 'light',
            disableSwitch: false,
            respectPrefersColorScheme: false,
        },
        zoomSelector: ".markdown :not(em) > img",
        announcementBar: {
            id: "supportus",
            content:
                'If you like <b>Starwhale</b>, <a target="_blank" rel="noopener noreferrer" href="https://github.com/star-whale/starwhale">give us a star on GitHub!</a> 🌺',
            isCloseable: true,
        },
        navbar: {
            style: "dark",
            hideOnScroll: true,
            title: " ",
            logo: {
                alt: "Starwhale",
                src: "img/starwhale-white.png",
                href: "/docs/what-is-starwhale",
            },
            items: [
                {
                    type: "docsVersionDropdown",
                    position: "right",
                    dropdownActiveClassDisabled: true,
                },
                {
                    to: "https://github.com/star-whale/starwhale",
                    position: "right",
                    className: "header-ico header-ico--github",
                    "aria-label": "GitHub repository",
                    label: "Github",
                },
                {
                    to: "https://starwhale.slack.com",
                    position: "right",
                    className: "header-ico header-ico--slack",
                    "aria-label": "Slack Channel",
                    label: "Slack",
                },
                {
                    type: 'localeDropdown',
                    position: 'right',
                },
            ],
        },
        footer: {
            style: "light",
            copyright: `Copyright © ${new Date().getFullYear()} Starwhale, Inc. All rights reserved. `,

            links: [],
        },
        custom: {
            footerSocials: [],
        },
    },
    customFields: {
        email: "developer@starwhale.ai",
        description:
            "Starwhale is an MLOps platform to manage machine learning projects, models and datasets.",
    },
    presets: [
        [
            "@docusaurus/preset-classic",
            {
                docs: {
                    sidebarPath: require.resolve("./sidebars.js"),
                    editUrl: "https://github.com/star-whale/starwhale/tree/main/docs",
                    versions: {
                        current: {
                            label: "WIP",
                            badge: true,
                            banner: "unreleased",
                            path: "/next",
                        }
                    }
                },
                theme: {
                    customCss: require.resolve("./src/css/custom.scss"),
                },
                gtag: {
                    trackingID: "none",
                    anonymizeIP: true,
                },
            },
        ],
    ],
    plugins: [
        "docusaurus-plugin-sass",
        path.resolve(__dirname, "src/zoom-plugin"),
    ],
};
