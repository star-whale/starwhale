const path = require("path");

module.exports = {
    title: "Starwhale",
    tagline: "A MLOps Platform for Model Evaluation",
    url: "https://starwhale.ai",
    baseUrl: "/",
    onBrokenLinks: "warn",
    onBrokenMarkdownLinks: "warn",
    favicon: "img/favicon.ico",
    organizationName: "star-whale.github.io",
    projectName: "starwhale",

    themeConfig: {
        prism: {
            theme: require("prism-react-renderer/themes/dracula"),
        },
        colorMode: {
            disableSwitch: true,
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
            },
            items: [
                {
                    to: "/docs/quickstart/standalone",
                    label: "Documentation",
                    position: "left",
                },
                {
                    to: "/docs/faq",
                    label: "FAQ",
                    position: "left",
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
            ],
        },
        footer: {
            style: "light",
            copyright: `Copyright © ${new Date().getFullYear()} Starwhale,Inc. All rights reserved. `,

            links: [],
        },
        custom: {
            footerSocials: [
                {
                    icon: "icon-Facebook",
                    to: "#",
                },
                {
                    icon: "icon-Twitter",
                    to: "#",
                },
                {
                    icon: "icon-Linkedin",
                    to: "#",
                },
                {
                    icon: "icon-Youtube",
                    to: "#",
                },
            ],
        },
    },
    customFields: {
        email: "contact@starwhale.ai",
        description:
            "Starwhale is a MLOps platform to manage machine learning projects, models and datasets.",
    },
    presets: [
        [
            "@docusaurus/preset-classic",
            {
                docs: {
                    sidebarPath: require.resolve("./sidebars.js"),
                    editUrl: "https://github.com/star-whale/starwhale/tree/main/docs",
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
