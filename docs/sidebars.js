module.exports = {
    mainSidebar: [
        {
            "Quickstart": ["quickstart/standalone", "quickstart/on-premises"],
            "Tutorials": [
                "tutorials/mnist",
                "tutorials/ag_news",
                "tutorials/cifar10",
                "tutorials/pfp",
                "tutorials/speech",
            ],
            "Fundamentals": ["fundamentals/concepts"],
            "Standalone Guides": [
                "standalone/overview",
                "standalone/runtime",
                "standalone/installation"
            ],
            "Cloud Guides": ["cloud/helm-charts"],
            "Reference": [
                {
                    "Command Line Interface": [
                        "reference/cli/main", "reference/cli/dataset", "reference/cli/model",
                        "reference/cli/runtime", "reference/cli/project", "reference/cli/instance", "reference/cli/job",
                        "reference/cli/utilities"]
                }
            ]
        },
    ],
};
