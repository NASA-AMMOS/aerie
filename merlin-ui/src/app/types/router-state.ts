import { Params } from '@angular/router';

export interface RouterState {
  params: Params;
  path: string;
  queryParams: Params;
  url: string;
}
