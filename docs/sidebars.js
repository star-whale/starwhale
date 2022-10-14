module.exports = {
    mainSidebar: [
        {
            "Quickstart": ["quickstart/standalone", "quickstart/on-premises"],
            "Tutorials": [
                "tutorials/pytorch",
                "tutorials/mnist",
                "tutorials/speech",
                "tutorials/ag_news",
                "tutorials/cifar10",
                "tutorials/pfp",
                "tutorials/nmt"
            ],
            "Fundamentals": [
                "fundamentals/concepts",
                "fundamentals/arch"
            ],
            "Guides": [
                "guides/uri",
                "guides/dataset",
                "guides/runtime",
                "guides/model",
                "guides/evaluation",
                {
                    type: 'category',
                    label: "Installation",
                    collapsed: true,
                    items: [
                        "guides/install/standalone",
                        "guides/install/helm-charts",
                    ]
                },
                {
                    type: 'category',
                    label: "Configurations",
                    collapsed: true,
                    items: [
                        "guides/config/standalone_config",
                        "guides/config/swignore",
                        "guides/config/cloud_admin",
                    ]
                },
                "guides/faq",
            ],
            "Reference": {
                "Command Line Interface": [
                    "reference/cli/basic",
                    "reference/cli/instance",
                    "reference/cli/project",
                    "reference/cli/dataset",
                    "reference/cli/model",
                    "reference/cli/runtime",
                    "reference/cli/eval",
                    "reference/cli/utilities"],
                "Python SDK": [
                    "reference/sdk/overview",
                    "reference/sdk/data_type",
                    "reference/sdk/dataset",
                    "reference/sdk/evaluation",
                    "reference/sdk/other",
                ],
            },
            "Community": [
                "community/contribute",
            ]
        },
    ],
};
