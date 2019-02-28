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

export interface Activity {
    activityId:     string;
    activityType:   string;
    color:          string;
    constraints:    ActivityConstraint[];
    duration:       number;
    end:            number;
    endTimestamp:   string;
    intent:         string;
    name:           string;
    parameters:     ActivityParameter[];
    start:          number;
    startTimestamp: string;
    y:              number | null;
}

export interface ActivityConstraint {
    name: string;
    type: string;
}

export interface ActivityParameter {
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
    adaptationId:   string;
    endTimestamp:   string;
    id:             string;
    name:           string;
    startTimestamp: string;
}
