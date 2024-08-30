# Contributing

We would love for you to contribute to Aerie and help make it even better than it is today! As a contributor, here are the guidelines we would like you to follow:

- [Question or Problem?](#question)
- [Prerequisites](#prerequisites)
- [Pull Request Guidelines](#pr-guidelines)
- [Good Commit, PR, and Code Review Practices](#best-practices)
- [Submitting a Pull Request](#submit-pr)

## <a name="question"></a> Got a Question or Problem?

If you would like to chat about the question in real-time, you can reach out via [our Slack channel](https://join.slack.com/t/nasa-ammos/shared_invite/zt-1mlgmk5c2-MgqVSyKzVRUWrXy87FNqPw).

## <a name="prerequisites"></a> Prerequisites
Before you begin contributing to our project, it's a good idea to familiarize yourself with the resources described below.

### Code of Conduct

Our Code of Conduct helps facilitate a positive interaction environment for everyone involved with the team, and provides guidance on what to do if you experience problematic behavior. Read more in our [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and make sure you agree to its terms.

### Development Environment

To build and develop Aerie please read through the [developer documentation](./DEVELOPER.md).

### Communication Channels

Before contributing changes to our project, it's a great idea to be familiar with our communication channels and to socialize your potential contributions to get feedback early. This will help give you context for your contributions, no matter their form.

Our communication channels are:
- [Issue tracking system](https://github.com/NASA-AMMOS/aerie/issues) - a regularly monitored area to report issues with our software or propose changes
- [Discussion board](https://github.com/NASA-AMMOS/aerie/discussions) - a permanently archived place to hold conversations related to our project, and to propose as well as show+tell topics to the contributor team. This resource can be searched for old discussions.
- [Our Slack channel](https://join.slack.com/t/nasa-ammos/shared_invite/zt-1mlgmk5c2-MgqVSyKzVRUWrXy87FNqPw) - a space for high-bandwidth communication with other users, as well as the development team.

## <a name="pr-guidelines"></a> Pull Request Guidelines

Here are some general Pull Request (PR) guidelines for the Aerie project:

- Every PR should include a summary of changes that gives reviewers an idea of what they should pay attention to.
- PR branches should have as "clean" of a history as possible.
- Each commit should present one change or idea to a reviewer.
- Commits that merely "fix up" previous commits should be interactively rebased and squashed into their targets.
- Prefer the use of `git rebase` over `git merge`:

  - `git rebase` actually _rebases_ your branch from the current development branch's endpoint. This localizes conflicts to the commits at which they actually appear, though it can become complicated when there are more than a few conflicts.
  - `git merge` pulls in all the updates that your branch does not have, and combines them with the updates you have made in a single merge commit. This allows you to deal with any and all conflicts at once, but information such as when conflicts originated is lost.

  For more info on `git merge` vs `git rebase` see [here](https://www.atlassian.com/git/tutorials/merging-vs-rebasing).

- Before merging a PR, the following requirements must be met. These requirements ensure that history is effectively linear, which aids readability and makes `git bisect` more useful and easier to reason about.
  - At least one (preferably two) reviewers have approved the PR.
  - No outstanding review comments have been left unresolved.
  - The branch passes continuous integration.
  - The branch has been rebased onto the current `develop` branch.
- The "Squash and merge" and "Rebase and merge" buttons on GitHub's PR interface should not be used. Always use the "Merge" strategy.
  - In combination with the restrictions above, this ensures that features are neatly bracketed by merge commits on either side, making a clear hierarchical separation between features added to `develop` and the work that went into each feature.

## <a name="best-practices"></a> Good Commit, PR, and Code Review Practices

The Aerie project relies on the ability to effectively query the Git history. Please read through the following resources before contributing:

- [How to write a good commit message](https://chris.beams.io/posts/git-commit/)
- [Telling Stories Through Your Commits](https://blog.mocoso.co.uk/talks/2015/01/12/telling-stories-through-your-commits/)
- [How to Make Your Code Reviewer Fall in Love with You](https://mtlynch.io/code-review-love/)

## <a name="submit-pr"></a> Submitting a Pull Request

Please follow these instructions when submitting a Pull Request:

1. Search [GitHub](https://github.com/NASA-AMMOS/aerie/pulls) for an open or closed PR that relates to your submission. You don't want to duplicate effort.
1. Be sure that an issue describes the problem you're fixing, or documents the design for the feature you'd like to add. Discussing the design up front helps to ensure that we're ready to accept your work.
1. Clone the [Aerie repo](https://github.com/NASA-AMMOS/aerie).
1. Make your changes in a new git branch:

   ```sh
   git checkout develop
   git pull origin develop
   git checkout -b my-fix-branch develop
   ```

1. Create your patch.
1. Commit your changes using a descriptive commit message that follows our [commit conventions](#best-practices).

   ```sh
   git commit -a
   ```

   Note: the optional commit `-a` command line option will automatically "add" and "rm" edited files.

1. Push your branch to GitHub:

   ```sh
   git push origin my-fix-branch
   ```

1. In GitHub, send a pull request to `aerie:develop`.
1. If we suggest changes then:

- Make the required updates.
- [Rebase your branch](https://dev.to/maxwell_dev/the-git-rebase-introduction-i-wish-id-had) and force push to your branch to GitHub (this will update your Pull Request):

  ```sh
  git rebase develop -i
  git push -f
  ```

After your pull request is merged, you can safely delete your branch and pull the changes from the repository:

- Check out the develop branch:

  ```shell
  git checkout develop
  ```

- Update your develop with the latest version:

  ```shell
  git pull origin develop
  ```

- Delete the local branch:

  ```shell
  git branch -D my-fix-branch
  ```
