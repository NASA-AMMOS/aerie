========================
Issues and Pull Requests
========================

Any documentation contribution, no matter how small, is made as a pull request (PR) on `our GitHub <https://github.com/NASA-AMMOS/aerie>`_.
You can use a client, the Web UI, or the command line to manage your PRs.

Issue assignment
================

The Aerie repository has `an issues page on GitHub <https://github.com/NASA-AMMOS/aerie/issues>`_.
Doc issues are labeled with a ``documentation`` label.
Please do not work on issues that are not assigned to you.
This avoids working on something someone else is working on.
Also, if there is an issue with any guide and the issue does not exist, please create an issue so it can be tracked.

Guidelines for branch names
===========================

If you are providing documentation alongside new code, prefix the name of your branch with ``feature/AERIE-[Issue Number]--``.

If you are only providing documentation, prefix the name of your branch with ``docs/AERIE-[Issue Number]--``.


Previewing local changes
========================

Before submitting docs changes, we ask that you build them first locally. To do so, you will need:

* `Python 3.7 <https://www.python.org/downloads/>`_ or later.
* `Poetry 1.12 <https://python-poetry.org/docs/master/>`_ or later.

To preview your changes while you are working, run ``make preview`` from the command line in the ``docs`` directory.
If you have previously run ``make preview``, it is recommended to run ``make clean`` first. Navigate to http://127.0.0.1:5500/.
The site will automatically update as you work. Fix all warnings raised during the build.

When you are finished making changes, run ``make clean`` and then ``make dirhtml-ext`` to ensure that the site will deploy.
Once the site builds successfully without warnings, you may proceed to the next step.

To check for broken links, run ``make clean && make dirhtml-ext`` then ``make linkcheck``.
Once ``make linkcheck`` builds with the only broken links being those that reference ``localhost`` sites, you may proceed to open a PR.


Submit a pull request (PR)
==========================

We expect that you are aware of how to submit a PR to GitHub. If you are not, please look at this `tutorial <https://docs.github.com/en/get-started/quickstart/hello-world>`_.
When creating a PR, fill out the provided template.

For Documentation PRs, the following guidelines apply:

* Test the instructions against the product. For all tests you must use a clean, new install unless otherwise specified in the issue.
* Make sure the PR renders with no errors and that make preview does not return any errors.
* Cite the issue you are fixing in the PR comments and use screenshots to show changes in formatting.
* In the subject line of the PR prepend the subject with ``[Aerie Issue Number]``.
* Apply the ``documentation`` label to your PR.

If you have any questions about the process, ask the maintainer of the project you're working on.

Best practices for content submission
=====================================

* Always open an issue describing what you want to work on if one doesn't already exist.
* Use GitHub search to see if there is someone else working on the issue already. Look at the open PRs.
* Test the new / changed content using the make preview script. Confirm there are no compilation errors before submitting.
* Give some text to your commit message. Explain why you did what you did. If you changed something in formatting, provide a before and after screenshot.
