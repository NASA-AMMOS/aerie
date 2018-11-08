## Raven Conversion Script

This script converts a Raven 1 State downloaded in JSON format to a equivelant Raven 2 State in JSON format.  The user can then upload this converted state to a specified data source in Raven 2, and apply this state as either a State or Layout (Raven 2 has no differentiation between States and Layouts).  This conversion script will convert States that contain APGen TOL, Telemetry or Generic files.  The process bellow contains all the steps to fully convert a Layout in Raven 1 to a State in Raven 2.  

### Prereqisites:
1. Running instances of Raven 1 and Raven 2, that use the same data_source where the files are contained.  This can also be the same MPS Server instance where the parameter raven.dir in the MPSServer.cfg file is altered between the install locations of the Raven 1 and Raven 2 build files.  The transistion between Raven 1 and Raven 2 occurs between steps 6 and 7.  An advanced option is to run two instances of MPSServer on different ports with similar configurations.  
2. Knowledge of where Raven 1 saves the State.  This can be found in the Raven configuration file for Raven 1, under the parameter statesURL.
3. A folder in Raven 2 to where the converted State will be saved.  

### Conversion Procedure:

The process for converting a Raven 1 Layout to a Raven 2 State is as follows (**Skip to step 5 if you have a Raven 1 State**):

1.  Expand the Source Explorer to the right of the screen in Raven 1 to a data source that is applicable to the Raven Layout you wish to convert, Expanding to the top level of the file that you wish to apply the layout to.  (i.e. This would be the name of the TOL file one level above the Activites By Legend, Activities By Type and Resources level)
2.  Click on the down arrow at the top of the Source Explorer in the purple bar to the right of the word RAVEN, and select Manage Data Sources from the dropdown menu.
3.  Select the Layouts Tab at the top of the menu that appears.  Find the Layout in this list and click the green checkbox to the right of this Layout.  Say Yes to any additional dialogs.  Wait for the Layout to be fully applied to Raven.  For larger Layouts this can take some time (~5min).
4.  Once again, Open the Manage Data Sources menu as done prior.  Click the States tab, and enter a `<NAME>` for this state.   
5.  Open a command prompt.  Use the following curl command:
    `curl -k -L -XGET https://<SERVER(:PORT)>/mpsserver/api/v2/fs/<StatesPath>/<NAME> > <NAME>.json`
    This will downlaod the state and save it to `<NAME>.json`.
6.  Run the following command to convert `<NAME>.json` to a Raven 2 State named `<NAME2>`: `convert_raven_state <NAME>.json > <NAME2>.json`
7.  Open the instance of Raven 2.  Upload this json file to an `<EXISTING_FOLDER>` in the Source Explorer using the following command: `curl -k -L -XPUT --header Content-Type:application/json -T <NAME2>.json https://mpsserver.jpl.nasa.gov:8001/mpsserver/api/v2/fs/<PATH_TO_EXISTING_FOLDER>/<NAME2>?timeline_type=state`
8.  Navigate Raven 2 to the `<EXISTING_FOLDER>`, click on `<NAME2>`, click on the snowman to the right of file, and select Apply As State.  
9.  Allow the state to load.  

### The Raven 1 State should now be successfully ready to be applied as a State or Layout in Raven 2.



