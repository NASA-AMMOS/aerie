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

export interface Plan {
    adaptationId:   string;
    endTimestamp:   string;
    id:             string;
    name:           string;
    startTimestamp: string;
}
