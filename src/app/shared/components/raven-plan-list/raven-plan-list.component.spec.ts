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

import { RavenPlan } from '../../models/raven-plan';
import { PlanMockService } from '../../services/plan-mock.service';
import { RavenPlanListComponent } from './raven-plan-list.component';
import { RavenPlanListModule } from './raven-plan-list.module';

@Component({
  selector: 'raven-plan-list-test',
  template: `
    <raven-plan-list
      (closed)="close()"
      (createPlanClicked)="create()"
      (deletePlanClicked)="remove($event)"
      (opened)="open()"
      (selectPlanClicked)="select($event)"
      (updatePlanClicked)="update($event)"
      [expanded]="expanded"
      [plans]="plans"
      [selectedPlan]="selected"
      [title]="title">
    </raven-plan-list>
  `,
})
class RavenPlanListTestComponent {
  expanded = true;
  plans: RavenPlan[] = PlanMockService.getMockData();
  selected: RavenPlan = { ...this.plans[0] };
  title = 'Foo';

  @ViewChild(RavenPlanListComponent)
  component: RavenPlanListComponent;

  close() {}
  create() {}
  open() {}
  remove(id: string) {}
  select(id: string) {}
  update(id: string) {}
}

describe('RavenPlanListComponent', () => {
  let component: RavenPlanListTestComponent;
  let fixture: ComponentFixture<RavenPlanListTestComponent>;
  let element: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [RavenPlanListTestComponent],
      imports: [RavenPlanListModule, NoopAnimationsModule],
    });

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenPlanListTestComponent);
    element = fixture.nativeElement;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit the createPlanClicked event', () => {
    spyOn(component, 'create');
    component.component.onClickCreate();
    expect(component.create).toHaveBeenCalled();
  });

  it('should emit the deletePlanClicked event', () => {
    spyOn(component, 'remove').and.callThrough();
    component.component.onClickDelete(new Event('click'), 'test0');
    expect(component.remove).toHaveBeenCalledWith('test0');
  });

  it('should emit the updatePlanClicked event', () => {
    spyOn(component, 'update').and.callThrough();
    component.component.onClickUpdate(new Event('click'), 'test0');
    expect(component.update).toHaveBeenCalledWith('test0');
  });

  it('should emit the selectPlanClicked event', () => {
    spyOn(component, 'select').and.callThrough();
    component.component.onClickSelect('test0');
    expect(component.select).toHaveBeenCalledWith('test0');
  });

  it('should emit the opened events', () => {
    component.expanded = false;
    fixture.detectChanges();
    spyOn(component, 'open').and.callThrough();
    component.component.panel.open();
    expect(component.open).toHaveBeenCalled();
  });

  it('should emit the closed event', () => {
    spyOn(component, 'close').and.callThrough();
    component.component.panel.close();
    expect(component.close).toHaveBeenCalled();
  });

  it('should set the expanded state', () => {
    component.expanded = false;
    fixture.detectChanges();
    spyOn(component, 'open').and.callThrough();
    component.component.panel.toggle();
    expect(component.open).toHaveBeenCalled();
  });

  it('should set the title', () => {
    component.title = 'Funky Donkey Man';
    fixture.detectChanges();

    const header: HTMLElement =
      element.querySelector('mat-expansion-panel-header > .mat-content') ||
      document.createElement('i');
    expect(header.innerHTML.trim()).toBe(component.title);
  });
});
