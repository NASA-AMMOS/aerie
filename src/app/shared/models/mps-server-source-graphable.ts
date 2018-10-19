/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { BaseType, StringTMap } from './index';

export interface MpsServerSourceGraphable {
  __db_type: string;
  __kind: string;
  __kind_sub: string;
  contents_url: string;
  created: string;
  createdBy: string;
  customMeta: StringTMap<BaseType>[];
  data_url: string;
  filterTarget: string;
  filterSetOf: string;
  file_data_url: string;
  hasCollectionType: string;
  importJobStatus: string;
  label: string;
  modified: string;
  name: string;
  arg: string;
  filter_key: string;
  permissions: string;
}
