Feature: User views module versions in an About dialog
  As a user of Nest
  I need to be able to see the versions of the currently loaded modules
  So that I know I am using the correct version

  Scenario: An About dialog is displayed
    Given A button called About in the Nest sidenav exists
    When A user clicks the button
    Then The name of the accessible modules should be displayed in a dialog
    And The version of each module should be displayed

  Scenario: Module versions are displayed using semver
    Given The About dialog is visible
    When The versions are displayed
    Then They should be in the form MAJOR.MINOR.PATCH
