# Contributing
There are several steps involved in contributing to the system:
  1. Create a JIRA ticket
        Create a ticket for the issue that needs to be addressed. A list of JIRA tickets that need work exists in the
        leftmost column of the
        [RapidBoard](https://jira.jpl.nasa.gov/secure/RapidBoard.jspa?rapidView=1179&projectKey=AERIE).
  2. Claim the ticket
        To claim a ticket, open it up and assign it to yourself. You can now click and drag it to the In Progress column
        on the RapidBoard, indicating that you are working on the ticket.
  3. Create a branch
       Now that you have your ticket open, you can make a new branch from the repo. Use a branch name that corresponds to
       the issue being resolved -- it is good to start with the ticket number. For example, a branch to add a feature to
       delete activities, as specified by a ticket AERIE-001 would be called `AERIE-001--delete-activities`.
  4. Make the changes
       Now on your branch, go ahead and make your changes to the code. Only work on the items defined by the ticket. If you
       notice other issues that need to be resolved, create a separate ticket. It is okay to make small modifications that
       are unrelated to your ticket, such as fixing typos, are minor formatting fixes. In general, try to make a commit
       after each meaningful change, and commit before leaving for a meeting, or to go home for the day.
  5. Write Tests
       After making changes, it is important to test that the code performs as expected. If tests do not already exist for
       the feature worked on, create them both to test behavior now, and in the future when the code may be modified.
  6. Perform tests
       Once all coding is complete, it is time to test that your changes do not break the system. Run the unit tests. If anything fails, determine if the issue is real, and if so fix it. If any issues
       appear to not be a result of your changes, run the same tests on the develop branch to check if they are failing
       there as well. If they are, then inform the developer in charge of the test.
  7. Open a pull request
       To open a pull request, head over to [GitHub](https://github.jpl.nasa.gov/MPS/aerie) and create the pull request for
       your branch. When you do this, be sure to go back to the
       [RapidBoard](https://jira.jpl.nasa.gov/secure/RapidBoard.jspa?rapidView=1179&projectKey=AERIE) and move the
       corresponding JIRA ticket to the In Review column. Be sure to use the ticket name in the name of the PR
       (if you followed the branch naming convention in step 3, the branch name will be just fine). Be sure to request
       review from interested developers, and be ready to respond to comments and requests.
  8. Complete the pull request
       Make any required fixes, rerun the tests, and when everything looks good, and you have received another developer's
       approval, you may merge your branch. Congratulations, you've contributed!

**tl;dr**
All development should correspond to a JIRA ticket, and branch names and PRs should include the ticket name.

## Updating Your Branch
From the time you branch off of the main branch to begin working a ticket to the time you open your pull request it is
possible that other branches will be merged. If this occurs, your code will fall behind the development branch. There are
two main options Git provides for dealing with this issue.
  1. `git merge`
        `git merge` pulls in all the updates that your branch does not have, and combines them with the updates you have
        made in a single merge commit. This allows you to deal with any and all conflicts at once, but information such as
        when conflicts originated is lost.
  2. `git rebase`
        `git rebase` actually _rebases_ your branch from the current development branch's endpoint. This localizes conflicts
        to the commits at which they actually appear, though it can become complicated when there are more than a few
        conflicts.
For this project we prefer to use `git rebase` as it allows us to keep our repository history neat, and
easy to utilize, however if you are more comfortable with `git merge` in a given situation, that is okay. For more
info on `git merge` vs `git rebase` see [here](https://www.atlassian.com/git/tutorials/merging-vs-rebasing).

**tl;dr**
`git rebase` is preferred to `git merge` in most situations.

### Coding Standards

This needs to be filled in.

### Asking for Help

If you have a question, a great place to reach out is the mpsa-aerie Slack channel. This allows you to ask questions in a
non-disruptive manner. If your issue is more urgent, you can mention someone in your message using @, or you can check if
the person you need is in their office.

We also have standup meetings every Tuesday and Thursday at 01:45 in the 301-265A where you can identify to the team latest
accomplishments, current tasks, obstacles, and any small questions, though we try to keep this meeting short, so save larger
discussions for other times. We also have Slackups on Monday, Wednesday and Friday, also at 01:45.

**tl;dr**
Reach out in mpsa-aerie or mpsa-aerie-merlin Slack channels, and attend standups at 01:45 pm on Tuesdays/Thursdays.

### Useful Links
GitHub Repo: https://github.jpl.nasa.gov/MPS/aerie

RapidBoard: https://jira.jpl.nasa.gov/secure/RapidBoard.jspa?rapidView=1179&projectKey=AERIE&view=planning.nodetail&epics=visible
