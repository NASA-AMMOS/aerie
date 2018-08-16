/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RavenCompositeBandComponent } from './raven-composite-band.component';
import { RavenCompositeBandModule } from './raven-composite-band.module';

describe('RavenCompositeBandComponent', () => {
  let component: RavenCompositeBandComponent;
  let fixture: ComponentFixture<RavenCompositeBandComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RavenCompositeBandModule,
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenCompositeBandComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });

  it('should default compositeAutoScale to be false', () => {
    expect(component.compositeAutoScale).toBe(false);
  });

  it('should default compositeLogTicks to be false', () => {
    expect(component.compositeLogTicks).toBe(false);
  });

  it('should default compositeScientificNotation to be false', () => {
    expect(component.compositeScientificNotation).toBe(false);
  });

  it('should default compositeYAxisLabel to be false', () => {
    expect(component.compositeYAxisLabel).toBe(false);
  });

  it('should default cursorColor to be #ff0000', () => {
    expect(component.cursorColor).toBe('#ff0000');
  });

  it('should default cursorTime to be null', () => {
    expect(component.cursorTime).toBe(null);
  });

  it('should default cursorWidth to be 1', () => {
    expect(component.cursorWidth).toBe(1);
  });

  it('should default dayCode to be ""', () => {
    expect(component.dayCode).toBe('');
  });

  it('should default earthSecToEpochSec to be 1', () => {
    expect(component.earthSecToEpochSec).toBe(1);
  });

  it('should default epoch to be null', () => {
    expect(component.epoch).toBe(null);
  });

  it('should default height to be 100', () => {
    expect(component.height).toBe(100);
  });

  it('should default heightPadding to be 0', () => {
    expect(component.heightPadding).toBe(0);
  });

  it('should default id to be ""', () => {
    expect(component.id).toBe('');
  });

  it('should default labelFontSize to be 9', () => {
    expect(component.labelFontSize).toBe(9);
  });

  it('should default labelWidth to be 150', () => {
    expect(component.labelWidth).toBe(150);
  });

  it('should default maxTimeRange to be { end: 0, start: 0 }', () => {
    expect(component.maxTimeRange).toEqual({ end: 0, start: 0 });
  });

  it('should default selectedPoint to be null', () => {
    expect(component.selectedPoint).toBe(null);
  });

  it('should default showTooltip to be true', () => {
    expect(component.showTooltip).toBe(true);
  });

  it('should default subBands to be []', () => {
    expect(component.subBands).toEqual([]);
  });

  it('should default viewTimeRange to be { end: 0, start: 0 }', () => {
    expect(component.viewTimeRange).toEqual({ end: 0, start: 0 });
  });
});
