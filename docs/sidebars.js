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
            "Fundamentals": ["fundamentals/concepts"],
            "Guides": [
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
                    label: "Standalone Instance",
                    collapsed: true,
                    items: [
                        "guides/standalone/overview",
                    ]
                },
                {
                    type: 'category',
                    label: "Cloud Instance",
                    collapsed: true,
                    items: [
                        "guides/cloud/overview",
                    ]
                },
                "guides/faq",
            ],
            "Reference": {
                "Command Line Interface": [
                    "reference/cli/main", "reference/cli/dataset", "reference/cli/model",
                    "reference/cli/runtime", "reference/cli/project", "reference/cli/instance", "reference/cli/job",
                    "reference/cli/utilities"],
                "Python SDK": [
                    "reference/sdk/dataset",
                    "reference/sdk/evaluation",
                ],
            },
            "Community": [
                "community/contribute",
                "community/dev",
                "community/roadmap",
            ]
        },
    ],
};
