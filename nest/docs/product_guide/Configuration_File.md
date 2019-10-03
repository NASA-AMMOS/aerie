# Configuration File

[This Configuration File](../../src/config.ts) is used to customize the setup of our products. It defines the URLs that hook our application with services that provide data. In addition, for each of our products there is a custom configuration that defines other default values based on user preference (e.g. default resource band color). The following is the interface of the configuration file:

```typescript
export interface ConfigState {
  app: {
    baseUrl: string;
    branch: string;
    commit: string;
    production: boolean;
    version: string;
  };
  mpsServer: {
    apiUrl: string;
    ravenConfigUrl: string;
    epochsUrl: string;
    ravenUrl: string;
    socketUrl: string;
  };
  raven: {
    defaultBandSettings: RavenDefaultBandSettings;
    itarMessage: string;
    shareableLinkStatesUrl: string;
  };
}
```

### MPS Server Default Settings

| Property      | Default |
|---------------|--------------|
| apiUrl        |  mpsserver/api/v2/fs |
| epochsUrl     |  mpsserver/api/v2/fs-mongodb/leucadia/taifunTest/europaEpoch.csv |
| ravenConfigUrl|  mpsserver/api/v2/raven_config_file |
| ravenUrl      |  mpsserver/raven |
| socketUrl     |  mpsserver/websocket/v1/topic/main |

<TODO Sequencing Default Setting>

### Raven Default Settings

| Property      | Default       | Description | 
|---------------|---------------|--------------|
| itarMessage   |  NONE         | If defined, Raven will display its content in the application at all times. 
| shareableLinkStatesUrl  |  TEST_ATS/STATES | Defaults the location where shareable links will be stored in the Source Explorer. 
| defaultBandSettings   |  See the `Default Band Settings` section below.    |  

#### Default Band Settings

```typescript
export interface RavenDefaultBandSettings {
  activityLayout: number;
  icon: string;
  iconEnabled: boolean;
  labelFont: string;
  labelFontSize: number;
  labelWidth: number;
  resourceColor: string;
  resourceFillColor: string;
  showTooltip: boolean;
}
```

| Property      | Default | Description |
|---------------|--------------| ---------| 
| activityLayout| 0 | Defines the default value for the activity layout when activities are added in the future. Options: 0 (Auto-Fit), 1 (Packed), 2 (Waterfall), 
| icon          | circle | Defines the icon displayed when icon is set to true. Options: Circle, Cross, Diamond, None, Plus, Square and Triangle |
| iconEnabled   | false | If true, it will display the icons, otherwise it will. | 
| labelFont     | Georgia | Defines the font of the label for each band. Options: Arial, Helvetica, Times New Roman, Courier and Georgia.
| labelFontSize | 9 | Defines the font size. 
| labelWidth    | 150 | Defines the width of the label area for each band.  
| resourceColor | #000000 | Sets the default value for the line color when resources are added in the future.
| resourceFillColor | #000000 | Sets the default value for the fill color when resources are added in the future.
| showTooltip   | true | If True, the tooltip will be displayed. The tooltip appears in three areas: the band's label area, timeline band and for each the data point when hovering the band. 
