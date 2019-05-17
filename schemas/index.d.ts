export interface ActivityType {
    activityClass: string;
    listeners:     string[];
    parameters:    ActivityTypeParameter[];
    typeName:      string;
}

export interface ActivityTypeParameter {
    name: string;
    type: string;
}

export interface Adaptation {
    id:       string;
    location: string;
    mission:  string;
    name:     string;
    owner:    string;
    version:  string;
}

export interface AMQPMessage {
    data?:        { [key: string]: any };
    messageType?: AMQPMessageTypeEnum;
}

export enum AMQPMessageTypeEnum {
    LoadAdaptation = "LoadAdaptation",
    SimulateActivity = "SimulateActivity",
    UnloadAdaptation = "UnloadAdaptation",
}

export interface CommandDictionary {
    id:      string;
    name:    string;
    version: null | string;
}

export interface MpsCommand {
    name:       string;
    parameters: MpsCommandParameter[];
}

export interface MpsCommandParameter {
    defaultValue?: string;
    help?:         string;
    name?:         string;
    range?:        string;
    type?:         string;
    units?:        string;
}

/**
 * A collection of Activity Instances, optionally pre-arranged to form a schedule.
 */
export interface Plan {
    /**
     * List of activity instances that comprise this plan
     */
    activityInstances?: ActivityInstance[];
    /**
     * The ID of the associated adaptation
     */
    adaptationId: string;
    /**
     * When the plan ends. Will be changed to a Unix timestamp.
     */
    endTimestamp: string;
    /**
     * ID of the plan. Currently a stringified MongoDB object ID.
     */
    id: string;
    /**
     * Name of the plan
     */
    name: string;
    /**
     * When the plan ends. Will be changed to a Unix timestamp.
     */
    startTimestamp: string;
}

export interface ActivityInstance {
    /**
     * ID of the activity instance
     */
    activityId: string;
    /**
     * The name of the activity type that this instance is based on
     */
    activityType: string;
    /**
     * Background color of the activity instance within an activity band
     */
    backgroundColor: string;
    /**
     * List of constraints associated with the activity instance
     */
    constraints: ActivityInstanceConstraint[];
    /**
     * How long the activity instance lasts
     */
    duration: number;
    /**
     * When the activity instance ends, as a Unix timestamp
     */
    end: number;
    /**
     * When the activity instances ends, as an ISO 8601 formatted date string
     */
    endTimestamp: string;
    /**
     * Description of the activity instance
     */
    intent: string;
    /**
     * A list of listeners
     */
    listeners?: string[];
    /**
     * Name of the activity instance
     */
    name: string;
    /**
     * Parameters which augment the runtime behavior of the instance
     */
    parameters: ActivityInstanceParameter[];
    /**
     * When the activity instance starts, as a Unix timestamp
     */
    start: number;
    /**
     * When the activity instances starts, as an ISO 8601 formatted date string
     */
    startTimestamp: string;
    /**
     * Text color of the activity instance within an activity band
     */
    textColor: string;
    /**
     * The y position of the activity instance within an activity band
     */
    y: number | null;
}

export interface ActivityInstanceConstraint {
    /**
     * Name of the constraint
     */
    name: string;
    /**
     * Type of the constraint
     */
    type: string;
}

export interface ActivityInstanceParameter {
    /**
     * Default value of the parameter
     */
    defaultValue?: string;
    /**
     * Name of the parameter
     */
    name: string;
    /**
     * A range of values, for instance min/max and enum
     */
    range?: string[];
    /**
     * The type of this parameter
     */
    type: string;
    /**
     * The value of the parameter
     */
    value: string;
}

export interface Schedule {
    id?: string;
}
