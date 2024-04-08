# semver-multi
[![Powered by KineticFire Labs](https://img.shields.io/badge/Powered_by-KineticFire_Labs-CDA519?link=https%3A%2F%2Flabs.kineticfire.com%2F)](https://labs.kineticfire.com/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
<p></p>

Clearly convey granular differences between artifact versions by using automatically-generated versioning as part of Continuous Integration & Continuous Delivery/Deployment--delighting your customers--that follow the [Semantic Versioning specification](https://semver.org/) and use standardized git commit messages per the [Conventional Commits specification](https://www.conventionalcommits.org/).

# Table of Contents
1. [Purpose](#purpose)
1. [Problem](#problem)
1. [Solution](#solution)
1. [Architecture](#architecture)
   1. [Primary Integration Points for Version and Tag Coordination](#primary-integration-points-for-version-and-tag-coordination)
1. [Approach](#approach)
   1. [Semantic Versioning](#semantic-versioning)
   1. [Standardized Commit Messages](#standardized-commit-messages)
   1. [Write Good Commit Messages](#write-good-commit-messages)
1. [Implementing](#implementing)
   1. [Enforcing Standardized Commit Messages](#enforcing-standardized-commit-messages)
   1. [Identify Scopes and Types](#define-scopes-and-types)
   1. [Create and Install Config](#create-and-install-config)
   1. [Install Hooks](#install-hooks)
      1. [Server-side Hooks](#server-side-hooks)
      1. [Client-side Hooks](#client-side-hooks)
         1. [commit-msg](#commit-msg) 
1. [License](#license)
1. [References](#references)

# Purpose

*semver-multi* automatically generates independent version numbers for multiple artifacts in the same project, helping to more clearly convey at a granular level the differences between versions of a given artifact.  Version numbers follow the [Semantic Versioning specification](https://semver.org/) to effectively indicate the meaning about artifact changes from one version to the next.  Standardized git commit messages, adhering to the [Conventional Commits specification](https://www.conventionalcommits.org/), solely drive the semantic version increments in a methodical and objective manner.

Standardized semantic versioning helps indicate the type and level of change between two different versions such as bug fixes, new features, and backwards-compatible vs. non-backwards compatible updates.  However, versioning at the project-level does not provide insight into the nature or degree of changes (or lack thereof) at the artifact-level.

Artifact-level semantic versioning indicates to your customers and your team the type and level of change between artifact versions.

Automatic artifact semantic versioning, powered by *semver-multi*, helps accelerate your Continuous Integration & Continuous Delivery/Deployment (CI/CD) process.

# Problem

Figure 1 demonstrates the issue with a single version at the project-level for all artifacts.

<p align="center">
   <img width="95%" alt="Multiple Artifact Versioning Problem" src="resources/multiple-artifact-version-problem.png">
</p>
<p align="center">Figure 1 -- Multiple Artifact Versioning Problem</p>

When versioning all artifacts with a single project-level version, an artifact may reflect a version increment even though the artifact has not changed.  Figure 1 shows this scenario in which a new feature added to the server results in an increment of the client's minor version.

Unnecessary and innacurate version increments incorrectly represent the artifact as a new and (likely) improved version of the previous one.  The CI/CD pipeline and DevSecOps processes kick-off and culminate to distribute, store, and deploy an identical artifact to the previous version with no benefit.  Needless version increments can produce a ripple of equally unnecessary version bumps on dependent projects.  This effect can further compound "dependency hell", where developers find themselves caught between *version lock* and *version promiscuity* [1].

Customer experience may suffer, especially if the customer must exert effort to adopt a new version--such as downloading and installing a binary--that has no value beyond the previous version.

# Solution

<p align="center">
   <img width="95%" alt="Multiple Artifact Versioning Solution with semver-multi" src="resources/multiple-artifact-version-solution-semver-multi.png">
</p>
<p align="center">Figure 2 -- Multiple Artifact Versioning Solution with <i>semver-multi</i></p>

<p align="center">
   <img width="95%" alt="Granular Artifact Versioning Solution with semver-multi" src="resources/multiple-artifact-version-solution2-semver-multi.png">
</p>
<p align="center">Figure 3 -- Granular Artifact Versioning Solution with <i>semver-multi</i></p>


# Architecture

Figure 4 shows the system architecture of *semver-multi* as integrated into a CI/CD pipeline.  The figure also illustrates the interaction of *semver-multi* with a CI server, such as Jenkins.

<p align="center">
   <img width="95%" alt="semver-multi Architecture" src="resources/semver-multi-architecture.png">
</p>
<p align="center">Figure 4 -- <i>semver-multi</i> Architecture</p>

*semver-multi* generates artifact-level version numbers in coordination with the CI server as follows:
1. Developers push to the git server commits aligning to the [Conventional Commits specification](https://www.conventionalcommits.org/) and preferably enforced by git hooks (todo link)
   1. Server-side and/or client-side git hooks may be used.  Server-side hooks are preferred since they are easier to install and enforce and more difficult to bypass.  Client-side hooks may help the developer before server-side hooks come into play.  Client-side hooks may be the only option if server-side hooks cannot be installed.
1. The CI server becomes aware of new commits to the repository such as through a push notification, poll, or manual trigger
1. The CI server retrieves the current contents of the repository by performing a `git checkout` or `git pull` of the repository
1. A local version of the git repository is now on the filesystem with the CI server and accessible to *semver-multi*
1. The CI server, in the course of building the project in the repository, requests that *semver-multi* generate version numbers for the build
1. *semver-multi* retrieves from the local copy of the git repository on the filesystem
   1. The last git tag number
   1. The annotation in the last git tag, which contains the versions for the project and its artifacts for the last build
   1. The commit message log from the last git tag to current
   1. The `project-def.json` (todo link) which describes the project, its sub-projects and artifacts, and their relationships
1. *semver-multi* computes the new version numbers for the build
1. *semver-multi* creates a new annotated git tag with the updated versions
1. *semver-multi* provides a response to the CI server that includes the updated versions for the project and its artifacts
1. The CI server pushes the new git tag
1. The CI server injects the version numbers as it builds, tests, and delivers/deploys the project artifacts

Note that the process neither changes the contents of the project nor produces additional commits.

## Primary Integration Points for Version and Tag Coordination

*semver-multi* coordinates version information and corresponding git tags as follows:
1. The git server retains all of the version information and corresponding git tags.  The git tag corresponds to the project-level version.  All other version information is stored as JSON data in the annotated git tag.
1. The `project-def.json` describes the project, its sub-projects and artifacts, and their relationships.  The file is stored in the git repository (by default, at the root level).
1. The CI server (or other entity) requests that *semver-multi* generate version information given a file path to a local git repository.  *semver-multi* creates git tags in the local repository with JSON data to record the updated version information and responds to the CI server with JSON version data.  The CI server must push the git tags and apply the version information to the build.

Key points include:
1. The git repository stores all version information for the history of the project as well as the project definition at the time specific version information was generated.  There is no additional data that need be backed-up for recovery.
1. The CI server (or other entity) is responsible for accessing the remote git repository and, likely, managing credentials for that access.  *semver-multi*, by design, does not need to manage credentials or have access to remote systems.
1. *semver-multi* is stateless.  There is no data to back-up for recovery purposes.

# Approach

Three tenets guide *semver-multi*:
1. [Semantic Versioning](#semantic-versioning)
1. [Standardized Commit Messages](#standardized-commit-messages)
1. [Write Good Commit Messages](#write-good-commit-messages)

## Semantic Versioning

*semver-multi* generates version numbers in accordance with the [Semantic Versioning specification](https://semver.org/).  The specification defines a set of rules and requirements that determines how a version number is incremented, which helps clearly indicate the nature and potential value and impact (e.g., new features or a backwards incompatible change) in a new artifact version.

A semantic version takes the form `<major version>.<minor version>.<patch version>` and increments the
- *major version* for backwards incompatible (e.g. breaking) changes
- *minor version* for added features that are backwards compatible
- *patch version* for backwards compatible bug fixes

A semantic version for a mainline release (such as from `main` in the git repository) may be `1.2.3`.  Given this version, then:
- a *bug fix* commit will result in a new version of 1.2.4
- a *new feature* commit will result in a new version of 1.3.0
- a *BREAKING CHANGE* commit will result in a new version of 2.0.0

*semver-multi* also supports development release versioning (which supports testing) whose semantic version takes the form `<major version from last main tag>.<minor version from last main tag>.<patch version from last main tag>-dev+<branch name>.<unique git object name>`.  Per the Semantic Versioning specification, the branch name will consist only of uppercase and lowercase letters, numbers, and dashes.  A development release version may be `1.2.3-dev+new-feature.gbba57`.

## Standardized Commit Messages

*semver-multi* requires git commit messages that follow the [Conventional Commits specification](https://www.conventionalcommits.org/).  The specification defines the format and content for commit messages.  Standardized commit messages allow *semver-multi* to understand commit messages and automatically generate the appropriate artifact-level version numbers.

The first line--the title line--is required and includes a *type*, *scope*, and *description*.
- *type*: The type of the commit, where *type* is an enumerated value that indicates the intent of the commit, e.g. a feature, bug fix, etc.  Required.
- *scope*: The scope of the commit, where *scope* is an enumerated value that indicates what is affected by the commit.  Required by *semver-multi*, although Conventional Commits says optional.
- *description*: Succintly describes the commit.  Required.

The optional body provides additional detail about the commit.
- If no body is provided, then the title line represents the entirety of the commit
- If a body is present, then an empty line must separate the title line from the body

A breaking change is indicated by either in the titlie line by an exclamation point after the closing parenthesis of the scope and before the colon e.g. `(<scope>)!: <description>`, by putting `BREAKING CHANGE: <description>` into the body, or both.

The general format of a commit message, following the rules described above, is:

```
<type>(<scope>): <description>

[optional body]
```

Example 1 - title line only (no body) without breaking change:
```
docs(project): correct misspellings and typos in README
```


Example 2 - title line only (no body) with breaking change:
```
feat(api)!: must include API token in all API queries
```


Example 3 - body without breaking change:
```
feat(app): allow users to register multiple contact email addresses

User may register more than email address.  Once verified, an email
address may be indicated as 'primary' for the user to login and to
receive email communications.
```


Example 4 - body with breaking change:
```
feat(app)!: user login requires username and not email address

User login identifies the user by configurable username and no
longer accepts an email address to identify the user

BREAKING CHANGE: user login requires username, and does not accept
email address
```

### Scopes and Types

The *scopes* and *types* in Conventional Commits act like objects and verbs to describe the project:  the *scope* indicates **what** changed, and the *type* indicates **how** it changed.  The scopes and types defined depend on the needs of the specific project.

A *project* scope--perhaps shortened *proj*--can be used to apply to the entire project.  This scope is equivalent to no indicated scope, which is permissiable under Conventional Commits.  *semver-multi* requires a scope, even for project-level scope, so that commits to that level are explicitly considered.

Scope examples appear in Table 1.

<p align="center">Table 1 -- Generic Scope Examples</p>

| Generic Scope | Description with Specific Scope Examples |
| --- | --- |
| project | Applies to entire project (proj) or to sub-projects (client-proj, server-proj) |
| code | Application (app), library (lib), API (api), container image (img), Ansible playbooks (infra), etc. |
| document | README (readme), user guide, developer guide, etc. |

Table 2 provides type examples.  Note that not every type will apply for every scope.

<p align="center">Table 2 -- Type Examples</p>

| Type | Description | Generic Scope | Triggers Build | Minor or Patch<sup>1</sup> |
| --- | --- | --- | --- | --- |
| revert | Revert to a previous commit version.  Applies only to top-level project. | project | yes | minor |
| feat | Add a new feature | code | yes | minor |
| more | Add code for a future feature (later inidicated as complete with 'feat').  Support branch abstraction. | code | yes | minor |
| change | Change implementation of existing feature | code | yes | patch |
| remove | Remove a feature | code | yes | minor |
| less | Remove code for a feature (already indicated as removed with 'remove').  Support branch abstraction. | code | yes | minor |
| deprecate | Indicate some code is deprecated | code | yes | patch |
| fix | Fix a defect (e.g., bug) | code | yes | patch |
| refactor | Rewrite and/or restructure code without changing behavior | code | no | patch |
| perf | Improve performance, as a special case of refactor | code | yes | minor |
| security | Improve security aspect | code | yes | minor |
| style | Does not affect the meaning or behavior | code | no | patch |
| test | Add or correct tests | code | no | patch |
| struct | Project structure, e.g. directory layout | project | yes | patch |
| docs | Affect documentation.  Scope may affect meaning.  When applied to 'code', affects API documentation (such as documentation for public and protected methods and classes with default javadocs) | project, code, document (e.g., README), etc. | no | patch |
| idocs | Affect internal documentation that wouldn't appear in API documentation (such as comments and documentation for private methods with default javadocs)  | code | no | patch |
| build | Affect build components like the build tool | project, code | no | patch |
| vendor | Update version for dependencies and packages | project, code, etc. | yes | patch |
| ci | Affect CI pipeline | project, code | no | patch |
| ops | Affect operational components like infrastructure, deployment, backup, recovery, etc. | project, code | yes | patch |
| chore | Miscellaneous commits, such as updating .gitignore | project, code | no | patch |

*1 - Unless indicated as a breaking change, then is 'major'*

Table 3 defines type modifiers.

<p align="center">Table 3 -- Type Modifiers</p>

| Modifier | Description |
| --- | --- |
| ~ | The tilde character may be prefixed to a type to indicate a work-in-progress |


## Write Good Commit Messages

Writing good commit messages not only helps developers understand the changes made (especially when tracking down regressions), but also supports the automatic generation of changelogs and release notes.  A good commit message:
- is **atomic**.  Good Commits align to the Single Responsibility Principle where, in this case, a unit of work covered by the commit should concern itself with one task.  This approach helps simplify the process of tracing regressions and corrective actions like reverting.  While atomic commits may introduce some drag with requiring work to be planned and split into smaller chunks, it can improve the overall quality and simplify debugging and corrections related to the repository.
- uses **imperative mood** in the subject line, as if in the tone of giving a command or order, e.g. "Add fix for user active state."
- addresses the **why** and **how** a change was made.
- has a description in the title line (first line) as if **answering "This change will <description>."**
- has a body that covers the **motivation for the change and contrasts with previous behavior**.
- uses lowercase and no punctuation in the subject.
- limits the first line to 50 characters and body lines to 72 characters each


# Implementing

1. [Enforce Stnadardized Commit Messages](#enforce-standardized-commit-message)
1. [Define Scopes and Types](#define-scopes-and-types)
1. [Create and Install Config](#create-and-install-config)
1. [Install Hooks](#install-hooks)



## Enforce Standardized Commit Messages

*semver-multi* provides git hooks (todo link) to help enforce standardized git commit messages.  The integrity of the git commit messages is key to understanding the changes in the project and generating the appropriate version information.

Server-side and/or client-side git hooks may be used.  Server-side hooks are preferred since they are easier to install and enforce and more difficult to bypass.  Client-side hooks may help the developer before server-side hooks come into play.  Client-side hooks may be the only option if server-side hooks cannot be installed.

## Define Scopes and Types

Identify the scopes and types to be used for the project.

Beginning with scopes, consider:
- What artifacts are produced?
- What needs to be individually versioned?



## Create and Install Config

Using the scopes and types identified above, create a configuration file named ```commit-msg.cfg.json``` and commit it to the top-level of your repository.  The configuration file should follow the format shown below, although *scopes* and *types* will vary.

Table 3 -- Descripton of Select 'commit-msg.cfg.json' Properties
| Property | Description |
| --- | --- |
| enabled | *true* to enable the hook enforcing commit message standard and *false* to disable; server-side hooks may be configured to always enforce the commit message standard regardless of this setting |
| length.titleLine | Sets the minimum (*.min*) and maximum (*.max*) number of characters for the title line (first line) of the commit message |
| length.bodyLine | Sets the minimum (*.min*) and maximum (*.max*) number of characters for a line in the body of the commit message |

```
{
   "enabled": true,
   "length": {
      "titleLine": {
         "min": 20,
         "max": 50
      },
      "bodyLine": {
         "min": 2,
         "max": 72
      }
   },
   "scopes": [
      {
         "name": "proj",
         "types": [
            "revert",
            "security",
            "build",
            "vendor",
            "ci",
            "docs",
            "ops",
            "chore"
         ]
      },
      {
         "name": "app",
         "types": [
            "feat",
            "more",
            "change",
            "fix",
            "deprecate",
            "remove",
            "less",
            "refactor",
            "perf",
            "security",
            "style",
            "test",
            "docs",
            "build",
            "vendor",
            "ci",
            "ops",
            "chore"
         ]
      },
      {
         "name": "readme",
         "types": [
            "docs"
         ]
      }
   ]
}
```

## Install Hooks

Ideally, both server-side and client-side hooks would be used.  Server-side hooks ensure enforcement of the commit message standard and are difficult to bypass, however these hooks require admin or root control of the server hosting the git repository, which may not always be possible.  Client-side hooks do not require admin/root control of the git server, but it's easy for users to bypass these hooks.

### Server-side Hooks

Coming soon

### Client-side Hooks

#### commit-msg

- copy the commit-msg hook script from ```client-side-hooks/src/main/bash/commit-msg``` to the client's local ```<git repository>/.git/hooks```
- make the script executable with ```chmod +x commit-msg```


# License
The git-conventional-commits-hooks project is released under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)


# References
1. [Semantic Versioning 2.0.0](https://semver.org/), downloaded 7 Apr. 2024.
