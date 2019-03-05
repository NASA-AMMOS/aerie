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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ActivityType } from '../../../../../../schemas/types/ts';
import { StringTMap } from '../../models/map';
import { getMockActivityTypes } from '../../services/adaptation-mock.service';
import { RavenActivityTypeListComponent } from './raven-activity-type-list.component';
import { RavenActivityTypeListModule } from './raven-activity-type-list.module';

@Component({
  selector: 'raven-activity-type-list-test',
  template: `
    <raven-activity-type-list
      [activityTypes]="activityTypesList"
      (createActivityTypeClicked)="create()"
      (deleteActivityTypeClicked)="remove($event)"
      (updateActivityTypeClicked)="update($event)"
      (selectActivityTypeClicked)="select($event)"
    >
    </raven-activity-type-list>
  `,
})
class RavenActivityTypeListTestComponent {
  activityTypes: StringTMap<ActivityType> = getMockActivityTypes();
  activityTypesList: ActivityType[] = Object.values(this.activityTypes);

  @ViewChild(RavenActivityTypeListComponent)
  component: RavenActivityTypeListComponent;

  create() {}
  remove(id: string) {}
  update(id: string) {}
  select(id: string) {}
}

describe('RavenActivityTypeListComponent', () => {
  let component: RavenActivityTypeListTestComponent;
  let fixture: ComponentFixture<RavenActivityTypeListTestComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [RavenActivityTypeListTestComponent],
      imports: [RavenActivityTypeListModule, NoopAnimationsModule],
    });

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenActivityTypeListTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit the createActivityTypeClicked event', () => {
    spyOn(component, 'create');
    component.component.onClickCreate();
    expect(component.create).toHaveBeenCalled();
  });

  it('should emit the deleteActivityTypeClicked event', () => {
    spyOn(component, 'remove').and.callThrough();
    component.component.onClickDelete(new Event('click'), 'test0');
    expect(component.remove).toHaveBeenCalledWith('test0');
  });

  it('should emit the updateActivityTypeClicked event', () => {
    spyOn(component, 'update').and.callThrough();
    component.component.onClickUpdate(new Event('click'), 'test0');
    expect(component.update).toHaveBeenCalledWith('test0');
  });

  it('should emit the selectActivityTypeClicked event', () => {
    spyOn(component, 'select').and.callThrough();
    component.component.onClickSelect('test0');
    expect(component.select).toHaveBeenCalledWith('test0');
  });
});
