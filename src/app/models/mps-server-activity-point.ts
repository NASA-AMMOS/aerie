/*
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { MpsServerActivityPointMetadata } from './mps-server-activity-point-metadata';
import { MpsServerActivityPointParameter } from './mps-server-activity-point-parameter';
import { MpsServerAnnotation } from './mps-server-annotation';

export interface MpsServerActivityPoint {
  __document_id: string;
  __file_id: string;
  __kind: string;
  ancestors: string[];
  childrenUrl: string;
  descendantsUrl: string;
  'Activity ID': string;
  'Activity Name': string;
  'Activity Parameters': MpsServerActivityPointParameter[];
  'Activity Type': string;
  'Annotations': MpsServerAnnotation[];
  'Metadata': MpsServerActivityPointMetadata[];
  'Tend Assigned': string;
  'Tstart Assigned': string;
}
