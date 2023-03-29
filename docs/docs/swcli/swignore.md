---
title: About the .swignore file
---

The `.swignore` file is used to ignore some files during the build process of Starwhale datasets and models.

By default, SWCLI will traverse the directory tree and include all `.py/.sh/.yaml` files. For models, SWCLI will also include those specified in the model.yaml. If some files should be excluded, for example, files under `.git`, you need to put their patterns in `.swignore`.

# PATTERN FORMAT

* Each line in a swignore file specifies a pattern, which matches files and directories.
* A blank line matches no files, so it can serve as a separator for readability.
* An asterisk `*` matches anything except a slash.
* A line starting with `#` serves as a comment.

# Example

Here is the .swignore file used in the [MNIST](../examples/mnist.md) example

```bash
venv
.git
.history
.vscode
.venv
```
