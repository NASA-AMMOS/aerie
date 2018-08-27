## RAVEN 101: Application Layout

<p align="center"><img src="./images/raven_layout.png" width="600" /></p>

*<p align="center">Image 1: Once RAVEN is loaded, the following containers will be displayed: (1) the Source Explorer, (2) the Bands Panels, (3) the Details Panel, (4) the Right Panel and (5) the Top Bar.</p>*

## Containers

#### 1 Source Explorer Panel 

The Source Explorer Panel displays in a tree like format the sources that the user can access. These sources will be available once the application is loaded and the user can navigate through them and select sources to visualize. Please refer to the [Source Explorer Section](./Raven_101_source_explorer.md) for more details.

#### 2 Bands Panels 

In the Bands Panels is where RAVEN visualizes the data. These are: the Main Bands Panel and the South Bands Panel. When a graphable source is selected from the Source Explorer, RAVEN will fetch its data and create a band that visualizes the data. This new band will be by default added to the Main Bands Panel. The user also has the option to [select a band](./Raven_101_3_bands.md#how-to-select-a-band) and move it between panels. 

#### 3 Details Panel

When a band is selected from one of the Bands Panels, the Details Panel will be populated with all the data points related to that specific source. Provided information will include start time, end time, value of the data point metadata and more. When a data point is selected, the details panel will scroll to it and highlight it.

#### 4 Right Panel

The Right Panel consists of the following tabs:

###### Selected Band Tab

When a [band is selected](./Raven_101_3_bands.md#how-to-select-a-band), the `Selected Band` Tab will show options to manipulate how the data is visualized. For each type of band there are different options available, please refer to the [Band Type Specific Options Section](/Raven_101_3_bands.md#band-type-specific-options) for more information.

###### Selected Point Tab

When a [data point is selected](./Raven_101_3_bands.md#select-a-data-point), the `Selected Point` Tab will display more details about the data point.  Provided information will include start time, end time, value of the data point metadata and more. 

#### 5 Top Bar

<p align="center"><img src="./images/top_bar.png" width="450" /></p>

*<p align="center">Image 3: The Top Bar contains the (1) Main Menu Button, (2) 'Pan To' Options, (3) Reset Time, (4) Zoom In/Out, (5) Pan Left/Right and (6) the Global Options.</p>*

The Top Bar will be the host of the [Global Settings Options](./Raven_101_3_bands.md#global-settings) (<img src="./images/cog.svg" width="18" />), the Main Menu (<img src="./images/baseline-menu-24px.svg" width="18" />) and other options to [manipulate the view range](./Raven_101_3_bands.md#manipulate-time-range) of the timeline. The Global Settings Options will configure default values when adding bands. Some examples of these are changing the default line color, adding dividers, update the width of the labels and more. Please refer to the  [Global Settings Options](./Raven_101_3_bands.md#global-settings)  for more detailed information.

The Main Menu will display options such as: Manipulation of the Layout, Management of Output, Time Cursor and Epochs, Get Shareable Link and get information About Raven which will provide RAVEN's version and the copyright statement. 

### How to: Manipulate the Application Layout

<p align="center"><img src="./images/toggle_panels_dropdown.png" width="450" /></p>

*<p align="center">Image 4: Available options to manipulate the panels in the application layout.</p>*

1. In the Top Bar, open the Main Menu. To do so, click the hamburger icon (<img src="./images/baseline-menu-24px.svg" width="18" />).
2. In the dropdown, select the `Panels` option. 
3. A list of options to toggle the visibility of RAVEN's panels will appear. Select one of them to toggle one of the panels of the application.

<p align="right"><a href="./Raven_101_2_source_explorer.md">Next: Source Explorer</a></p>

