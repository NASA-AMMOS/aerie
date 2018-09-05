/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { async, TestBed } from '@angular/core/testing';
import { keyBy } from 'lodash';
import { bands } from '../../mocks';
import { RavenStateBand, RavenUpdate } from '../../models';
import { RavenSettingsBandsComponent } from './raven-settings-bands.component';
import { RavenSettingsBandsModule } from './raven-settings-bands.module';

describe('RavenSettingsBandsComponent', () => {
  let component: RavenSettingsBandsComponent;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [RavenSettingsBandsModule],
    }).compileComponents();
  }));

  beforeEach(() => {
    component = new RavenSettingsBandsComponent();
    component.bandsById = keyBy(bands, 'id');
    component.selectedBandId = '100';
    component.selectedSubBandId = '0';
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });

  it('should update subBand (height to 100 and heightPadding to 10) and band (height to 100 and heightPadding to 20) when plotType isNumeric', () => {
    let bandUpdate: any;
    let subBandUpdate: any;
    component.updateBand.subscribe(
      (emit: RavenUpdate) => (bandUpdate = emit.update)
    );
    component.updateSubBand.subscribe(
      (emit: RavenUpdate) => (subBandUpdate = emit)
    );
    component.onChangePlotType(bands[0].subBands[0], true);
    expect(bandUpdate.height).toEqual(100);
    expect(bandUpdate.heightPadding).toEqual(20);
    expect(subBandUpdate.bandId).toBe('100');
    expect(subBandUpdate.subBandId).toBe('0');
    expect(subBandUpdate.update.height).toEqual(100);
    expect(subBandUpdate.update.heightPadding).toEqual(10);
    expect(subBandUpdate.update.isNumeric).toBe(true);
  });

  it('should update subBand and band (height to 50 and heightPadding to 0) when plotType !isNumeric and !showStateChnageTimes', () => {
    let bandUpdate: any;
    let subBandUpdate: any;
    component.updateBand.subscribe(
      (emit: RavenUpdate) => (bandUpdate = emit.update)
    );
    component.updateSubBand.subscribe(
      (emit: RavenUpdate) => (subBandUpdate = emit.update)
    );
    component.onChangePlotType(bands[0].subBands[0], false);
    expect(bandUpdate.height).toEqual(50);
    expect(bandUpdate.heightPadding).toEqual(0);
    expect(subBandUpdate.height).toEqual(50);
    expect(subBandUpdate.heightPadding).toEqual(0);
    expect(subBandUpdate.isNumeric).toBe(false);
  });

  it('should update subBand (height to 50 and heightPadding to 10) and band (height to 50 and heightPadding to 0) when plotType !isNumeric and showStateChnageTimes', () => {
    let bandUpdate: any;
    let subBandUpdate: any;
    component.updateBand.subscribe(
      (emit: RavenUpdate) => (bandUpdate = emit.update)
    );
    component.updateSubBand.subscribe(
      (emit: RavenUpdate) => (subBandUpdate = emit.update)
    );
    (bands[1].subBands[0] as RavenStateBand).showStateChangeTimes = true;
    component.onChangePlotType(bands[1].subBands[0], false);
    expect(bandUpdate.height).toEqual(50);
    expect(bandUpdate.heightPadding).toEqual(0);
    expect(subBandUpdate.height).toEqual(50);
    expect(subBandUpdate.heightPadding).toEqual(10);
    expect(subBandUpdate.isNumeric).toBe(false);
  });

  it('should update subBand heightPadding to 12 when showStateChangeTimes is true', () => {
    let subBandUpdate: any;
    component.updateSubBand.subscribe(
      (emit: RavenUpdate) => (subBandUpdate = emit.update)
    );
    component.onChangeShowStateChangeTimes(bands[0].subBands[0], true);
    expect(subBandUpdate.heightPadding).toEqual(12);
    expect(subBandUpdate.showStateChangeTimes).toBe(true);
  });

  it('should update subBand heightPadding to 0 when showStateChangeTimes is false', () => {
    let subBandUpdate: any;
    component.updateSubBand.subscribe(
      (emit: RavenUpdate) => (subBandUpdate = emit.update)
    );
    component.onChangeShowStateChangeTimes(bands[0].subBands[0], false);
    expect(subBandUpdate.heightPadding).toEqual(0);
    expect(subBandUpdate.showStateChangeTimes).toBe(false);
  });
});
