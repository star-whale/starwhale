---
title: Resource versioning in Starwhale
---

Starwhale manages the history of all models, datasets, and runtimes. Every update to a specific resource appends a new version of the history.

Versions are identified by a version id which is a random string generated automatically by Starwhale and are ordered by their creation time.

Versions can have tags. Starwhale uses version tags to provide a human-friendly representation of versions. By default, Starwhale attaches a default tag to each version. The default tag is the letter "v", followed by a number. For each versioned resource, the first version tag is always tagged with "v0", the second version is tagged with "v1", and so on. And there is a special tag "latest" that always points to the last version. When a version is removed, its default tag will not be reused. For example, there is a model with tags "v0, v1, v2". When "v2" is removed, tags will be "v0, v1". And the following tag will be "v3" instead of "v2" again. You can attach your own tags to any version and remove them at any time.

Starwhale uses a linear history model. There is neither branch nor cycle in history.

History can not be rollback. When a version is to be reverted, Starwhale clones the version and appends it as a new version to the end of the history. Versions in history can be manually removed and recovered.
