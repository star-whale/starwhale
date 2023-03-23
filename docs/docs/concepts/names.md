---
title: Names in Starwhale
---

Names mean project names, model names, dataset names, runtime names, and tag names.

# 1. Names Limitation

- Names are case-insensitive.
- A name MUST only consist of letters `A-Z a-z`, digits `0-9`, the hyphen character `-`, the dot character `.`, and the underscore character `_`.
- A name should always start with a letter or the `_` character.
- The maximum length of a name is 80.

# 2. Names uniqueness requirement

The resource name should be a unique string within its owner. For example, the project name should be unique in the owner instance, and the model name should be unique in the owner project.

The resource name can not be used by any other resource of the same kind in their owner, including those removed ones. For example, Project "apple" can not have two models named "Alice", even if one of them is already removed.

Different kinds of resources can have the same name. For example, a project and a model can be called "Alice" simultaneously.

Resources with different owners can have the same name. For example, a model in project "Apple" and a model in project "Banana" can have the same name "Alice".

Garbage-collected resources' names can be reused. For example, after the model with the name "Alice" in project "Apple" is removed and garbage collected, the project can have a new model with the same name "Alice".
