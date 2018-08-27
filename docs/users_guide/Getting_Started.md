# RAVEN

### 1.    About

RAVEN is a web-based application included in the SEQ subsystem of the Advanced Multimission Operations System (AMMOS) and managed by the Multimission Ground System and Services (MGSS). It allows users to view science planning, spacecraft activities, resource usage and predicted data, or any time -based data, displayed in a timeline format via web browser. Subsequently, it can be viewed simultaneously by distributed users/teams for collaboration when creating, updating and validating activity plans and command sequences. 

### 2.    Getting Stated

#### What do you need?

- [ ] MPS Server instance
- [ ] Link to a RAVEN instance
- [ ] Your Data!

#### Graph your data

To Start, you need to access RAVEN. [What I'm I looking at?](./Raven_101-application-layout.md)

The Left Panel in the application is the Source Explorer. In the Source Explorer you can navigate between your sources and select the ones that you want to visualize. 

If a node in the Source Explorer Tree, contains a graph icon (<img src="./images/chart-area.svg" width="15" />), that means that you can select that source and visualize it. [Want to learn more about the iconography?](./Raven_101_source_explorer.md#iconography)

Vualá! A new band will be added in the Bands Panel with your data.

#### Take a close look

There are different ways to see your data in more detail. To do so, you can zoom in/zoom out what you are seeing. When you added your the first band, two bands where added:

1. Timeline Band, and 
2. Your data band

in that same order. The Timeline Band will always be on the top. You can [brush](./Raven_101_3_bands.md#brush-the-timeline-band) over the Timeline Band and this will zoom your data to the selected range. You can also use the Zoom In(<img src="./images/search-plus.svg" width="15" />)/Zoom Out (<img src="./images/search-minus.svg" width="15" />) buttons at the top of the application, to [manipulate the range view](./Raven_101_3_bands.md#manipulate-time-range).

#### Deep Dive in your Data

Want to see more information about your data? Click the band's label.
What happened?

1. The Details Panel will be populated with table that displays all the Data Points in your band's source.
2. On the Right Panel, you will notice different options to change the settings of your band. To learn more on how to style your band, check out the [Band Options Section](/Raven_101_3_bands.md#band-type-specific-options).

Want to deep dive your data even more? Select any data point in your band.
What happened?

1. The Details Panel will scroll and highlight that data point, which will also be highlighted in the band.
2. On the Right Panel, go to the Selected Point tab, you will see more information about your data point.

#### Now what?
- [Pin sources in the Source Explorer](./Raven_101_source_explorer.md#pins)
- [Add a Time Cursor](./Raven_101_time_cursor.md)
- [Export your Data](./Raven_101_export_data.md)
- [Save your state](./Raven_101_states_layouts_shareable_link.md)
- [Style those bands](./Raven_101_3_bands.md#band-type-specific-options)

There is plenty to do and learn. Take a look at the [RAVEN 101 Section](./Raven_101RAVEN.md) if you want to learn more in detail how to use RAVEN. 

### License Agreement

A license agreement is a contract between an intellectual property rights owner ("licensor") and another party ("licensee"), who is allowed to use some or the IP rights in exchange for payment (a fee or royalty). Caltech will use license terms to reflect each individual situation. License agreements go through the office of technology transfer. To request a license, please visit http://download.jpl.nasa.gov

### Appendix
- [Abbreviations](https://github.jpl.nasa.gov/MPS/raven2/blob/dev/users-manual/docs/users_guide/abbreviations.md)
