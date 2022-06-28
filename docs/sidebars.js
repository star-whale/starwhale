module.exports = {
    mainSidebar: [
        {
            "Quickstart": ["quickstart/standalone", "quickstart/on-premises"],
            "Tutorials": ["tutorials/main"],
            "Fundamentals": ["fundamentals/concepts", "fundamentals/arch"],
            "Standalone Guides": [
                "standalone/overview",
                "standalone/runtime", "standalone/model", "standalone/dataset",
                "standalone/evaluation",
                "standalone/installation", "standalone/configuration",
            ],
            "Cloud Guides": ["cloud/main", "cloud/helm-charts", "cloud/ansible"],
            "Python SDK Guides": ["sdk/main"],
            "Reference": [
                {
                    "Command Line Interface": [
                        "reference/cli/main", "reference/cli/dataset", "reference/cli/model",
                        "reference/cli/runtime", "reference/cli/project", "reference/cli/instance", "reference/cli/job",
                        "reference/cli/utilities"]
                }
            ],
            "Contributing": ["contribute/main"],
            "Releases": ["release/main", "release/changelog"],
        },
    ],
};
