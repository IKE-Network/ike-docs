---
date_published: 2026-05-10
date_modified: 2026-05-10
canonical_url: https://github.com/IKE-Network/ike-docs/workspace-getting-started.html
---

# Workspace Developer Getting Started

This guide walks you through setting up an IKE workspace and working on tinkar/komet components day to day. For conventions and architecture rationale, see the [Workspace Conventions](workspace-conventions.html)[1] reference.

## [#prerequisites](#prerequisites)Prerequisites

Java 25 Download from [https://jdk.java.net/25/](https://jdk.java.net/25/)[2]. The workspace builds with `--enable-preview` across all modules. Maven 4.0.0-rc-5 or later Download from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)[3]. All POMs use model version `4.1.0`. Git Any recent version. SSH access to `github.com/ikmdev` and `github.com/IKE-Community` orgs. Maven settings — `network.ike` plugin group Add to `~/.m2/settings.xml` so that `ike:` prefix goals resolve:

```
<settings>
  <pluginGroups>
    <pluginGroup>network.ike</pluginGroup>
  </pluginGroups>
</settings>
```

## [#first-time-workspace-setup](#first-time-workspace-setup)First-Time Workspace Setup

### [#clone-the-workspace-repository](#clone-the-workspace-repository)Clone the workspace repository

```
git clone git@github.com:IKE-Community/ike-workspace.git
cd ike-workspace
```

The workspace repo contains `pom.xml`, `workspace.yaml`, and file-activated profiles for every component.

### [#initialize-components](#initialize-components)Initialize components

Clone all components declared in `workspace.yaml`:

```
mvn ws:init
```

This clones every component into a subdirectory matching its `name` field in the manifest. Each clone lands on the branch declared in `workspace.yaml` (default: `main`).

For a smaller initial checkout, use a group:

```
mvn ws:init -Dgroup=core
```

This clones only `ike-pipeline` and `tinkar-core` — enough to build the foundation and start working.

### [#open-in-intellij](#open-in-intellij)Open in IntelliJ

Open the workspace `pom.xml` as a project. File-activated profiles automatically include only the components you have checked out. Missing components are silently skipped — no red underlines, no broken reactor.

## [#daily-workflow](#daily-workflow)Daily Workflow

### [#sync-all-repositories](#sync-all-repositories)Sync all repositories

```
mvn ws:pull
```

Runs `git pull --rebase` in every checked-out component.

### [#check-status](#check-status)Check status

```
mvn ws:status
```

Shows branch, uncommitted/clean state, and branch mismatch detection across all checked-out repos.

### [#full-overview](#full-overview)Full overview

```
mvn ws:dashboard
```

Composite goal: runs `ws:verify` (manifest consistency), `ws:status` (git state), and cascade analysis for release-pending components.

## [#starting-a-feature](#starting-a-feature)Starting a Feature

### [#create-the-feature-branch](#create-the-feature-branch)Create the feature branch

```
mvn ws:feature-start -Dfeature=my-feature
```

This creates a `feature/my-feature` branch in every checked-out component and sets branch-qualified POM versions (e.g., `24-my-feature-SNAPSHOT`).

To scope the feature to a group:

```
mvn ws:feature-start -Dfeature=my-feature -Dgroup=core
```

### [#work-and-commit](#work-and-commit)Work and commit

Work across repos, commit normally with `git add` / `git commit`. Branch-qualified versions are already set — no manual POM edits needed.

### [#save-a-checkpoint](#save-a-checkpoint)Save a checkpoint

```
mvn ws:checkpoint -Dname=progress
```

Records SHAs, versions, and uncommitted-change flags for every component into `checkpoints/checkpoint-progress.yaml`. Useful before risky operations or as a team-visible progress marker.

### [#preview-the-merge](#preview-the-merge)Preview the merge

```
mvn ws:feature-finish -Dfeature=my-feature -DdryRun=true
```

Shows what would happen: which branches merge, version changes, tag names.

### [#finish-the-feature](#finish-the-feature)Finish the feature

```
mvn ws:feature-finish -Dfeature=my-feature -Dpush=true
```

Merges `feature/my-feature` to `main` with `--no-ff` in every affected component, strips the branch qualifier from POM versions, tags the merge commit, and pushes to origin.

## [#releasing](#releasing)Releasing

### [#preview-the-release-plan](#preview-the-release-plan)Preview the release plan

```
mvn ws:release -DdryRun=true
```

Output shows which components are release-pending, their version transitions, and the topological release order:

```
[INFO] === Workspace Release Plan (DRY RUN) ===
[INFO] Release-pending components (topo order):
[INFO]   1. ike-pipeline       24-SNAPSHOT → 24 → 25-SNAPSHOT
[INFO]   2. tinkar-core         1.80.0-SNAPSHOT → 1.80.0 → 1.81.0-SNAPSHOT
[INFO] Cross-reference updates:
[INFO]   tinkar-core: ike-pipeline parent 24-SNAPSHOT → 24
[INFO] === No changes made (dry run) ===
```

### [#execute-the-release](#execute-the-release)Execute the release

```
mvn ws:release -Dpush=true
```

For each release-pending component in dependency order:

1. Strips `-SNAPSHOT` from the version
2. Builds and verifies
3. Tags the release commit
4. Pushes to origin
5. Bumps to next SNAPSHOT version
6. Updates cross-references in downstream POMs

A pre-release checkpoint is created automatically.

## [#multi-machine-development-syncthing](#multi-machine-development-syncthing)Multi-Machine Development (Syncthing)

Syncthing keeps working trees in sync between machines. Git state, build output, and IDE config are per-machine.

### [#generate-ignore-patterns](#generate-ignore-patterns)Generate ignore patterns

```
mvn ws:stignore
```

Writes `.stignore` files that exclude `target/`, `.git/`, `.idea/`, `.DS_Store`, `.claude/worktrees/`, and `.mvn/local-repo/`.

### [#resume-on-another-machine](#resume-on-another-machine)Resume on another machine

Walk away from machine A. Syncthing propagates source files to machine B. On machine B:

```
cd ike-workspace
mvn ws:pull
```

This syncs Git history for all components. `ws:init` is Syncthing-aware: if a directory already exists (synced files, but no `.git`), it runs `git init` + `git reset` instead of `git clone`.

## [#troubleshooting](#troubleshooting)Troubleshooting

### [#ws-release-fails-mid-cascade](#ws-release-fails-mid-cascade)`ws-release` fails mid-cascade

The pre-release checkpoint file records the state of each component before the release started. Re-running `mvn ws:release` skips components that were already tagged and released — it picks up where it left off.

### [#merge-conflict-during-feature-finish](#merge-conflict-during-feature-finish)Merge conflict during `feature-finish`

Resolve the conflict manually in the affected repository, commit the merge resolution, then re-run:

```
mvn ws:feature-finish -Dfeature=my-feature -Dpush=true
```

The goal detects already-merged components and skips them.

### [#ws-init-on-a-syncthing-directory](#ws-init-on-a-syncthing-directory)`ws:init` on a Syncthing directory

Handled automatically. When a component directory already exists but has no `.git` directory, `ws:init` runs `git init` followed by `git reset` to the manifest branch instead of cloning.

### [#plugin-not-found-ike-goals-fail](#plugin-not-found-ike-goals-fail)Plugin not found: `ike:*` goals fail

Verify that `~/.m2/settings.xml` contains `network.ike` in `<pluginGroups>`:

```
<pluginGroups>
  <pluginGroup>network.ike</pluginGroup>
</pluginGroups>
```

Also confirm that `ike-maven-plugin` is declared in the workspace `pom.xml`.

### [#updating-the-parent-version-after-an-ike-pipeline-](#updating-the-parent-version-after-an-ike-pipeline-)Updating the parent version after an ike-pipeline release

After a new ike-pipeline release, update the workspace to use the new parent version:

```
mvn ws:set-parent-draft -Dparent.version=94    # preview changes
mvn ws:set-parent-publish -Dparent.version=94   # apply to all POMs
```

This updates the root POM and all component POMs in one pass. If you omit `-Dparent.version`, the goal prompts interactively.

### [#build-warnings-about-bom-imports](#build-warnings-about-bom-imports)Build warnings about BOM imports

There should be zero warnings about BOM imports. If you see them, check for stale `ike-bom` references in dependent POMs. The BOM is auto-generated from `ike-parent` — manual version references can drift after a release.
