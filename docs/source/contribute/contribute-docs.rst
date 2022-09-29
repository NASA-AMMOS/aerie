===================================
Aerie docs contributor’s handbook
===================================

If you are reading this guide because you have decided to contribute to Aerie's Documentation, thank you!

The purpose of this handbook is to explain how to contribute new content to Aerie's documentation either as a new
topic or by editing an existing topic.

If you feel something is missing from this document, do not hesitate to let us know. You can use the Feedback
button at the bottom to open an issue.

About Aerie Docs
-----------------

Aerie Docs contains all of the user documentation for installing, maintaining, administering, and developing in Aerie.

How we write
============

Documentation is written primarily for mission model developers, mission planners, and Aerie administrators.
All documentation is saved and tracked on GitHub.
We have created a style guide that breaks down the writing rules.

Languages/toolchain we use
==========================

The backbone of the documentation is written in reStructuredText and is compiled with Sphinx.
Some of our upstream content is written in Markdown and Sphinx also supports Markdown as well.

You will find templates and cheatsheet links later in this document.
You can use them to make sure your document is organized and tagged correctly.


About Issues and Pull Requests
-------------------------------

Any documentation contribution, no matter how small, is made as a pull request (PR) on GitHub.
You can use a client, the Web UI, or the command line to manage your PRs.

Issue assignment
================

The Aerie repository has an issues page on GitHub.
Doc issues are generally labeled with a documentation or docs label.
Although assignment specifics may change from project to project, please do not work on issues that are not
assigned to you.
This avoids working on something someone else is working on.
Also, if there is an issue with any guide and the issue does not exist, please create an issue so it can be tracked.

Guidelines for branch names
===========================

Ask the Maintainer of the project if he/she has any preference for naming branches before you contribute to the repo
to avoid any collisions or confusion.
If you are providing both documentation and code, it is recommended to name all of your documentation branches
with a `doc/` prefix.

Write content
=============

When writing content for Aerie, we use an informal topic-based writing approach.
We generally try to guide our writing and organization by the
`Divio Documentation System <https://documentation.divio.com/>`_:

* Tutorial --- Tutorials are lessons that take the reader by the hand through a series of steps to complete a project or task of some kind.
* Procedural --- Gives instructions on how to use the subject
* Referential --- Gives additional information about the particular topic
* All content must be written in US English. Use as few words as possible and try to keep the reading level to under grade 8. You can use word counters and readability tests to keep the reading level down.

Write procedures
................

Each procedure should have an introductory paragraph (1-3 sentences) which explains what the procedure does, when you should use it, and what benefit the procedure provides.
After the introduction, there should be a numbered list of steps. Use the following guidelines to write the steps:

* Each step should be one single action.
* Steps should be written in clear, simple vocabulary that is easy to follow. If the step includes a code snippet, a screenshot of the expected outcome should follow.
* If the procedure includes changing a configuration, the next step should include how to verify that the configuration change was successful.
* Wherever possible, instructions on how to reverse the action should also be included (not part of the original procedure, but included in a separate procedure.

Aerie Style guide
..................

The Aerie Docs Style guide is being developed. The style guide outlines the way we write documentation. In short, use this handbook, the style guide, and the templates to write content.

Cheatsheets
...........

If you want to use a cheat sheet for Markdown or restructuredText, here are some which are helpful:

* :doc:`Aerie Cheat Sheet <../examples/index>` - samples of |rst| markup.
* `restructuredText Cheat Sheet <https://github.com/ralsina/rst-cheatsheet/blob/master/rst-cheatsheet.rst>`_
* `GitHub Markdown Cheat Sheet <https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet>`_

Best practices for content submission
=====================================

* Always open an issue describing what you want to work on if one doesn't already exist.
* Use GitHub search to see if there is someone else working on the issue already. Look at the open PRs.
* Test the new / changed content using the make preview script. Confirm there are no compilation errors before submitting.
* Give some text to your commit message. Explain why you did what you did. If you changed something in formatting, provide a before and after screenshot.

Previewing local changes
========================

Before submitting docs changes, we ask that you build them first locally. To do so, you will need:

* `Python 3.7 <https://www.python.org/downloads/>`_ or later.
* `Poetry 1.12 <https://python-poetry.org/docs/master/>`_ or later.

To preview your changes while you are working, run ``make preview`` from the command line in the ``docs`` directory. If you have previously run ``make preview``, it is recommended to run ``make clean`` first. Navigate to http://127.0.0.1:5500/. The site will automatically update as you work. Fix all warnings raised during the build.

When you are finished making changes, run ``make clean`` and then ``make dirhtml`` to ensure that the site will deploy. Once the site builds successfully without warnings, you may proceed to the next step.

To check for broken links, run ``make dirhtml`` and then ``make linkcheck``.

Submit a pull request (PR)
==========================

We expect that you are aware of how to submit a PR to GitHub. If you are not, please look at this `tutorial <https://guides.github.com/activities/hello-world/>`_.
Every repository handles PRs differently. Some require you to use a template for submissions and some do not.
Make sure to speak with the project’s maintainer before submitting the PR to avoid any misunderstanding or issues.

If you are writing new content it is **highly recommended** to set your PR to a draft state.
For Documentation PRs, the following guidelines should be applicable to all Aerie projects:

* Test the instructions against the product. For all tests you must use a clean, new install unless otherwise specified in the issue.
* Make sure the PR renders with no errors and that make preview does not return any errors.
* Cite the issue you are fixing in the PR comments and use screenshots to show changes in formatting.
* In the subject line of the PR prepend the subject with ``docs:``.

If you have any questions about the process, ask the maintainer of the project you're working on.
