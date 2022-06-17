module.exports = {
    mainSidebar: [
        {
            "Quickstart": ["quickstart/standalone", "quickstart/on-premises"],
            "Tutorials": ["tutorials/main"],
            "Fundamentals": ["fundamentals/concepts", "fundamentals/arch"],
            "Standalone Guides": [
                "standalone/main",
                "standalone/client_user_guide",
                "standalone/installation",
            ],
            "Cloud Guides": ["cloud/main", "cloud/helm-charts", "cloud/ansible"],
            "Python SDK Guides": ["sdk/main"],
            "Reference": [
                "reference/main",
                {
                    "Command Line Interface": [
                        "reference/cli/main", "reference/cli/dataset", "reference/cli/model",
                        "reference/cli/runtime", "reference/cli/project", "reference/cli/instance",
                        "reference/cli/other"]
                }
            ],
            "Contributing": ["contribute/main"],
            "Releases": ["release/main", "release/changelog"],
        },
    ],
};
