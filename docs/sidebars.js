module.exports = {
    mainSidebar: [
        {
            "Overview": [
                "overview/getting_started",
                "overview/concepts",
            ],
            "Starwhale Dataset": [
                "dataset/getting_started",
                "dataset/concepts",
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
            ],
            "Starwhale Model": [
                "model/getting_started",
                "model/concepts",
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
                        "model/api/cli",
                        "model/api/sdk",
                    ]
                },
            ],
            "Starwhale Runtime": [
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
            ],
            "Starwhale Evaluation": [
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
            ],
            "Starwhale Instances": [
                {
                    type: 'category',
                    label: "Starwhale Standalone",
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
                    collapsed: true,
                    items: [
                        "instances/server/getting_started",
                        "instances/server/concepts",
                        {
                            type: 'category',
                            label: "Installation",
                            collapsed: true,
                            items: [
                                "instances/server/install/docker",
                                "instances/server/install/helm-charts",
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
            ],
            "Reference": {
                "Command Line Interface": [
                    "reference/cli/basic",
                    "instances/standalone/api/instance_cli",
                    "instances/standalone/api/project_cli",
                    "model/api/cli",
                    "runtime/api/cli",
                    "dataset/api/cli",
                    "evaluation/api/cli",
                    "instances/standalone/api/utilities_cli"],
                "Python SDK": [
                    "reference/sdk/overview",
                    "dataset/api/sdk",
                    "dataset/api/data_type",
                    "model/api/sdk",
                    "evaluation/api/sdk",
                    "reference/sdk/other",
                ],
            },
            "Community": [
                "community/contribute",
            ]
        },
    ],
};
