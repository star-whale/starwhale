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
                "getting-started/cloud"
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
            type: 'category',
            label: "Starwhale Dataset",
            link: {
                type: "doc",
                id: "dataset/index"
            },
            collapsed: true,
            items: [
                {
                    type: 'category',
                    label: "Examples",
                    collapsed: true,
                    items: [
                        "examples/mnist",
                        "examples/speech",
                        "examples/ag_news",
                        "examples/cifar10",
                        "examples/pfp",
                        "examples/nmt",
                        "examples/ucf101"
                    ]
                },
                {
                    type: 'category',
                    label: "API",
                    collapsed: true,
                    items: [
                        "dataset/api/cli",
                        "dataset/api/data_type",
                        "dataset/api/sdk",
                    ]
                },
            ]
        },
        {
            type: 'category',
            label: "Starwhale Runtime",
            link: {
                type: "doc",
                id: "runtime/index"
            },
            collapsed: true,
            items: [
                "runtime/getting_started",
                "runtime/concepts",
                {
                    type: 'category',
                    label: "Examples",
                    collapsed: true,
                    items: [
                        "runtime/examples/pytorch",
                    ]
                },
                {
                    type: 'category',
                    label: "API",
                    collapsed: true,
                    items: [
                        "runtime/api/cli",
                    ]
                },
            ]
        },
        {
            type: 'category',
            label: "Starwhale Evaluation",
            link: {
                type: "doc",
                id: "evaluation/index"
            },
            collapsed: true,
            items: [
                "evaluation/getting_started",
                "evaluation/concepts",
                {
                    type: 'category',
                    label: "Guides",
                    collapsed: true,
                    items: [
                        {
                            type: 'category',
                            label: "Heterogeneous-devices",
                            collapsed: true,
                            items: [
                                "evaluation/heterogeneous/node-able",
                                "evaluation/heterogeneous/virtual-node",
                            ]
                        },
                    ]
                },
                {
                    type: 'category',
                    label: "Examples",
                    collapsed: true,
                    items: [
                        "examples/mnist",
                        "examples/speech",
                        "examples/ag_news",
                        "examples/cifar10",
                        "examples/pfp",
                        "examples/nmt",
                        "examples/ucf101"
                    ]
                },
                {
                    type: 'category',
                    label: "API",
                    collapsed: true,
                    items: [
                        "evaluation/api/cli",
                        "evaluation/api/sdk",
                    ]
                },
            ]
        },
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
        {
            type: "category",
            label: "User Guides",
            items: [
                "userguide/model",
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
                        "instances/standalone/api/instance_cli",
                        "instances/standalone/api/project_cli",
                        "reference/swcli/model",
                        "runtime/api/cli",
                        "dataset/api/cli",
                        "evaluation/api/cli",
                        "instances/standalone/api/utilities_cli"
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
