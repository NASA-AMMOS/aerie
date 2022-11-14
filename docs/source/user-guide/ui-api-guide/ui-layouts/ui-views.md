# UI Views

Users can create custom planning views for different sub-systems (e.g. science, engineering, thermal, etc.), where only data (e.g. activities and resources) for those sub-systems are visualized. This is done through custom JSON configuration files (or directly via the UI). The format of a UI View is the subject of this document.

See the [UI view JSON schema specification](https://github.com/NASA-AMMOS/aerie-gateway/blob/develop/schemas/view.json) for the complete set of view object properties and types. For more concrete examples see the [JSON view objects](https://github.com/NASA-AMMOS/aerie/tree/develop/deployment/postgres-init-db/data/ui/views) included in the [default Aerie deployment](https://github.com/NASA-AMMOS/aerie/tree/develop/deployment).

## View Schema

This is the main type interface for the planning UI view:

```ts
type ViewActivityTable = {
  columnDefs: ColumnDef[];
  columnStates: ColumnState[];
  id: number;
};

type ViewIFrame = {
  id: number;
  src: string;
  title: string;
};

type ViewDefinition = {
  plan: {
    activityTables: ViewActivityTable[];
    iFrames: ViewIFrame[];
    layout: Grid;
    timelines: Timeline[];
  };
};
```

For example, here is a JSON object that implements an empty `ViewDefinition` interface:

```json
{
  "plan": {
    "activityTables": [],
    "iFrames": [],
    "layout": {},
    "timelines": []
  }
}
```

### Layout (Grid)

A planning UI view consists of a layout which describes the different visible components and how they are arranged in re-sizeable rows and columns. The layout follows the following `Grid` type definitions:

```ts
type GridComponent = {
  activityTableId?: number;
  componentName: string;
  gridName?: string;
  iFrameId?: number;
  id: number;
  props?: any;
  timelineId?: number;
  type: "component";
};

type GridColumns = {
  columnSizes: string;
  columns: Grid[];
  gridName?: string;
  id: number;
  type: "columns";
};

type GridGutter = {
  gridName?: string;
  id: number;
  track: number;
  type: "gutter";
};

type GridRows = {
  gridName?: string;
  id: number;
  rowSizes: string;
  rows: Grid[];
  type: "rows";
};

type Grid = GridColumns | GridComponent | GridGutter | GridRows;
```

For example, here is a grid layout definition in JSON:

```json
{
  "columnSizes": "1fr 3px 2fr 3px 1fr",
  "columns": [
    { "componentName": "ActivityFormPanel", "id": 1, "type": "component" },
    { "id": 2, "track": 1, "type": "gutter" },
    {
      "id": 3,
      "rowSizes": "70% 3px 1fr",
      "rows": [
        {
          "componentName": "TimelinePanel",
          "id": 4,
          "timelineId": 0,
          "type": "component"
        },
        { "id": 5, "track": 1, "type": "gutter" },
        {
          "activityTableId": 0,
          "componentName": "ActivityTablePanel",
          "id": 6,
          "type": "component"
        }
      ],
      "type": "rows"
    },
    { "id": 7, "track": 3, "type": "gutter" },
    { "componentName": "ActivityTypesPanel", "id": 8, "type": "component" }
  ],
  "gridName": "Activities",
  "id": 0,
  "type": "columns"
}
```

### Timeline

The `timelines` section allows you to specify a list of `timeline` visualizations which display time-ordered data (i.e. activities or resources). Here is the interface of a timeline:

```ts
interface Timeline {
  id: number;
  marginLeft: number;
  marginRight: number;
  rows: Row[];
  verticalGuides: VerticalGuide[];
}
```

To visualize data in a timeline you need to add row objects to the `rows` array. A row is a layered visualization of time-ordered data. Each layer of a row is specified as an object of the `layers` array. The interfaces for a `Row` and `Layer` are as follows:

```ts
interface Row {
  autoAdjustHeight: boolean;
  expanded: boolean;
  height: number;
  horizontalGuides: HorizontalGuide[];
  id: number;
  layers: Layer[];
  name: string;
  yAxes: Axis[];
}

interface Layer {
  chartType: "activity" | "line" | "x-range";
  filter: {
    activity?: ActivityLayerFilter;
    resource?: ResourceLayerFilter;
  };
  id: number;
  yAxisId: number | null;
}
```

Here is a JSON object that creates a single row with one activity layer. Notice the `filter` property, which is a [JavaScript Regular Expression](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions) that specifies we only want to see `activity` of `type` `.*`. This is a regex for giving all activity types.

```json
{
  "autoAdjustHeight": true,
  "height": 200,
  "horizontalGuides": [],
  "id": 0,
  "layers": [
    {
      "activityColor": "#283593",
      "activityHeight": 20,
      "chartType": "activity",
      "filter": { "activity": { "type": ".*" } },
      "id": 0,
      "yAxisId": null
    }
  ],
  "yAxes": []
}
```

For data that has y-values (for example resource data), you can specify a y-axis and link a layer to it by ID. Here are the interfaces for `Axis` and `Label`:

```ts
interface Axis {
  color: string;
  id: number;
  label: Label;
  scaleDomain: (number | null)[];
  tickCount: number | null;
}

interface Label {
  align?: CanvasTextAlign;
  baseline?: CanvasTextBaseline;
  color?: string;
  fontFace?: string;
  fontSize?: number;
  hidden?: boolean;
  text: string;
}
```

Y-axes are specified in the row separately from layers so we can specify multi-way relationships between axes and layers. For example you could have many layers corresponding to a single row axis.

Here is the JSON for creating a row with two overlaid `resource` layers. The first layer shows only resources with the name `peel`, and uses the y-axis with ID `1`. The second layer shows only resources with the name `fruit`, and uses the y-axis with the ID `2`.

```json
{
  "autoAdjustHeight": false,
  "height": 100,
  "horizontalGuides": [],
  "id": 1,
  "layers": [
    {
      "chartType": "line",
      "filter": { "resource": { "name": "peel" } },
      "id": 1,
      "lineColor": "#283593",
      "lineWidth": 1,
      "pointRadius": 2,
      "yAxisId": 1
    },
    {
      "chartType": "line",
      "filter": { "resource": { "name": "fruit" } },
      "id": 2,
      "lineColor": "#ffcd69",
      "lineWidth": 1,
      "pointRadius": 2,
      "yAxisId": 2
    }
  ],
  "yAxes": [
    {
      "color": "#000000",
      "id": 1,
      "label": { "text": "peel" },
      "scaleDomain": [0, 4],
      "tickCount": 5
    },
    {
      "color": "#000000",
      "id": 2,
      "label": { "text": "fruit" },
      "scaleDomain": [-10, 4],
      "tickCount": 5
    }
  ]
}
```

### Activity Tables

Here is an example `activityTables` view JSON definition. The `columnDefs` and `columnStates` follow the schemas of the [ag-grid](https://www.ag-grid.com/) [ColDef](https://github.com/ag-grid/ag-grid/blob/c602622913d6e8600f01f7634d29b2a80a637205/community-modules/core/src/ts/entities/colDef.ts#L112) and [ColumnState](https://github.com/ag-grid/ag-grid/blob/c602622913d6e8600f01f7634d29b2a80a637205/community-modules/core/src/ts/columns/columnModel.ts#L88) respectively.

```json
{
  "activityTables": [
    {
      "columnDefs": [
        {
          "field": "id",
          "filter": "agTextColumnFilter",
          "headerName": "ID",
          "sortable": true,
          "resizable": true
        },
        {
          "field": "type",
          "filter": "agTextColumnFilter",
          "headerName": "Type",
          "sortable": true,
          "resizable": true
        },
        {
          "field": "start_time",
          "filter": "agTextColumnFilter",
          "headerName": "Start Time",
          "sortable": true,
          "resizable": true
        },
        {
          "field": "duration",
          "filter": "agTextColumnFilter",
          "headerName": "Duration",
          "sortable": true,
          "resizable": true
        }
      ],
      "columnStates": [],
      "id": 0
    }
  ]
}
```

To use the `ActivityTablePanel` you need to add an `ActivityTablePanel` component to the grid layout and connect it via the `activityTableId`. For example:

```json
{
  "activityTableId": 0,
  "componentName": "ActivityTablePanel",
  "id": 2,
  "type": "component"
}
```

Notice how we connect the grid component `activityTableId` with the `id` of the definition in the `activityTables` array.

### IFrames

An IFrame component allows you to embed another application in the Aerie UI. Here is an example `iFrames` view JSON definition:

```json
{
  "iFrames": [
    {
      "id": 0,
      "src": "https://eyes.nasa.gov/apps/solar-system",
      "title": "NASA-Eyes-Solar-System"
    }
  ]
}
```

To use the `IFrame` you need to add an `IFramePanel` component to the grid layout and connect it via the `iFrameId`. For example:

```json
{ "componentName": "IFramePanel", "iFrameId": 0, "id": 1, "type": "component" }
```

Notice how we connect the grid component `iFrameId` with the `id` of the definition in the `iFrames` array.

## GraphQL Queries

The following GraphQL queries can be used to programmatically operate on UI views.

### Create Single View

```
mutation {
  insert_view_one(
    object: {
      definition: {
        plan: { activityTables: [], iFrames: [], layout: {}, timelines: [] }
      }
      name: "My First View"
      owner: "system"
    }
  ) {
    id
  }
}
```

### Create Multiple Views

```
mutation {
  insert_view(
    objects: [
      {
        definition: {
          plan: { activityTables: [], iFrames: [], layout: {}, timelines: [] }
        }
        name: "First View"
        owner: "system"
      },
      {
        definition: {
          plan: { activityTables: [], iFrames: [], layout: {}, timelines: [] }
        }
        name: "Second View"
        owner: "system"
      }
    ]
  ) {
    returning {
      id
    }
  }
}
```

### Get All Views

```
query {
  view {
    created_at
    definition
    id
    name
    owner
    updated_at
  }
}
```

### Get Single View by ID

```
query {
  view_by_pk(id: 1) {
    created_at
    definition
    id
    name
    owner
    updated_at
  }
}
```

### Delete Single View by ID

```
mutation {
  delete_view_by_pk(id: 1) {
    id
  }
}
```

### Delete All Views in the Database

```
mutation {
  delete_view(where: {}) {
    affected_rows
  }
}
```

### Update Definition and Name of a Single View by ID

```
mutation {
  update_view_by_pk(
    pk_columns: { id: 1 }
    _set: { definition: { plan: {} }, name: "New Name" }
  ) {
    id
  }
}
```
