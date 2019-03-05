export interface ActivityInstance {
    activityId:     string;
    activityType:   string;
    color:          string;
    constraints:    ActivityInstanceConstraint[];
    duration:       number;
    end:            number;
    endTimestamp:   string;
    intent:         string;
    listeners?:     string[];
    name:           string;
    parameters:     ActivityInstanceParameter[];
    start:          number;
    startTimestamp: string;
    y:              number | null;
}

export interface ActivityInstanceConstraint {
    name: string;
    type: string;
}

export interface ActivityInstanceParameter {
    name: string;
    type: string;
}

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

export interface CommandDictionary {
    id:       string;
    name:     string;
    selected: boolean;
    version:  null | string;
}

export interface Command {
    name:       string;
    parameters: CommandParameter[];
    template?:  string;
}

export interface CommandParameter {
    defaultValue: string;
    help?:        string;
    max?:         number;
    min?:         number;
    name:         string;
    range:        string;
    regex?:       string;
    type:         string;
    units?:       string;
}

export interface Plan {
    adaptationId?:   string;
    endTimestamp?:   string;
    id?:             string;
    name?:           string;
    startTimestamp?: string;
}
