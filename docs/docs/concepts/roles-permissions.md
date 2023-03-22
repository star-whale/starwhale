---
title: Roles and permissions in Starwhale
---

Roles are used to assign permissions to users. Only Starwhale Server/Cloud has roles and permissions, and Starwhale Standalone does not.

The Administrator role is automatically created and assigned to the user "admin". Some sensitive operations can only be performed by users with the Administrator role, for example, creating accounts in Starwhale Server.

Projects have three roles:

* Admin - Project administrators can read and write project data and assign project roles to users.
* Maintainer - Project maintainers can read and write project data.
* Guest - Project guests can only read project data.

| Action | Admin | Maintainer | Guest |
| --- | --- | --- | ---- |
| Manage project members | Yes | | |
| Edit project | Yes | Yes | |
| View project | Yes | Yes | Yes |
| Create evaluations | Yes | Yes | |
| Remove evaluations | Yes | Yes | |
| View evaluations | Yes | Yes | Yes |
| Create datasets | Yes | Yes | |
| Update datasets | Yes | Yes | |
| Remove datasets | Yes | Yes | |
| View datasets | Yes | Yes | Yes |
| Create models | Yes | Yes | |
| Update models | Yes | Yes | |
| Remove models | Yes | Yes | |
| View models | Yes | Yes | Yes |
| Create runtimes | Yes | Yes | |
| Update runtimes | Yes | Yes | |
| Remove runtimes | Yes | Yes | |
| View runtimes | Yes | Yes | Yes |

The user who creates a project becomes the first project administrator. They can assign roles to other users later.
