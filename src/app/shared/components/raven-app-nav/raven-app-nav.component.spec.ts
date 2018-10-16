/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, ViewChild } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { NestModule } from '../../models';
import {
  COLLAPSED_WIDTH,
  OPENED_WIDTH,
  RavenAppNavComponent,
} from './raven-app-nav.component';
import { RavenAppNavModule } from './raven-app-nav.module';

@Component({
  selector: 'raven-app-header-test-host',
  template: `<raven-app-nav></raven-app-nav>`,
})
class RavenAppNavTestHostComponent {
  @ViewChild(RavenAppNavComponent)
  childComponent: RavenAppNavComponent;
}

@Component({
  selector: 'blank-component',
  template: '',
})
class BlankComponent {}

describe('RavenAppNavComponent', () => {
  let component: RavenAppNavTestHostComponent;
  let fixture: ComponentFixture<RavenAppNavTestHostComponent>;
  let element: HTMLElement;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [RavenAppNavTestHostComponent, BlankComponent],
      imports: [
        RavenAppNavModule,
        /**
         * Set up a route that corresponds to the route that will be used in a test
         * @see https://angular.io/api/router/testing/RouterTestingModule
         */
        RouterTestingModule.withRoutes([
          { path: 'frankenstein', component: BlankComponent },
        ]),
      ],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenAppNavTestHostComponent);
    element = fixture.nativeElement;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render each module as a list item', () => {
    const module: NestModule = {
      icon: 'face',
      path: 'frankenstein',
      title: 'flim-flam',
    };

    component.childComponent.modules = [module];
    fixture.detectChanges();

    const listItem: HTMLElement =
      element.querySelector('.mat-list-item') || new HTMLElement();

    const icon: HTMLElement =
      listItem.querySelector('.mat-icon') || new HTMLElement();

    const text: HTMLElement =
      listItem.querySelector('.mat-line') || new HTMLElement();

    expect(listItem.getAttribute('href')).toBe(`/${module.path}`);
    expect(icon.innerText).toBe(module.icon);
    expect(text.innerText).toBe(module.title);
  });

  it('should hide the labels', () => {
    const navList: HTMLElement =
      element.querySelector('.mat-nav-list') || new HTMLElement();

    expect(navList.clientWidth).toBe(OPENED_WIDTH as number);

    component.childComponent.iconsOnly = true;
    fixture.detectChanges();

    expect(navList.clientWidth).toBe(COLLAPSED_WIDTH as number);
  });
});
