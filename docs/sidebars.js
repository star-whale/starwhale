module.exports = {
    mainSidebar: [
        "what-is-starwhale",
        {
            type: "category",
            label: "Getting Started",
            link: {
                type: "doc",
                id: "getting-started/index"
            },
            items: [
                "getting-started/standalone",
                "getting-started/server",
                "getting-started/cloud",
                "getting-started/runtime"
            ]
        },
        {
            type: "category",
            label: "Concepts",
            link: {
                type: "doc",
                id: "concepts/index"
            },
            items: [
                "concepts/names",
                "concepts/project",
                "concepts/roles-permissions",
                "concepts/versioning"
            ]
        },
        {
            type: "category",
            label: "User Guides",
            items: [
                {
                    "Starwhale Instances": [
                        {
                            type: 'category',
                            label: "Starwhale Standalone",
                            link: {
                                type: "doc",
                                id: "instances/standalone/index"
                            },
                            collapsed: true,
                            items: [
                                "instances/standalone/getting_started",
                                "instances/standalone/concepts",
                                "instances/standalone/install",
                                {
                                    type: 'category',
                                    label: "Guides",
                                    collapsed: true,
                                    items: [
                                        "instances/standalone/guides/uri",
                                        "instances/standalone/guides/config",
                                        "instances/standalone/guides/swignore",
                                    ]
                                },
                                {
                                    type: 'category',
                                    label: "API",
                                    collapsed: true,
                                    items: [
                                        "instances/standalone/api/instance_cli",
                                        "instances/standalone/api/project_cli",
                                        "instances/standalone/api/utilities_cli",
                                    ]
                                },
                            ]
                        },
                        {
                            type: 'category',
                            label: "Starwhale Server",
                            link: {
                                type: "doc",
                                id: "instances/server/index"
                            },
                            collapsed: true,
                            items: [
                                {
                                    type: 'category',
                                    label: "Installation",
                                    link: {
                                        type: "doc",
                                        id: "instances/server/installation/index"
                                    },
                                    collapsed: true,
                                    items: [
                                        "instances/server/installation/docker",
                                        "instances/server/installation/helm-charts",
                                    ]
                                },
                                {
                                    type: 'category',
                                    label: "Guides",
                                    collapsed: true,
                                    items: [
                                        "instances/server/guides/server_admin",
                                    ]
                                },
                            ]
                        },
                        {
                            type: 'category',
                            label: "Starwhale Cloud",
                            collapsed: true,
                            items: [
                                "instances/cloud/getting_started",
                                "instances/cloud/concepts",
                            ]
                        },
                    ]
                },
                {
                    type: "category",
                    label: "Starwhale Client",
                    link: {
                        type: "doc",
                        id: "swcli/index"
                    },
                    items: [
                        "swcli/installation",
                        "swcli/uri",
                        "swcli/swignore"
                    ]
                },
                "userguide/model",
                "userguide/runtime",
            ]
        },
        {
            "Reference": [
                {
                    type: "category",
                    label: "Starwhale Client",
                    link: {
                        type: "doc",
                        id: "reference/swcli/index"
                    },
                    items: [
                        "reference/swcli/instance",
                        "reference/swcli/project",
                        "reference/swcli/model",
                        "reference/swcli/dataset",
                        "reference/swcli/runtime",
                        "reference/swcli/job",
                        "reference/swcli/utilities",
                    ]
                },
                {
                    type: "category",
                    label: "Python SDK",
                    link: {
                        type: "doc",
                        id: "reference/sdk/overview"
                    },
                    items: [
                        "reference/sdk/overview",
                        "dataset/api/sdk",
                        "dataset/api/data_type",
                        "evaluation/api/sdk",
                        "reference/sdk/other",
                    ]
                }
            ],
            "Community": [
                "community/contribute",
            ]
        },
    ],
};
