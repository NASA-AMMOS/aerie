*** Settings ***
Documentation   This is an example test
Library         SeleniumLibrary

***Variables***
${BROWSER}  %{BROWSER}

*** Test Cases ***
User can open NEST
  Open Browser  host.docker.internal:4200  ${BROWSER}
  Capture Page Screenshot