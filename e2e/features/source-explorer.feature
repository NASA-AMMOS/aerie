Feature: SourceExplorer
  As a user of RAVEN
  I need to look-up different sources
  So that I can plot activities and resource to gain planning insight

  Scenario: Click on a parent Source Explorer node
    Given I am on the RAVEN page
    When I click on a + for a parent node in the Source Explorer
    Then I should see the children nodes loaded and displayed under the parent
