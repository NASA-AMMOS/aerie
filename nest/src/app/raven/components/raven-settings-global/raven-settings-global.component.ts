/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { RavenDefaultBandSettings, RavenUpdate } from '../../models';
import { defaultColors } from '../../util';

@Component({
  selector: 'raven-settings-global',
  styleUrls: ['./raven-settings-global.component.css'],
  templateUrl: './raven-settings-global.component.html',
})
export class RavenSettingsGlobalComponent {
  @Input()
  defaultBandSettings: RavenDefaultBandSettings;

  @Output()
  updateBandsActivityFilter: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  updateDefaultBandSettings: EventEmitter<RavenUpdate> = new EventEmitter<
    RavenUpdate
  >();

  colors = defaultColors;

  heightControl: FormControl = new FormControl('', [
    Validators.required,
    Validators.min(1),
  ]);

  /**
   * Event. ActivityInitiallyHidden Change callback.
   */
  onChangeActivityInitiallyHidden(hidden: boolean) {
    this.updateDefaultBandSettings.emit({
      update: { activityInitiallyHidden: hidden },
    });
    if (hidden) {
      this.updateBandsActivityFilter.emit('.*');
    }
  }

  /**
   * Event. Change callback. Only allow label font size between the min/max font size ranges.
   */
  onLabelFontSizeChange(labelFontSize: number) {
    if (labelFontSize > 5 && labelFontSize < 31) {
      this.updateDefaultBandSettings.emit({ update: { labelFontSize } });
    }
  }

  /**
   * Helper. Invoke updateDefaultSettings if condition is true.
   */
  conditionalUpdateDefaultDividerHeight(
    condition: boolean,
    updateDict: RavenUpdate,
  ) {
    if (condition) {
      this.updateDefaultBandSettings.emit(updateDict);
    }
  }
}
