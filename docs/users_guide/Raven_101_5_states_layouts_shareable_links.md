# States, Layouts and Shareable Links

When the user desires to save and recover his/her work in a future. States, layouts and shareable links provides this functionality.

## States

A state saves all the bands, bands options, view range and all the items that the user added and customized at the moment a state was saved. A user can save his/her state anywhere in the source tree where allowed to do so.

### How To...

#### Save a State

1. Select a Database to save your state.
2. Click the snowman icon (<img src="./images/baseline-more_vert-24px.svg" width="20" />).
3. In the dropdown, select: `Save`.
4. A dialog will open requesting a name for the state.
5. Click `Save`. In the selected Database the state should be added with the designated name.



#### Apply as a State

1. Look in the Source Explorer for a state of your preference.
2. Select the item, to do so click on it's name.
3. Click the snowman icon (<img src="./images/baseline-more_vert-24px.svg" width="20" />).
4. In the dropdown, select: `Apply`.
5. Another dropdown will be displayed, select `State`.
6. A dialog will appear asking for permission to remove the current state of the application. Press `Yes` to proceed. All the bands, the view range and the customized options will be restored.



#### Update a State

1. Create a new state that you want as a result of the existing state.
2. Select a Database where the state that you want to update is contained.
3. Click the snowman icon (<img src="./images/baseline-more_vert-24px.svg" width="20" />).
4. In the dropdown, select: `Save`.
5. A dialog will enter the same name of the state that you want to update. A warning of rewriting will be displayed.
6. Click `Save`. The state should be updated.



#### Remove a State

1. Look in the Source Explorer for a state of your preference.
2. Select the item, to do so click on it's name.
3. Click the snowman icon (<img src="./images/baseline-more_vert-24px.svg" width="20" />).
4. In the dropdown, select: `Delete`.
5. A dialog will appear asking for permission to proceed. Press `Yes` to continue.



## Layouts

Some of the available collections or databases share the same structure. A state can be saved as a template to apply the same options to another collections or databases.

Take for example the following tree:

```
+-- Collection-a
|	+-- source-a
|  	+-- source-b
+-- Collection-b
|	+-- source-a
|  	+-- source-b
```

both `Collection-a` and `Collection-b` contain sources with the same names. There exists a state, let's call it `example-state`, that contains the band:  `Collection-a > source-a`. If the user wants to apply the same structure to `Collection-b`, then the user can apply `example-state` as a layout to `Collection-b`. As a result the only band that will be added to the Bands Panel will be: `Collection-b > source-a`.

#### How To: Apply a state as a layout

1. Look in the Source Explorer for a state of your preference.
2. Open the nodes of the sources that you want this state to be applied as a layout.
3. Select the item, to do so click on it's name.
4. Click the snowman icon (<img src="./images/baseline-more_vert-24px.svg" width="20" />).
5. In the dropdown, select: `Apply`.
6. Another dropdown will be displayed, select `Layout`.
7. A drawer will appear in the right side of the application like the one on ***Image 1***.
8. A dropdown with states and the opened sources in the source explorer. Select the ones of your preference.
9. Click `Apply`.
10. A dialog will appear asking for permission to proceed. Press `Yes` to continue.

<img src="./images/layout_drawer.png" width="400" />

*<p align="center">Image 1: **Apply as a layout drawer**. When the user desires to apply a state as a layout, the drawer showed in the right side of the application will appear. In the Sources dropdown, the user will be able to select all the sources to which the layout is desired to be applied. </p>*



## Shareable Links

Shareable links helps with the creation of states and their access. When the user wants to share their view with another user a Shareable link can be used. 

#### How to: Get a Shareable Link

<p align="center"><img src="./images/shareable_link_dialog.png" width="500" /></p>

*<p align="center">Image 2: **Shareable link dialog**. Left side: A Shareable Link with a generated unique identifier. Right Side: A Shareable Link with custom name entered by the user.</p>*

1. Open the Main Menu in the Top Bar.
2. Select `Get Shareable Link`
3. A dialog to copy the Shareable Link will appear. 
4. By default RAVEN will create a random id to generate the Shareable Link, as shown on the left side on *Image 2*. The user also has an option to rename their shareable links, as shown on the right side on *Image 2*.

5. Once defined the shareable link, copy the content on the Shareable Link input field and the dialog will close.
6. You can open your link in your browser's tab or share it with a college.

On RAVEN's settings, a host folder for the shareable links is defined. Since a shareable link is a state, the user can access them in a future and update, delete them.

<p align="right"><a href="./Raven_101_6_export_data.md">Next: Export Data</a></p>
