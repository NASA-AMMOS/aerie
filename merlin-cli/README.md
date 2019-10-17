# Merlin Command Line Interface

## Capabilities

### Adaptation

1. Create an adaptation [-A, --create-adaptation]
   - merlin-cli --create-adaptation <path-to-adaptation-jar>
1. Query for a list of available adaptations [-adaptations, --list-ions]
   - merlin-cli --list-adaptations
1. Query for metadata of a adaptation by ID [-display, --view-adaptation]
   - merlin-cli --adaptation-id <adaptation-id> --view-adaptation
1. Query for a list of activity types in an adaptation [-activities, --activity-types]
   - merlin-cli --adaptation-id <adaptation-id> --activity-types
1. Query for an activity type by ID in an adaptation [-activity, --activity-type]
   - merlin-cli --adaptation-id <adaptation-id> --activity-type <activity-type-id>
1. Query for an activity type's parameters in an adaptation [-parameters, --activity-type-parameters]
   - merlin-cli --adaptation-id <adaptation-id> --activity-type-parameters <activity-type-id>
1. Delete an adaptation by ID [--delete-adaptation]
   - merlin-cli --adaptation-id <adaptation-id> --delete-adaptation

*Note:* -a can be used instead of --adaptation-id

### Plan

1. Create a plan [-P, --create-plan]
   - merlin-cli --create-plan <path-to-plan-json>
1. Query for a list of available plans [-plans, --list-plans]
   - merlin-cli --list-plans
1. Query for an activity instance by ID in an adaptation [--display-activity]
   - merlin-cli --plan-id <plan-id> --display-activity <activity-instance-id>
1. Update plan metadata (ex: adaptationID, startTimestamp) [--update-plan]]
   - merlin-cli --plan-id <plan-id> --update-plan <field>=<value> ...
   - Valid fields are:
     - adaptationId
     - startTimestamp
     - endTimestamp
1. Update plan activity instance list by file upload [-U, --update-plan-from-file]
   - merlin-cli --plan-id <plan-id> --update-plan-from-file <path-to-json>
1. Add activity instances to a plan by file upload [--append-activities]
   - merlin-cli --plan-id <plan-id> --append-activities <path-to-json>
1. Download a plan JSON [-pull, --download-plan]
   - merlin-cli --plan-id <plan-id> --download-plan <output-path>
1. Update an activity instance by ID in a plan [--update-activity]
   - merlin-cli --plan-id <plan-id> --update-activity <field>=<value>
   - Valid fields are:
     - start
     - startTimestamp
     - end
     - endTimestamp
     - duration
     - intent
     - name
     - textColor
     - backgroundColor
     - y
1. Delete an activity instance from a plan by id [--delete-activity]
   - merlin-cli --plan-id <plan-id> --delete-activity <activity-instance-id>
1. Delete a plan by ID [--delete-plan]
   - merlin-cli --plan-id <plan-id> --delete-plan

*Note:* -p can be used instead of --plan-id

### Other

1. Convert an APGen APF file to a Merlin JSON Plan file (requires adaptation .aaf files) [-c, --convert-apf]
   - merlin-cli --convert-apf <path-to-apf-file> <output-path> <path-to-adaptation-directory>
