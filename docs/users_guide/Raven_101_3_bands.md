# Bands

<p align="center"><img src="./images/bands_container.png" width="500"/></p>

*<p align="center">Image 1: Bands represent the selected sources in the source explorer. Each band displays the data points for each source. There are two panels: (1) the Main Bands Panel and the (2) the South Bands Panel. The user can move the bands between both panels and arrange their order if desired.</p>*

A band is the result of a selected source from the Source Explorer. When a source is selected a band is added to the Main Bands Panel and displays its data. There are three different band types that the user can add: Activity, Resource and State Bands. 

<p align="center"><img src="./images/bands_types.png" width="600"/></p>

*<p align="center">Image 2: **Band Types.** When a first band is added, the (1) Timeline Band will be also added to the Main Panel. This Band displays the view time range among all the bands. You can hover over this band to see the timestamp or brush on it to change the view range. There are 3 different kinds of bands to display data: (2) Resource Band, (3) State Band and (4) Activity Band.</p>*

Each type of band have their own settings that can configure how data is displayed. In order to do so, you need to select a band of your preference, and the right panel will show the available configuration options.

#### How to Select a Band

<p align="center"><img src="./images/select_band_b.png"/></p>

*<p align="center">Image 3: **How to select a band.** Right side: Click on the band's label area. Left side: As a result of the selection, the band will be highlighted, the details panel will displays the bands data points in a table and the Selected Band Tab in the left panel will be populated with the configuration options for the selected band.</p>*

1. A band should be in the Bands Panel. To do so, [select a graphable source in the Source Explorer](./Raven_101_3_source_explorer.md#select-a-source).

2. Click the band in the label area.

Selecting a band will trigger the following actions:  the band's background will be highlighted with a light blue. The Details panel populate a table with the source's data points. The Right Panel will display the Band's configuration options in the Selected Band Tab. 



## Band Type Configuration Options

<p align="center"><img src="./images/selected_bands_options_by_type.png" width="600"/></p>

*</p>Image 4: Selected Band Configuration Options by Band Type for (1) the Resource Bands, (2) the States Bands and (3) the Activities Bands.</p>*

#### 1-3 Common Options

These are the configuration options shared among all the bands types.

| Option         | Description                                                  |
| -------------- | ------------------------------------------------------------ |
| Label          | Defines the label displayed in the Band                      |
| Height         | Defines the height of the band.                              |
| Delete Band    | On Click, the user will have the option to remove a band from the Bands Panels. |
| Overlay        | If On, future selected sources will be displayed on top of the band. Please refer to [Overlay Bands Section](#overlay-bands) for more details. |
| Show Pin Label | If On and if the selected source a child of a pin, it will append the pin name to the band's label. |

#### 1 Activity Band Options

| Option                | Description                                                  |
| --------------------- | ------------------------------------------------------------ |
| Layout                | Defines how the activities are displayed in the Band. Options are (1) Autofit (2) Waterfall and (3) Packed. <p align="center"><img src="./images/activities_layout.png" width="600"/></p> |
| Activity Style        | Defines how the data points will be displayed. Options are (1) Bar, (2) Icon and (3) Line. <p align="center"><img src="./images/activities_style.png" width="600"/></p> |
| Horizontally Align    | Will align the label of each data point horizontally. Options are: Left and Center. |
| Vertically Align      | Will align the label of each data point vertically. Options are: Top, Bottom and Center. |
| Show Activities Times | If On, it will show at what time an activity starts and ends in the bottom of the activities.<p align="center"><img src="./images/activities_show_times.png" width="400"/></p> |
| Add To                | If On, future selected sources will add the data points to the same band. |
| Default Icon          | If Activity Style is set to Icon, the selected Icon will be the one displayed for each data point. Options: None, Plus, Cross, Circle, Triangle, Square and Diamond. |
| Show Labels           | If On, labels will be shown for each data points.            |

#### 2 Resource Band Options

| Option                 | Description                                                  |
| ---------------------- | ------------------------------------------------------------ |
| Unit Label             | Defines the unit label that is appended to the band's label. |
| Show Unit Label        | If On, the Unit label will be appended to the band's label.  |
| Auto Scale             | If On, it will calculate the y-Axis ticks based on the current range view. |
| Line Color             | Defines the color of the resource's line.                    |
| Fill                   | If On, it will fill the chart from the line to the bottom.   |
| Fill Color             | Defines the color of the resource's fill.                    |
| Interpolation          | Defines the interpolation of the chart. Options: Constant, Linear and None. |
| Log Scale              | If On, it will calculate the log for all the data points and graph them. |
| Scientific Notation    | If On, the yAxis ticks will be represented with scientific notation. |
| Show Icon              | If On, icons will be show for each data point.               |
| Default Icon           | If Show Icon is On, the selected Icon will be the one displayed for each data point. Options: None, Plus, Cross, Circle, Triangle, Square and Diamond. |
| Composite Y-Axis Label | If On and if there are two or more resource band types overlaid, then the Y-axis will works as a one. In addition, options like `Scientific Notation`, `Log Scale` and `Auto Scale` will be applied to the composite y-axis. |

#### 3 State Options

| Option                   | Description                                                  |
| ------------------------ | ------------------------------------------------------------ |
| Horizontally Align Label | Will align the label of each data point horizontally. Options are: Left and Center. |
| Vertically Align Label   | Will align the label of each data point vertically. Options are: Top, Bottom and Center. |



## Global Settings

The global settings will apply to all the bands once the options are changes.

| Option                      | Description                                                  |
| --------------------------- | ------------------------------------------------------------ |
| Label Width                 | Defines the width of the label area for each band.           |
| Add Divider Band            | On Click, a new divider band will be added.                  |
| Show Tooltip                | If On, the tooltip will be displayed. The tooltip appears in three areas: the band's label area, timeline band and for each the data point when hovering the band. |
| Default Activity Layout     | Sets the default value for the activity layout when activities are added in the future. |
| Default Label Font          | Defines the font of the label for each band.                 |
| Default Resource Color      | Sets the default value for the line color when resources are added in the future. |
| Default Resource Fill Color | Sets the default value for the fill color when resources are added in the future. |
| Default Icon                | Sets the default icon for when activities and resources are added in the future. |



## How To...

#### Overlay Bands

1. Add a band of your preference. To do so, [select a graphable source in the Source Explorer](./Raven_101_3_source_explorer.md#select-a-source).

2. Select the band.

3. In the `Selected Band` Tab in the Right Panel, set true the `Overlay` option.

4. Select another source of your preference. The band of this last selected source will be added in the same band from step one(1).

<p align="center"><img src="./images/overlaid_options.png" width="400"/></p>

*</p>Image 5: When a band is overlaid, the Selected Band Panel will add a `Selected Sub-Band` option. This will allow the user to change the properties of both overlaid sources. In the case that the user uses `Add To` instead of `Overlay` this option will not be provided, since the data points for both sources will exist in the same band.</p>*



#### 'Add' To Bands

1. Add a band of your preference. To do so, [select a graphable source in the Source Explorer](./Raven_101_3_source_explorer.md#select-a-source).
2. Select the band.
3. In the `Selected Band` Tab in the Right Panel, set true the `Add To` option.
4. Select another source of your preference. The data points of this last selected source will be added in the same band from step one(1).



#### Manipulate Time Range

There are different ways to change the view range for your bands. From the Top Bar you can execute Pan To, Zoom In/Out, Pan Left/Right, and Reset Time. From the Time Band you can brush and execute pan, and also move the view frame among the entire band's frame.

<p align="center"><img src="./images/time_management_top_bar.png" width="400"/></p>

*<p align="center">Image 6: The Top Bar contains options to update the view range such as:  (1) 'Pan To' Options, (2) Reset Time, (3) Zoom In/Out, and (4) Pan Left/Right Options.</p>*

###### Pan To

To execute `Pan`, 

1. Enter the start time in the `Pan To` input field located in the Top Bar.
2. Enter the desired `Pan Duration` for the view range. As a result, the end time will be equal to the start time + duration.
3. Click the magnifier icon (<img src="./images/search.svg" width="20" />).

###### Reset Time

	To `Reset Time`, click the Reset Clock Icon in the Top Bar  (<img src="./images/baseline-restore-24px.svg" width="20" />).

###### Zoom In/Out

	To `Zoom In`, click the Zoom In Icon in the Top Bar (<img src="./images/search-plus.svg" width="20" />).
	
	To `Zoom Out`, click the Zoom Out Icon in the Top Bar (<img src="./images/search-minus.svg" width="20" />).

###### Pan Left/Right

	To `Pan Left`, click the Left Arrow Icon in the Top Bar (<img src="./images/baseline-skip_previous-24px.svg" width="20" />).
	
	To `Pan Right`, click the Right Arrow Icon in the Top Bar (<img src="./images/baseline-skip_next-24px.svg" width="20" />).

###### Brush the timeline band

To zoom your view range from the timeline bar,

1. Click and hold in the timeline bar where you want the view range to start.

2. Move the cursor left or right defining the length of the view range that you desire.

3. Release the cursor.

<p align="center"><img src="./images/brush.png" width="400"/></p>

*</p>Image 7: **Execute Brush in the Timeline.** Top side: Click and hold at any place where you want to start or end your view range. Middle Side: Move your cursor left or right and release the cursor once the desired duration is selected. Bottom side: All the bands in the Bands Panels will update their view based on the user's selection.</p>*



#### Select a Data Point

To select a Data Point, click on the data point of your preference. When a data point is selected, the `Selected Point` Tab will display more details about the data point.  Provided data will include start time, end time, value of the data point metadata and more.

<p align="center"><img src="./images/select_data_point.png" /></p>

*</p>Image 8: **Select a Data Point.** Left side: On a band of your preference, click any data point. Right Side: Once a data point is selected, the details panel will scroll and highlight the selected data point. Also within the `Selected Point` Tab in the Right Panel, the data point's metadata will be displayed.</p>*

<p align="right"><a href="./Raven_101_4_time_cursor.md">Next: Time Cursor</a></p>
