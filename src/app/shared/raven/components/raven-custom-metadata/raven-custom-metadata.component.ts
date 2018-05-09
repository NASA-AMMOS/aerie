/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';

import {
  RavenToKeyValueArrayPipe,
} from './../../pipes';

import {
  StringTMap,
} from './../../../models';

@Component({
  selector: 'raven-custom-metadata',
  styleUrls: ['./raven-custom-metadata.component.css'],
  templateUrl: './raven-custom-metadata.component.html',
})
export class RavenCustomMetadataComponent implements OnChanges {
  @Input() customMetadata: StringTMap<any>;

  metadataObjects: any[];
  metadataValues: any[];

  private toKeyValueArray = new RavenToKeyValueArrayPipe().transform;

  ngOnChanges(changes: SimpleChanges) {
    if (changes.customMetadata) {
      this.setMetadataObjectsAndValues();
    }
  }

  /**
   * Helper. Builds two arrays of metadata: objects, and values.
   * Objects are rendered recursively with another `raven-custom-metadata` component.
   * Values are rendered in a table in the current component.
   */
  setMetadataObjectsAndValues() {
    this.metadataObjects = [];
    this.metadataValues = [];

    this.toKeyValueArray(this.customMetadata).forEach(meta => {
      if (typeof meta.value === 'object') {
        this.metadataObjects.push(meta);
      } else {
        this.metadataValues.push(meta);
      }
    });
  }
}
