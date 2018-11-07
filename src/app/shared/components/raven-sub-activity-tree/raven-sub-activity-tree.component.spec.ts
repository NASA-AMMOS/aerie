/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RavenSubActivityTreeComponent } from './raven-sub-activity-tree.component';
import { RavenSubActivityTreeModule } from './raven-sub-activity-tree.module';

@Component({
  selector: 'raven-sub-activity-tree-test',
  template: `
    <raven-sub-activity-tree>
    </raven-sub-activity-tree>
  `,
})
class RavenSubActivityTreeTestComponent {
  @ViewChild(RavenSubActivityTreeComponent)
  childComponent: RavenSubActivityTreeComponent;
}

describe('RavenSubActivityTreeComponent', () => {
  let component: RavenSubActivityTreeTestComponent;
  let fixture: ComponentFixture<RavenSubActivityTreeTestComponent>;
  let element: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [RavenSubActivityTreeTestComponent],
      imports: [RavenSubActivityTreeModule],
    });

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenSubActivityTreeTestComponent);
    element = fixture.nativeElement;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit an activityClicked event with an activityId when an activity in the list is clicked', () => {
    const id = '1';
    const subActivityTypeItem: HTMLElement = element.querySelector(
      `.sub-activity-type-item-${id}`,
    ) as HTMLElement;

    component.childComponent.activityClicked.subscribe((activityId: string) => {
      expect(activityId).toEqual(id);
    });

    subActivityTypeItem.click();
  });
});
