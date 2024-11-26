# semver-multi
[![Powered by KineticFire Labs](https://img.shields.io/badge/Powered_by-KineticFire_Labs-CDA519?link=https%3A%2F%2Flabs.kineticfire.com%2F)](https://labs.kineticfire.com/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
<p></p>

*Automatically compute artifact-level [standardized Semantic Versions](https://semver.org/) for each artifact in your project.*

Convey to your customers and team the granular differences between artifact versions with automatically-computed, 
artifact-level [standardized Semantic Versions](https://semver.org/) as part of your Continuous Integration & Continuous 
Delivery/Deployment (CI/CD) process, placing no additional burden on team other than writing [Conventional Commits](https://www.conventionalcommits.org/) 
compliant Git messages.

# Table of Contents
1. [Purpose](#purpose)
1. [Problem](#problem)
1. [Solution](#solution)
1. [Capabilities](#capabilities)
1. [Approach](#approach)
   1. [Produce Semantic Version Numbers Compliant with the Semantic Versioning Specification](#produce-semantic-version-numbers-compliant-with-the-semantic-versioning-specification)
   1. [Integrate with Common Tools to Compute and Apply Semantic Versions](#integrate-with-common-tools-to-compute-and-apply-semantic-versions)
   1. [Use Standardized Git Commit Messages per the Conventional Commits Specification](#use-standardized-git-commit-messages-per-the-conventional-commits-specification)
      1. [Write Effective Git Commit Messages](#write-effective-git-commit-messages)
   1. [Require the Complete Git Commit Message History](#require-the-complete-git-commit-message-history)
   1. [Use a Project Definition File to Describe Project and Artifact Relationships](#use-a-project-definition-file-to-describe-project-and-artifact-relationships)
      1. [Scopes and Types](#scopes-and-types)
      1. [Project Definition File Format](#project-definition-file-format)
   1. [Store Versioning Data in the Git Repository](#store-versioning-data-in-the-git-repository)
   1. [Record All Inputs in the Git Repository](#record-all-inputs-in-the-git-repository)
   1. [Produce Easily-Consumable Version Output](#produce-easily-consumable-version-output)
   1. [Architecture and Operation](#architecture-and-operation)
   1. [Implement in Babashka](#implement-in-babashka)
1. [Deploying](#deploying)
1. [Managing](#managing)
1. [Git Hooks](#git-hooks)
1. [Utilities](#utilities)
1. [Building from Source](#building-from-source)
   1. [Initial Setup](#initial-setup)
   1. [Environment Setup](#environment-setup)
      1. [Development Containers](#development-containers)
      1. [Traditional System Setup](#traditional-system-setup)
   1. [Building the Artifacts](#building-the-artifacts)
1. [Contributing](#contributing)
1. [Tools](#tools)
   1. [Babashka](#babashka)
   1. [Java](#java)
1. [License](#license)
1. [References](#references)



# Purpose

*semver-multi* computes a version number for each configured artifact in a project, helping to more clearly express at 
a granular level the differences between versions of a given artifact.  Version numbers follow the 
[Semantic Versioning specification](https://semver.org/) to effectively indicate the meaning about artifact changes from 
one version to the next.  Standardized Git commit messages, adhering to the 
[Conventional Commits specification](https://www.conventionalcommits.org/), solely drive the semantic version increments 
in a methodical, accurate, and objective manner.

Semantic versioning helps indicate the type and level of change between two different versions such as a new feature vs. 
a bug fix and backwards-compatible vs. non-backwards compatible updates.  However, typical versioning at the 
project-level does not provide insight into the nature or degree of changes (or lack thereof) at the artifact-level.

Artifact-level semantic versioning indicates to your customers and team the type and level of change between artifact 
versions.

Automatic artifact semantic versioning--powered by *semver-multi*--helps automate the accurate versioning of project 
artifacts, thereby accelerating your Continuous Integration & Continuous Delivery/Deployment (CI/CD) process.

*semver-multi* provides a light-weight semantic versioning capability that easily integrates into a CI/CD pipeline with 
a CI server, such as [Jenkins](https://www.jenkins.io/):
1. The CI server executes *semver-multi* with access to the local, updated Git repository.
1. There is no additional data to backed-up for recovery, beyond the Git repository.
   1. The Git repository stores all version information (in annotated tags) for the history of the project as well as 
      the project definition (e.g., the `semver-multi.json`) at the time specific version information was generated.
   1. *semver-multi* is stateless.  The system does not contain data to back-up for recovery purposes.
1. No additional Git commit is made to record versioning information (annotated tags are used).
1. *semver-multi* does not need to manage credentials or have access to remote systems.  The CI server (or other entity) 
   is responsible for accessing the remote Git repository and, likely, managing credentials for that access.

# Problem

Figure 1 demonstrates the issue with a single version at the project-level for all artifacts.

<p align="center">
   <img width="95%" alt="Multiple Artifact Versioning Problem" src="resources/multiple-artifact-version-problem.png">
</p>
<p align="center">Figure 1 -- Multiple Artifact Versioning Problem</p>

When versioning all artifacts with a single project-level version, an artifact may reflect a version increment even 
though the artifact has not changed.  Figure 1 shows this scenario in which a new feature added to the `server` results 
in an increment of the `client`'s minor version.

Unnecessary and innacurate version increments incorrectly represent the artifact as a new and (presumably) improved 
version of the previous one.  The CI/CD pipeline and DevSecOps processes kick-off and culminate to distribute, store, 
and deploy an identical artifact to the previous version with no benefit.  Needless version increments can produce a 
ripple of equally unnecessary version bumps on dependent projects.  This effect can further compound "dependency hell", 
where developers find themselves caught between *version lock* and *version promiscuity* [1].

Customer experience may suffer, especially if the customer must exert effort to adopt a new version--applying their 
DevSecOps and distribution processes--that has no value beyond the previous version.

# Solution

*semver-multi* solves the [problem](#problem) of incorrectly conveying the generation of a new artifact (due to 
incrementing the version of an artifact when the artifact didn't change or didn't change to the degree indicated by the 
version increment) by computing a version for each configured artifact as shown in Figure 2.

<p align="center">
   <img width="95%" alt="Multiple Artifact Versioning Solution with semver-multi" src="resources/multiple-artifact-version-solution-semver-multi.png">
</p>
<p align="center">Figure 2 -- Multiple Artifact Versioning Solution with <i>semver-multi</i></p>

In this case, the `client` artifact did not change, so no version increment should be applied and the `client` remains 
at its original version.  For the `client`:  no DevSecOps processes kick-off, no ripple affects occur for dependent 
projects, likelihood of "dependency hell" is reduced, and customers save their own DevSecOps resources.

Figure 3 further illustrates the benefits and capabilities of granular artifact versioning with *semver-multi*.  In this 
scenario, we see that two projects--`JAR` and `container image`--constitute the `server project`.  The `server project` 
distributes the server in two forms to the user:  a JAR to be run on the JVM and a Docker image to run as a container 
with Docker Swarm, Kubernetes, or other container orchestration system.

<p align="center">
   <img width="75%" alt="Granular Artifact Versioning Solution with semver-multi" src="resources/multiple-artifact-version-solution2-semver-multi.png">
</p>
<p align="center">Figure 3 -- Granular Artifact Versioning Solution with <i>semver-multi</i></p>

Consider a scenario where developers add a new feature to the `container image`.  Without granular semantic versioning, 
both the container image and the JAR distributions (along with all the other project artifacts) receive a version 
bump, though only the container image changed.  With granular semantic versioning by *semver-multi*, only the container 
image version is incremented but the JAR version remains the same.

# Capabilities

Primary capabilities provided by *semver-multi* include:
1. Automatic semantic version generation for each artifact in the project for
   1. releases on configured branches (e.g., `main`)
   1. developer releases from the developer's machine (using scripts committed to the Git repository) and test releases
1. Easy integration with the CI/CD pipeline and a CI server, such as [Jenkins](https://www.jenkins.io/)
1. Server and client-side Git hooks to enforce standardized Git commit messages
1. Utilities to
   1. validate, display, and query the `semver-multi.json` project definition file
   1. create, update, and validate the project version data committed in a Git tag

# Approach

*semver-multi* takes the following approach:

1. [Produce Semantic Version Numbers Compliant with the Semantic Versioning Specification](#produce-semantic-version-numbers-compliant-with-the-semantic-versioning-specification)
1. [Integrate with Common Tools to Compute and Apply Semantic Versions](#integrate-with-common-tools-to-compute-and-apply-semantic-versions)
1. [Use Standardized Git Commit Messages per the Conventional Commits Specification](#use-standardized-git-commit-messages-per-the-conventional-commits-specification)
   1. [Write Effective Git Commit Messages](#write-effective-git-commit-messages)
1. [Require the Complete Git Commit Message History](#require-the-complete-git-commit-message-history)
1. [Use a Project Definition File to Describe Project and Artifact Relationships](#use-a-project-definition-file-to-describe-project-and-artifact-relationships)
   1. [Scopes and Types](#scopes-and-types)
   1. [Project Definition File Format](#project-definition-file-format)
1. [Store Versioning Data in the Git Repository](#store-versioning-data-in-the-git-repository)
1. [Record All Inputs in the Git Repository](#record-all-inputs-in-the-git-repository)
1. [Produce Easily-Consumable Version Output](#produce-easily-consumable-version-output)
1. [Architecture and Operation](#architecture-and-operation)
1. [Implement in Babashka](#implement-in-babashka)


## Produce Semantic Version Numbers Compliant with the Semantic Versioning Specification

*semver-multi* generates version numbers in accordance with the 
[Semantic Versioning specification](https://semver.org/).  The specification defines a set of rules and requirements 
that determines how a version number is incremented, which helps:
1. clearly indicate--both to customers and the team--the nature and potential value and impact (e.g., new features or a 
   backwards incompatible change) in a new artifact version
1. objectively determine the next version increment (if any) for an artifact

A semantic version takes the form `<major version>.<minor version>.<patch version>` and increments the
- *major version* for backwards incompatible (e.g. breaking) changes
- *minor version* for added features that are backwards compatible
- *patch version* for backwards compatible bug fixes

Consider, for example, a semantic version for a mainline release (such as from the `main` branch in the Git repository) 
may be `1.2.3`.  Given this, then:
- a *bug fix* commit will result in a new version of 1.2.4
- a *new feature* commit will result in a new version of 1.3.0
- a *BREAKING CHANGE* commit will result in a new version of 2.0.0

*semver-multi* also provides test release versioning whose semantic version takes the form `<major version from last 
main tag>.<minor version from last main tag>.<patch version from last main tag>-test+<branch name>.<unique Git object 
name>`.  Per the Semantic Versioning specification, the branch name will consist only of uppercase and lowercase 
letters, numbers, and dashes.  A test release version may look like `1.2.3-test+new-feature.gbba57`.

## Integrate with Common Tools to Compute and Apply Semantic Versions

*semver-multi* easily integrates with common CI/CD tools--or custom ones--to produce and help apply semantic version 
numbers.

To generate semantic version numbers, *semver-multi* needs only access to a local copy of the Git repository.  A tool 
invokes *semver-multi* with a command-line call to trigger the computation of semantic version numbers for a release.  
*semver-multi* accesses a local copy of the Git repository to retrieve:  the last annotated tag that marks a release to 
determine the last version numbers for project artifacts and any other annotated tags thereafter that update project 
information, the `semver-multi.json` project definition to understand the artifacts in the project and their 
relationships, and the Git commit messages to understand what changed and how.  Later sections further describe the 
[inputs](#store-versioning-inputs-in-git-repository) and [architecture and operation](#architecture-and-operation).  
From this information, *semver-multi* computes the semantic version numbers for the configured project artifacts.

*semver-multi* returns to the caller a complete list of semantic version numbers for all projects and artifacts in the 
project (including those that did not change).  Applying the semantic versions to the project artifacts depends on the 
build and CI/CD tooling as well as the project source code.  A build script or CI/CD system could be configured to 
find-and-replace in a file a token that represents a placeholder for the version with the computed semantic version.

This process readily suites most CI/CD tools, such as [Jenkins](https://www.jenkins.io/).

## Use Standardized Git Commit Messages per the Conventional Commits Specification

*semver-multi* requires Git commit messages that follow the 
[Conventional Commits specification](https://www.conventionalcommits.org/).  The specification defines the format and 
content for commit messages.  Standardized commit messages allow *semver-multi* to understand commit messages and 
automatically generate the appropriate artifact-level version numbers.

The first line--the title line--is required and includes a *type*, *scope* (one or two), and *description*.
- *type*: The type of the commit, where *type* is an enumerated value that indicates the intent of the commit, e.g. a 
  feature, bug fix, etc.  Required.
- *scope*: The scope of the commit, where *scope* is an enumerated value that indicates what is affected by the commit.  
  Conventional Commits allows for at most one scope, however *semver-multi* requires at least one scope and allows a 
  second scope for certain *types* as described later.
- *description*: Succinctly describes the commit.  Required.

The optional body provides additional detail about the commit.
- If no body is provided, then the title line represents the entirety of the commit
- If a body is present, then an empty line must separate the title line from the body

A breaking change is indicated by either in the title line by an exclamation point after the closing parenthesis of the 
scope and before the colon e.g. `(<scope>)!: <description>`, by putting `BREAKING CHANGE: <description>` into the body, 
or both.

The *scope* may consist of two scopes, separated by a comma, in the specific case where a change type of 'refactor' or 
'struct' affects both scopes.  Two examples of this case include:
1. Structural changes involving the move of files and possibly directories from one scope to another scope.  For 
   example, source code written for the project 'Client' was later observed to also apply to the future implementation 
   of the 'Server' project.  So some source files from project 'Client' are moved to the new project 'Common'.
1. Internal file changes involving the move of file contents from a file in one scope to a file in another scope.  For 
   example, a function written in project 'Client' was observed to also apply to the future implementation of the  
   'Server' project.  So the function code (not the entire file) was moved from project 'Client' to a new project 
   'Common'.

The general format of a commit message, following the rules described above, is:

```
<type>(<scope>[|,<scope>]): <description>

[| body]
```

Example 1 - title line only (no body), without breaking change, and affecting only one scope:
```
docs(project): correct misspellings and typos in README
```


Example 2 - title line only (no body), with breaking change, and affecting only one scope:
```
feat(api)!: must include API token in all API queries
```


Example 3 - body without breaking change and affecting only one scope:
```
feat(app): allow users to register multiple contact email addresses

User may register more than email address.  Once verified, an email
address may be indicated as 'primary' for the user to login and to
receive email communications.
```


Example 4 - body with breaking change and affecting only one scope:
```
feat(app)!: user login requires username and not email address

User login identifies the user by configurable username and no
longer accepts an email address to identify the user

BREAKING CHANGE: user login requires username, and does not accept
email address
```


Example 5 - a refactor involving moving files from one scope to another scope:
```
refactor(client,common): refactor client code to common
```


todo point to scripts to help enforce commit message; client and server-side


### Write Effective Git Commit Messages

Though not required by *semver-multi*, **effective** Git commit messages have **content** that helps developers 
understand the changes made to the repository; this is especially true when tracking down regressions.  Good commit 
messages also support the validation of changelogs and release notes.

An effective commit message:
- is **atomic**.  Good Commits align to the Single Responsibility Principle where, in this case, a unit of work covered 
  by the commit should concern itself with one task.  This approach helps simplify the process of tracing regressions 
  and corrective actions like reverting.  While atomic commits may introduce some drag with requiring work to be planned 
  and split into smaller chunks, it can improve the overall quality and simplify debugging and corrections related to 
  the repository.
- uses **imperative mood** in the subject line, as if in the tone of giving a command or order, e.g. "add fix for user 
  active state".
- addresses the **why** and **how** a change was made.
- has a description in the title line (first line) as if **answering "This change will &lt;description&gt;."**
- has a body that covers the **motivation for the change and contrasts with previous behavior**.
- uses lowercase and no punctuation in the subject.
- limits the title line to 50 characters and body lines to 72 characters each


## Require the Complete Git Commit Message History

A complete Git commit history informs *semver-multi* of each change and the type of change to every artifact within the 
project.  Actions like rebasing destroy Git commit history.  Common reasons to rebase--and alternatives--include:
1. "Rebasing makes it easier to understand project history because (numerous, intermediate) Git commits mainly have 
   meaning only to the developer"
   1. ALTERNATIVE:  Write and enforce better Git commit messages.  Also, consider using better tools to navigate project 
      history.
1. "Without rebasing, it's harder to revert"
   1. ALTERNATIVE: Use better tools.

todo scripts to help enforce



## Use a Project Definition File to Describe Project and Artifact Relationships

*semver-multi* uses a project definition file--named `semver-multi.json`--to describe the project.  This file enables 
*semver-multi* to understand all the subprojects and artifacts in the project as well as the relationships between them 
to compute semantic version numbers.

The project definition file uses *scopes* and *types* from 
[Conventional Commits specification](https://www.conventionalcommits.org/) to describe the project, subprojects, and 
artifacts and the types of changes that be committed against them.  The file has a set format, expressed in JSON.

The following sections further explanation the project definition file:
1. [Scopes and Types](#scopes-and-types)
1. [Project Definition File Format](#project-definition-file-format)

### Scopes and Types

The *scopes* and *types* in [Conventional Commits specification](https://www.conventionalcommits.org/) act like objects 
and verbs to describe the project:  the *scope* indicates **what** changed, and the *type* indicates **how** it changed.  
The scopes and types defined depend on the needs of the specific project.

*semver-multi* interprets the defined *scopes* and *types* in standardized commit messages to determine versioning 
information.

A *project* scope--perhaps shortened *proj*--can be used to apply to the entire project.  This scope is equivalent to 
no indicated scope, which is permissible under Conventional Commits.  *semver-multi* requires a scope, even for 
project-level scope, so that commits to that level are explicitly considered.

Scope examples appear in Table 1.

<p align="center">Table 1 -- Generic Scope Examples</p>

| Generic Scope | Description with Specific Scope Examples                                                             |
|---------------|------------------------------------------------------------------------------------------------------|
| project       | Applies to entire project (proj) or to subprojects (client and server)                               |
| code          | Application (app), library (lib), API (api), container image (img), Ansible playbooks (infra), etc.  |
| document      | README (readme), user guide, developer guide, etc.                                                   |

Table 2 provides type examples.  The types and their behaviors noted in the table apply by default.  Types can be 
removed, modified, and removed using the [semver-multi.json project definition file](#project-definition-file-format).  
Note that not every type will apply for every scope.

<p align="center">Table 2 -- Type Examples</p>

| Type      | Description                                                                                                                                                                                    | Generic Scope                                | Triggers Build                             | Minor or Patch<sup>1</sup>                 | Direction of change propagation<sup>2</sup> | Can Affect Two Scopes? | Editable |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|--------------------------------------------|--------------------------------------------|---------------------------------------------|------------------------|----------|
| revert    | Revert to a previous commit version                                                                                                                                                            | project                                      | depends on the 'type' of reverted commit   | depends on the 'type' of reverted commit   | depends on the 'type' of reverted commit    | no                     | no       |
| merge     | Merge one branch into another                                                                                                                                                                  | code                                         | depends on the 'type(s)' of merged commits | depends on the 'type(s)' of merged commits | depends on the 'type(s)' of merged commits  | no                     | no       |
| feat      | Add a new feature                                                                                                                                                                              | code                                         | yes                                        | minor                                      | up                                          | no                     | yes      |
| more      | Add code for a future feature (later indicated as complete with 'feat').  Support branch abstraction.                                                                                          | code                                         | yes                                        | patch                                      | up                                          | no                     | yes      |
| change    | Change implementation of existing feature                                                                                                                                                      | code                                         | yes                                        | patch                                      | up                                          | no                     | yes      |
| remove    | Remove a feature                                                                                                                                                                               | code                                         | yes                                        | minor                                      | up                                          | no                     | yes      |
| less      | Remove code for a feature (already indicated as removed with 'remove').  Support branch abstraction.                                                                                           | code                                         | yes                                        | patch                                      | up                                          | no                     | yes      |
| deprecate | Indicate some code is deprecated                                                                                                                                                               | code                                         | yes                                        | patch                                      | up                                          | no                     | yes      |
| fix       | Fix a defect (e.g., bug)                                                                                                                                                                       | code                                         | yes                                        | patch                                      | up                                          | no                     | yes      |
| clean     | Clean-up code                                                                                                                                                                                  | code                                         | no                                         | patch                                      | up                                          | no                     | yes      |
| refactor  | Rewrite and/or restructure code without changing behavior.  Could affect two scopes.                                                                                                           | code                                         | no                                         | patch                                      | up                                          | yes                    | yes      |
| struct    | Project structure, e.g. directory layout.  Could affect two scopes.                                                                                                                            | project                                      | yes                                        | patch                                      | up                                          | yes                    | yes      |
| perf      | Improve performance, as a special case of refactor                                                                                                                                             | code                                         | yes                                        | minor                                      | up                                          | no                     | yes      |
| security  | Improve security aspect                                                                                                                                                                        | code                                         | yes                                        | minor                                      | up                                          | no                     | yes      |
| style     | Does not affect the meaning or behavior                                                                                                                                                        | code                                         | no                                         | patch                                      | up                                          | no                     | yes      |
| test      | Add or correct tests                                                                                                                                                                           | code                                         | no                                         | patch                                      | up                                          | no                     | yes      |
| docs      | Affect documentation.  Scope may affect meaning.  When applied to 'code', affects API documentation (such as documentation for public and protected methods and classes with default javadocs) | project, code, document (e.g., README), etc. | no                                         | patch                                      | up                                          | no                     | yes      |
| idocs     | Affect internal documentation that wouldn't appear in API documentation (such as comments and documentation for private methods with default javadocs)                                         | code                                         | no                                         | patch                                      | up                                          | no                     | yes      |
| build     | Affect build components like the build tool                                                                                                                                                    | project, code                                | no                                         | patch                                      | up<sup>2</sup>                              | no                     | yes      |
| vendor    | Update version for dependencies and packages                                                                                                                                                   | project, code, etc.                          | yes                                        | patch                                      | up<sup>2</sup>                              | no                     | yes      |
| ci        | Affect CI pipeline                                                                                                                                                                             | project, code                                | no                                         | patch                                      | up<sup>2</sup>                              | no                     | yes      |
| ops       | Affect operational components like infrastructure, deployment, backup, recovery, etc.                                                                                                          | project, code                                | yes                                        | patch                                      | up                                          | no                     | yes      |
| chore     | Miscellaneous commits, such as updating .gitignore                                                                                                                                             | project, code                                | no                                         | patch                                      | up                                          | no                     | yes      |

1. *If not indicated as a breaking change, else then is 'major'*
1. *Ideally, all changes should propagate up. Planning the project structure and build can help ensure upward change 
   propagation, which greatly reduces unnecessary version bumps.  Beware that changes to the build (build), vendor 
   dependencies (vendor), and continuous integration pipeline definitions (ci) tend to propagate down.  Using "build by 
   composition", such as in Gradle, can help ensure changes propagate up.*


Table 3 defines type modifiers.

<p align="center">Table 3 -- Type Modifiers</p>

| Modifier | Description                                                                  |
|----------|------------------------------------------------------------------------------|
| ~        | The tilde character may be prefixed to a type to indicate a work-in-progress |


### Project Definition File Format

The project should be described in terms of *scopes* and *types* in a standardized project definition file 
`semver-multi.json`.  That file should then be committed to the Git repository so that *semver-multi* can pull the 
project definition corresponding to the release at that time and understand the project and its artifacts to compute 
semantic version numbers.

The project and each subproject and artifact should be identified and assigned a unique *scope*.  Then for each *scope*, 
one or more *types* should be applied that indicate what categories of changes may be applied to that *scope*.  The 
granularity of subprojects and artifacts defined depends on the granularity of versioning desired.

A project definition file--`semver-multi.json`--captures the project structure and the *scopes* and *types*.  The file 
should be commited to the top-level of the Git repository.

The project definition file should follow the format given by Table 4 and example `semver-multi.json` file in Figure 4, 
although *scopes* and *types* will vary.  The project definition file uses JSON to describe the data.  *semver-multi* 
ignores keys that aren't defined, such that the same `semver-multi.json` file can be used by other systems.

<p align="center">Table 4 -- Description of Select 'commit-msg.cfg.json' Properties</p>

| Property                                             | Description                                                                                                                                                                                                       | Required                                  |
|------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| version                                              | the version of the `semver-multi.json` file format                                                                                                                                                                | yes                                       |
| commit-msg-enforcement.enabled                       | *true* to enable enforcing standardized commit messages and *false* to disable                                                                                                                                    | no                                        |
| commit-msg.length.titleLine.{min, max}               | Sets the minimum (*.min*) and maximum (*.max*) number of characters for the title line (first line) of the commit message                                                                                         | no                                        |
| commit-msg.length.bodyLine.{min, max}                | Sets the minimum (*.min*) and maximum (*.max*) number of characters for a line in the body of the commit message                                                                                                  | no                                        |
| release-branches                                     | Defines the branches on which releases may be tagged                                                                                                                                                              | yes                                       |
| type-override                                        | Override default *type* definitions and behaviors.  Types 'revert' and 'merge' cannot be overridden.                                                                                                              | no                                        |
| project                                              | The top-level project definition.  There must be exactly one of these.                                                                                                                                            | yes                                       |
| {project, projects, artifacts}.name                  | The name of the project or artifact                                                                                                                                                                               | yes                                       |
| {project, projects, artifacts}.description           | The description of the project or artifact                                                                                                                                                                        | no                                        |
| {project, projects, artifacts}.scope                 | The *scope* of the project or artifact.  The *scope* must be unique among other *scopes* and scope aliases at that level.                                                                                         | yes                                       |
| {project, projects, artifacts}.scope-alias           | The scope alias, as a short version of the *scope*, of the project or artifact.  The scope alias must be unique among other scope aliases and *scopes* at that level.                                             | no                                        |
| {project, projects}.paths                            | Defines the path(s) in the repository for the project scope                                                                                                                                                       | yes for root project, optional for others |
| {project, projects, artifacts}.types                 | One or more *types* that define the changes that can be performed on the project or artifact.  *semver-multi* defines default *types* and their behavior, which can be overridden with the 'type-override' field. | yes                                       |
| {project, projects}.includes                         | A list of artifacts that are considered to be included within the project or subproject and are versioned accordingly.  This list is for human use only and is not used by *semver-multi*                         | no                                        | 
| {project, projects}.artifacts                        | A list of artifacts that are contained by the project or subproject                                                                                                                                               | no                                        |
| {project, projects}.projects                         | A list of subprojects that are contained by the project or subproject                                                                                                                                             | no                                        |
| {project, projects, artifacts}.dependsOn<sup>1</sup> | A list of scopes that refer to projects or artifacts that are dependencies for this entity.  A change to a scope listed in 'dependsOn' results in an equivalent change for this entity.                           | no                                        |

1. *The config will be invalid if dependency loops are created with 'dependsOn'.  Use 'dependsOn' cautiously to avoid 
   unexpected and/or unnecessary version bumps.  If using 'dependsOn frequently, evaluate the project structure.*

Figure 4 shows an example `semver-multi.json` file for the hypothetical project shown in Figure 3.

```json
{
   "version": "1.0.0",
   "commit-msg-enforcement": {
      "enabled": true
   },
   "commit-msg": {
      "length": {
         "title-line": {
            "min": 20,
            "max": 50
         },
         "body-line": {
            "min": 2,
            "max": 72
         }
      }
   },
   "release-branches": ["main"],
   "type-override": {
      "add": {
         "int-test": {
            "description": "Add or correct tests",
            "triggers-build": false,
            "version-increment": "patch",
            "direction-of-change": "up",
            "num-scopes": [1]
         }
      },
      "update": {
         "build": {
            "direction-of-change": "down"
         }
      },
      "remove": ["less", "more"]
   },
   "project": {
      "name": "project client-server",
      "description": "Project that produces a client and server",
      "includes": [
         "readme"
      ],
      "scope": "proj",
      "scope-alias": "p",
      "paths": ["(([^\/]*)|(.*((etc)|(gradle)|(resources))\/.*))"],
      "types": [
         "revert",
         "security",
         "build",
         "vendor",
         "struct",
         "ci",
         "ops",
         "docs",
         "chore"
      ],
      "projects": [
         {
            "name": "project client",
            "description": "Project for producing a client",
            "scope": "p-client",
            "scope-alias": "pc",
            "paths": ["client\/.*"],
            "types": [
               "revert",
               "security",
               "build",
               "vendor",
               "struct",
               "ci",
               "docs",
               "chore"
            ],
            "artifacts": [
               {
                  "name": "client-jar",
                  "description": "Client JAR",
                  "scope": "client",
                  "scope-alias": "c",
                  "types": [
                     "feat",
                     "more",
                     "change",
                     "fix",
                     "deprecate",
                     "remove",
                     "less",
                     "clean",
                     "refactor",
                     "perf",
                     "security",
                     "style",
                     "test",
                     "docs",
                     "idocs",
                     "build",
                     "vendor",
                     "ci",
                     "chore"
                  ]
               }
            ]
         },
         {
            "name": "project server",
            "description": "Project for producing a server",
            "scope": "p-server",
            "scope-alias": "ps",
            "paths": ["server\/(([a-zA-Z0-9._-])+|(resources\/.*))"],
            "types": [
               "revert",
               "security",
               "build",
               "vendor",
               "struct",
               "ci",
               "docs",
               "chore"
            ],
            "projects": [
               {
                  "name": "project server-jar",
                  "description": "Project for producing a server JAR",
                  "scope": "p-server-jar",
                  "scope-alias": "psj",
                  "paths": ["server\/jar\/(([a-zA-Z0-9._-])+|(resources\/.*))"],
                  "types": [
                     "revert",
                     "security",
                     "build",
                     "vendor",
                     "struct",
                     "ci",
                     "docs",
                     "chore"
                  ],
                  "artifacts": [
                     {
                        "name": "server-jar",
                        "description": "Server JAR",
                        "scope": "server-jar",
                        "scope-alias": "sj",
                        "types": [
                           "feat",
                           "more",
                           "change",
                           "fix",
                           "deprecate",
                           "remove",
                           "less",
                           "clean",
                           "refactor",
                           "perf",
                           "security",
                           "style",
                           "test",
                           "docs",
                           "idocs",
                           "build",
                           "vendor",
                           "ci",
                           "chore"
                        ]
                     }
                  ]
               },
               {
                  "name": "project server-container-image",
                  "description": "Project for producing a server container image",
                  "scope": "p-server-image",
                  "scope-alias": "psi",
                  "paths": ["server\/docker-image\/(([a-zA-Z0-9._-])+|(resources\/.*))"],
                  "types": [
                     "revert",
                     "security",
                     "build",
                     "vendor",
                     "struct",
                     "ci",
                     "docs",
                     "chore"
                  ],
                  "artifacts": [
                     {
                        "name": "server-container-image",
                        "description": "Server container image",
                        "scope": "server-container-image",
                        "scope-alias": "sci",
                        "types": [
                           "feat",
                           "more",
                           "change",
                           "fix",
                           "deprecate",
                           "remove",
                           "less",
                           "clean",
                           "refactor",
                           "perf",
                           "security",
                           "style",
                           "test",
                           "docs",
                           "idocs",
                           "build",
                           "vendor",
                           "ci",
                           "chore"
                        ],
                        "depends-on": ["server-jar"]
                     }
                  ]
               }
            ]
         }
      ]
   }
}
```
<p align="center">Figure 4 -- Example `semver-multi.json` File</p>

#### Type Override Field

The `type-override` field allows for changing the default type definitions.  New *types* can be added or existing 
*types* can be modified or removed, as shown in Figure 4.  The *types* of *revert* and *merge* cannot be modified. 

A *type* definition consists of a map of the following fields shown in Table 5.

<p align="center">Table 5 -- Type Definition Fields</p>

| Field               | Description                                                                                                                                                                                                                                                                                                                                                                                                                               | Valid Values                                   |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------|
| description         | Describes the field                                                                                                                                                                                                                                                                                                                                                                                                                       | non-empty String                               |
| triggers-build      | 'true' triggers a new build and 'false' otherwise                                                                                                                                                                                                                                                                                                                                                                                         | boolean 'true' or 'false'                      |
| version increment   | Which version part is incremented                                                                                                                                                                                                                                                                                                                                                                                                         | String 'minor' or 'patch'                      |
| direction-of-change | The direction that a version increment propagates through the project graph.  A direction of 'up' propagates from the point of change to the root project; 'down' propagates from the point of change to all connected leaf nodes then up to the root project.  The direction of 'up' is ideal to reduce unnecessary version bumps; care must be taken to both structure the project layout, build, CI, etc. to avoid 'down' proportions. | String 'up' or 'down'                          |
| num-scopes          | The number of *scopes* that can be affected, which allows for that number of *scopes* in the title line of the commit message.  Validation hooks will ensure that changed references in the commit match the allowed number of *scopes*.                                                                                                                                                                                                  | list of allowed number of *scopes* as integers | 

A complete *type* definition is shown in Figure 5.

```json
{
   "int-test": {
      "description": "Add or correct tests",
      "triggers-build": false,
      "version-increment": "patch",
      "direction-of-change": "up",
      "num-scopes": [1]
   }
}
```
<p align="center">Figure 5 -- Complete Type Definition Example</p>

##### Add a New Type Definition

A new *type* definition can be added by defining a `type-override.add.<new type name>` field on the root of the 
`semver-multi.json` project definition file.

Figure 6 provides an example of adding a new type definition *int-test*.  See also Figure 4 for how the definition fits
into a complete `semver-multi.json` project definition file.

```json
{
   "type-override": {
     "add": {
       "int-test": {
         "description": "Add or correct tests",
         "triggers-build": false,
         "version-increment": "patch",
         "direction-of-change": "up",
         "num-scopes": [1]
       }
     }
   }
}
```
<p align="center">Figure 6 -- Adding a New Type Definition</p>


##### Update an Existing Type Definition

An existing *type* definition can be updated by defining a `type-override.update.<existing type name>` field on the 
root of the `semver-multi.json` project definition file.  Only those fields defined will be modified, all other fields 
will remain unmodified.

Figure 7 provides an example of modifying the existing type definition *build*.  See also Figure 4 for how the
definition fits into a complete `semver-multi.json` project definition file.

```json
{
   "type-override": {
      "update": {
         "build": {
            "direction-of-change": "down"
         }
      }
   }
}
```
<p align="center">Figure 7 -- Modifying an Existing Type Definition</p>

todo

##### Remove an Existing Typing Definition

An existing *type* definition can be removed by adding the *type* name to a list in `type-override.remove` field on the
root of the `semver-multi.json` project definition file.

Figure 8 provides an example of removing two existing *type*" definitions: *less* and *more*.  See also Figure 4 for how 
the definition fits into a complete `semver-multi.json` project definition file.

```json
{
   "type-override": {
      "remove": ["less", "more"]
   }
}
```
<p align="center">Figure 8 -- Removing an Existing Type Definition</p>


#### Paths Field

The `paths` field defines a list of one or more String regexes as paths in the repository that pertain to a scope or 
artifact.  The client-side commit hook and server-side update hook validate the specified scope in the commit message 
against the references that actually changed in the commit.  The commit is rejected if the paths of the changed 
references in the commit do not agree with the regex path(s) specified by the scope(s)'s `paths` field.

The `paths` field applies to projects only, not artifacts.  If paths are not defined for a scope, then it inherits the 
paths of its parent.  The paths of an artifact's parent scope applies to the artifact.

The paths regex in the `paths` field is applied as a regex match from the start to the end of the String for the paths 
in the changed references; the `paths` field should not include the start and end of String regex symbols.  A path 
definition must not begin with a '/'.


## Store Versioning Data in the Git Repository

Git annotated tags record the version data, which *semver-multi* reads to compute project and artifact versions.  
Version data specifies the version of each configured project and artifact since the last release or test release; 
version data can also define changes to the structure of projects and artifacts that affect the number of those entities 
or changes to the full scopes.  *semver-multi* defines three types of version data:  *release*, *test-release*, and 
*update*.

*release* type version data defines versions for all projects and artifacts for a release.  Release versions must be 
compliant with the [Semantic Versioning Specification](https://semver.org/) and contain exactly major, minor, and patch 
version numbers.  For a release, *semver-multi* computes changes using the commit message history from the current 
commit until the first commit with an annotated tag containing version data of type *release*, which defines the 
starting version numbers for that release computation.  *semver-multi* requires an initial *release* type version data 
tag to set the beginning versions for projects and artifacts.

*test-release* type version data specifies versions for all projects and artifacts for a test release.  A test release 
may be initiated and used by developers, an automated CI/CD system, or others to create a release for testing purposes 
only.  Test release versions must be compliant with the [Semantic Versioning Specification](https://semver.org/) and 
formatted as `<major version from last main tag>.<minor version from last main tag>.<patch version from last main tag>-
test+<branch name>.<unique Git object name>`.  Per the Semantic Versioning specification, the branch name will consist 
only of uppercase and lowercase letters, numbers, and dashes.  A test release version may look like 
`1.2.3-test+new-feature.gbba57`.  For a test release, *semver-multi* computes changes using the commit message history 
from the current commit until the first commit with an annotated tag contain version data of type *release* or 
*test-release*.

Figure 9 shows the format of version data for a type 'release' or 'test-release'.

```
semver-multi_start
{
   "type": "<release or test-release>",
   "project-root": "<full scope of top-level project>",
   "versions": {
      "<full scope 1>": { "version": "<version e.g., 1.0.0>" },
      "<full scope 2>": { "version": "<version e.g., 1.0.0>" },
      ...
      "<full scope n>": { "version": "<version e.g., 1.0.0>" },
   }
}
semver-multi_end
```
<p align="center">Figure 9 -- Format of Version Data for Type 'release' or 'test-release' in Git Annotated Tags</p>

*update* type version data defines changes that occurred to the project and artifacts, including the number of the 
entities (added or removed) or changes to the scopes (moved or changed).  Such updates are likely to occur during the 
life of the project such as adding or removing artifacts or subprojects, reorganizing artifacts or projects, or 
changing the directory structure.

Figure 10 shows the format of version data for a type 'update'.

```
semver-multi_start
{
   "type": "update",
   "add": [
              {"scope": "<full scope>"
               "version": "<version e.g., 1.0.0>"},
              ...
              {"scope": "<full scope>"
               "version": "<version e.g., 1.0.0>"}
          ],
   "remove": [
                  "<full scope>",
                  ...
                  "<full scope>"
             ],
   "move": [
                {"from-scope": "<from full scope>",
                 "to-scope": "<to full scope">,
                 "version": "<optional starting version e.g., 1.0.0>"},
                ...
                {"from-scope": "<from full scope>",
                 "to-scope": "<to full scope">,
                 "version": "<optional starting version e.g., 1.0.0>"}
            ]
}
semver-multi_end
```
<p align="center">Figure 10 -- Format of Version Data for Type 'update' in Git Annotated Tags</p>


## Record All Inputs in the Git Repository

The Git repository stores all inputs used by *semver-multi* to compute semantic version numbers.  As a result, there is 
no extra data to back-up for semantic versioning purposes beyond the Git repository itself.

Inputs used by *semver-multi*, all stored in the Git repository, consist of:
1. `semver-multi.json` project definition file
1. commit messages
1. version data in annotated tags


## Produce Easily-Consumable Version Output

*semver-multi* provides version computation results in a simple format using JSON, making the version data readily 
usable by many different systems.  Figure 11 shows the format of the version computation result returned by 
*semver-multi*.

```json
{
   "success": <boolean 'true' or 'false'>,
   "reason": "<reason why version computation failed; only set if 'success' is 'false'>",
   "type": <'release' or 'test-release'; only set if 'changed' is 'true'>,
   "changed": <boolean 'true' or 'false' if versions changed from last release or test-release>,
   "changed-list": [
         "<full scope 1>",
         "<full scope 3>",
         "<full scope 12>",
         ...
         "<full scope n>"
      ]
   "project-root": "<full scope of top-level project>",
   "versions": {
      "<full scope 1>": {
         "version": "<version, e.g. 1.0.0>",
         "changed": <boolean 'true' or 'false' if version changed from last release or test-release>
      },
      ...
      "<full scope n>": {
         "version": "<version, e.g. 1.0.0>",
         "changed": <boolean 'true' or 'false' if version changed from last release or test-release>
      }
   }
}
```

<p align="center">Figure 11 -- Format of Version Output from <i>semver-multi</i></p>

If the version computation fails (e.g., 'success' is 'false'), then only these fields are set:
1. success
1. reason

If the version computation succeeds (e.g., 'success' is 'true'), then:
1. 'reason' is not set, but all other fields shown in Figure 11 are set
1. The 'changed-list' includes only those projects or artifacts whose versions changed since the last release or
test-release.  If no assets changed version since that point, then this list is empty.
1. The 'versions' is a map of ALL projects and scopes, regardless of if they changed or not.


## Architecture and Operation

Figure 12 shows the system architecture and operation of *semver-multi* as integrated into a CI/CD pipeline.  The figure 
also illustrates the interaction of *semver-multi* with a CI server, such as [Jekins](https://www.jenkins.io/).

<p align="center">
   <img width="95%" alt="semver-multi Architecture" src="resources/semver-multi-architecture.png">
</p>
<p align="center">Figure 12 -- <i>semver-multi</i> Architecture and Operation</p>

*semver-multi* generates artifact-level version numbers in coordination with the CI server as follows:
1. Developers push to the Git server commits aligning to the 
   [Conventional Commits specification](https://www.conventionalcommits.org/) and preferably enforced by Git hooks 
   (todo link)
   1. Server-side and/or client-side Git hooks may be used.  Server-side hooks are preferred since they are easier to 
      install and enforce and more difficult to bypass.  Client-side hooks may help the developer before server-side 
      hooks come into play; it's easier to correct a single commit rejected locally vs. multiple commits as part of a 
      push that gets the rejected by the server.  Client-side hooks may be the only option if server-side hooks cannot 
      be installed.
1. The CI server becomes aware of new commits to the repository such as through a push notification, poll, or manual 
   trigger
1. The CI server retrieves the current contents of the repository by performing a `git pull` or `git pull` of the 
   repository
1. A local version of the Git repository is now on the filesystem with the CI server and accessible to *semver-multi*
1. The CI server, in the course of building the project in the repository, requests that *semver-multi* generate version 
   numbers for the build
1. *semver-multi* reads from the local copy of the Git repository on the filesystem the:
   1. last Git tag version
   1. annotation in the last Git tag, which contains the versions for the project and its artifacts for the last build
   1. commit message log from the last Git tag to the current commit
   1. `semver-multi.json` (todo link) which describes the project, its subprojects and artifacts, and their 
      relationships
1. *semver-multi* computes the new version numbers for all projects and artifacts
1. *semver-multi* creates a new annotated Git tag with the updated versions
1. *semver-multi* provides a response to the CI server that includes the updated versions for the projects and artifacts
1. The CI server pushes the new Git tag
1. The CI server injects the version numbers as it builds, tests, and delivers/deploys the project artifacts

Note that the process neither changes the contents of the repository nor produces additional commits on the repository.

## Implement in Babashka

[Babashka](https://babashka.org/) is used to implement *semver-multi* as well as the supporting Git hooks and utilities.  
Babashka provides a native Clojure interpreter for scripting.  Babashka was selected because it allowed the 
implementation of scripts--Git hooks and related utilities--to be in the same language as *semver-multi*, which promoted 
significant code re-use.

todo see installing Babashka


# Deploying

The [Architecture and Operation](#architecture-and-operation) section discusses the conceptual operation of 
*semver-multi* in a typical environment with CI server.

The following sections detail the deployment steps for *semver-multi*:
1. [Setup semver-multi](#setup-semver-multi)
   1. [Option 1: Babashka Setup](#option-1-babashka-setup)
   1. [Option 2: Container Image Setup](#option-2-container-image-setup)
1. [Configure the Git Server](#configure-the-git-server)
1. [Configure the Developer Environments](#configure-the-developer-environments)
1. [Create the Project Definition File](#create-the-project-definition-file)
1. [Commit the Initial Version Information](#commit-the-initial-version-information)
1. [Integrate with the CI Server](#integrate-with-the-ci-server)

## Setup semver-multi

*semver-multi* can be run either as a Babashka script or as a Docker container, as discussed in the next sections.

### Option 1: Babashka Setup

Install Babashka.  See the [Babashka installation instructions](#install-babashka).

todo copy the semver-multi script from???, and place the *semver-multi* script at a convenient location on the file 
system.

Ensure that the script is executable (`chmod +x semver-multi` todo) and that the user of the script, such as the CI 
server, has the proper permissions to execute it.

- access to local Git repo

### Option 2: Container Image Setup

- access to local Git repo

## Configure the Git Server

- hooks
- babashka

## Configure the Developer Environments

- hooks
- babashka

## Create the Project Definition File

## Commit the Initial Version Information

## Integrate with the CI Server

- trigger computer
- apply versions

# Managing

## Modifying Version Information

Changes to the project structure and artifacts may require modifying the version information committed as annotated 
tags in the Git repository so that *semver-multi* understands the changes.  Such modifications to the project may 
include:  adding or removing a subproject or artifact, or changing the parent/child relationship between subprojects or 
artifacts (e.g., changing the directory structure or moving subprojects or artifacts).

todo

# Git Hooks

*semver-multi* provides Git hooks to facilitate semantic versioning.  The client and server-side `commit-msg-enforcement` 
scripts are particularly important as they help ensure standardized Git commit messages.

Ideally, both server-side and client-side hooks would be used.  Server-side hooks are easier to ensure enforcement as 
they need only be deployed and managed in one place (e.g., the server) and not installed for every development 
environment; server-side hooks are also more difficult to bypass than client-side hooks.  However, server-side hooks 
require admin or root control of the server hosting the Git repository, in which case client-side hooks are the only 
option.

Even with server-side hooks, client-side hooks can add some benefit for developers such as helping to warn of some 
condition locally before attempting to push such issues to the remote server.

| Purpose                                         | Client or Server-Side | Git Hook Name | Script Name            | Configuration?           |
|-------------------------------------------------|-----------------------|---------------|------------------------|--------------------------|
| Enforce standardized Git commit messages        | client-side           | commit-msg    | commit-msg-enforcement | Uses `semver-multi.json` |
| Prevent rebasing, which destroys commit history | client-side           | pre-rebase    | prevent-rebase         | none                     |
| Warn when committing to 'main' branch           | client-side           | pre-commit    | warn-commit-branch     | none                     |
| Warn when pushing to 'main' branch              | client-side           | pre-push      | warn-push-branch       | none                     |


## Using Git Hooks

### Install Babashka

See [Babashka](#babashka).

### Install Client-side Git Hooks

Copy the script(s) to the client's `<git repository>/.git/hooks`

Make the script(s) executable with `chmod +x <script name>`



# Utilities

*semver-multi* provides utility scripts.

| Purpose                                                           | Script Name        |
|-------------------------------------------------------------------|--------------------|
| Validate, display, and query the `semver-multi.json`              | semver-def-display |
| Create, update, and validate project version data for the Git tag | semver-ver         |

## Install Babashka

See [Babashka](#babashka).

### Install the Utilities

Copy the script(s) to a directory, such as `~/semver-multi/util` 

Make the script(s) executable with `chmod +x <script name>`

Put the path to the script(s) in your path by adding this line to your `~.bashrc`: `export PATH="$HOME/semver-multi/util:$PATH"`

# Building from Source

## Initial Setup

Clone the *semver-multi* repository.

## Environment Setup

The environment to build *semver-multi* from source can be setup in one of two ways:
1. [Development Containers](#development-containers)
1. [Traditional System Setup](#traditional-system-setup)

### Development Containers

The *semver-multi* project provides a [development container (e.g., dev container)](https://containers.dev/).  A development container is a 
Docker container that's configured to provide a complete development environment.  Many popular IDEs natively support 
development container-based development.

The instructions that follow cover the setup for using a development container with [Visual Studio (VS) Code](https://code.visualstudio.com/).  If 
using a different IDE, then consult the IDE's documentation for using development containers.

To use development containers with VS Code:
1. [Install Docker](https://docs.docker.com/get-docker/)
1. [Install VS Code](https://code.visualstudio.com/download)
1. Close the *semver-multi* project, if it's opened in VS Code
1. Install the "Dev Containers" extension
   1. Click the "extensions" icon on the left vertical menu or press "F1" for the Command Palette and search for 
      "Extensions: Install Extensions"
   1. Search "Dev Containers" and install the one produced by Microsoft
1. Choose "Open Folder" and open the *semver-multi* repository.  Do NOT use a workspace.
1. VS Code should display a prompt at the bottom right saying that this repository contains a Dev Container and asking if the project should be re-opened in the Dev Container.  If so, click "yes."  If not, do one of the following:
   1. Option 1:
      1. Click the small, blue icon at the bottom left
      1. Select "Reopen in Container"
   1. Option 2:
      1. Select "F1" for the Command Palette
      1. Search for "Dev Containers: Reopen in Container"

Once setup, start a terminal in the container by selecting "Terminal" on the top vertical menu bar then "New Terminal."  
See [Building the Artifacts](#building-the-artifacts) to build the project's artifacts.

See the [VS Code tutorial](https://code.visualstudio.com/) for more information on using development containers with VS Code.

### Traditional System Setup

Setup a traditional system (e.g., not a dev container) by following these steps:
1. [Install Babashka](#babashka)
1. [Install Java](#java), selecting the JDK option

See [Building the Artifacts](#building-the-artifacts) to build the project's artifacts.

## Building the Artifacts


# Contributing


# Tools

## Babashka

*semver-multi* as well as the supporting tools are implemented in [Babashka](https://babashka.org/).  Babashka provides a native Clojure 
interpreter for scripting.  Babashka was selected because it allowed the implementation of both the *semver-multi* 
application and the tools in the same language, which promoted significant code re-use.

See the [Babashka site](https://babashka.org/) or the [Babashka GitHub](https://github.com/babashka/babashka) for further details on Babashka.

To install Babashka:
1. Use the [Babashka GitHub installation](https://github.com/babashka/babashka?tab=readme-ov-file#quickstart) instructions to install Babashka.

Note that Babashka requires Java.  See [Java](#java).

## Java

Babashka requires Java.  Non-development environments require the Java Runtime Environment (JRE).  Development 
environments require the Java Development Kit (JDK), which includes the JRE.

To install Java:
1. Select the Java provider of your choice, such as:  [Adoptium](https://adoptium.net/temurin/releases/) or [OpenJDK](https://openjdk.org/), and follow any specific 
   provider directions over those that follow
1. Download the desired Java version
1. Extract the file to `/lib/jvm/<new java version>`
1. Set `JAVA_HOME`
   1. In `~/.bashrc`, add `export JAVA_HOME=<path to the Java install>`
1. Update the `PATH`
   1. In `~/.bashrc`, add `export PATH="<path to Java install>/bin:$PATH"`

Note that users of development-based systems may choose to use [jenv](https://github.com/jenv/jenv) to manage multiple Java installations, which 
would change some of the installation instructions above.


# License
The *semver-multi* project is released under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)


# References
1. [Semantic Versioning 2.0.0](https://semver.org/), downloaded 7 Apr. 2024.







