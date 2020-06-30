Contributions to the Aerie repository should abide by the following conventions:

* Branches should be named `feature/AERIE-XXX--short-desc` or `hotfix/short-desc`, where "AERIE-XXX" is the associated Jira ticket.
  - If a branch is not associated with a ticket, either it's a hotfix or it needs a ticket.
  - Hotfixes should be used sparingly. For instance, a bug introduced in the same release cycle (or discovered very shortly
    after merging a PR) can be hotfixed. Bugs that a user may have been exposed to should be logged as tickets.
* Pull requests should be made against `develop`, using the pull request template in `.github/pull_request_template.md`.
  - GitHub will automatically populate a new PR with this template.
  - Please fill out all information in the PR header, as well as any information in the subsections.
  - Every PR should include a summary of changes that gives reviewers an idea of what they should pay attention to.
  - Any unused or empty subsection may be removed.
* PR branches should have as "clean" of a history as possible.
  - Each commit should present one change or idea to a reviewer.
  - Commits that merely "fix up" previous commits should be interactively rebased and squashed into their targets.
* Before merging a PR, the following requirements must be met. These requirements ensure that history is
  effectively linear, which aids readability and makes `git bisect` more useful and easier to reason about.
  - At least one (perferably two) reviewers have approved the PR.
  - No outstanding review comments have been left unresolved.
  - The branch passes continuous integration.
  - The branch has been rebased onto the current `develop` branch.
* The "Squash and merge" and "Rebase and merge" buttons on GitHub's PR interface should not be used.
  Always use the "Merge" strategy.
  - In combination with the restrictions above, this ensures that features are neatly bracketed by merge commits
    on either side, making a clear hierarchical separation between features added to `develop` and the work
    that went into each feature.
