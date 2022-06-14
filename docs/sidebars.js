module.exports = {
    mainSidebar: [
        {
            "Quickstart": ["quickstart/main"],
            "Tutorials": ["tutorials/main"],
            "Fundamentals": ["fundamentals/concepts", "fundamentals/arch"],
            "Standalone Guides": ["standalone/main"],
            "Cloud Guides": ["cloud/main", "cloud/installation"],
            "Python SDK Guides": ["sdk/main"],
            "Contributing": ["contribute/main"],
            "Reference": [
                "reference/main",
                {
                    "Command Line Interface": [
                        "reference/cli/main", "reference/cli/dataset", "reference/cli/model",
                        "reference/cli/runtime", "reference/cli/project", "reference/cli/instance",
                        "reference/cli/other"]
                }
            ],
            "Releases": ["release/main", "release/changelog"],
        },
    ],
};
