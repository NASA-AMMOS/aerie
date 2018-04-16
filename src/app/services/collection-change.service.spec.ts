import { TestBed, inject } from '@angular/core/testing';

import { CollectionChangeService } from './collection-change.service';

describe('CollectionChangeService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CollectionChangeService]
    });
  });

  it('should be created', inject([CollectionChangeService], (service: CollectionChangeService) => {
    expect(service).toBeTruthy();
  }));
});
