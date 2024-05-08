declare global {
  export type ChannelDictionary = {
    header: Header;
    id: string;
    path: string | null;
    telemetryGroups: TelemetryGroup[];
    telemetryGroupMap: TelemetryGroupMap;
    telemetries: Telemetry[];
    telemetryMap: TelemetryMap;
  };
  export type TelemetryMap = {
    [abbreviation: string]: Telemetry;
  };
  export type Telemetry = {
    abbreviation: string;
    byte_length: number;
    channel_derivation: 'None' | 'bit_extract' | 'algorithm';
    converted_on_board?: 'Yes' | 'No';
    description: string;
    group_name?: string;
    measurement_id?: number;
    name: string;
    type: string;
  };
  export type TelemetryGroup = {
    group_name: string;
    group_desc: string;
    channel_abbreviations: string[];
  };
  export type TelemetryGroupMap = {
    [group_name: string]: TelemetryGroup;
  };
  export type Header = {
    mission_name: string;
    schema_version: string;
    spacecraft_ids: number[];
    version: string;
  };
}

export {};
