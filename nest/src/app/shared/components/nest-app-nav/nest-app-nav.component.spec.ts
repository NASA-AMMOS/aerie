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
import { NestAppNavComponent } from './nest-app-nav.component';
import { NestAppNavModule } from './nest-app-nav.module';

@Component({
  selector: 'nest-app-header-test-host',
  template: `
    <nest-app-nav></nest-app-nav>
  `,
})
class NestAppNavTestHostComponent {
  @ViewChild(NestAppNavComponent)
  childComponent: NestAppNavComponent;
}

@Component({
  selector: 'blank-component',
  template: '',
})
class BlankComponent {}

describe('NestAppNavComponent', () => {
  let component: NestAppNavTestHostComponent;
  let fixture: ComponentFixture<NestAppNavTestHostComponent>;
  let element: HTMLElement;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [NestAppNavTestHostComponent, BlankComponent],
      imports: [
        NestAppNavModule,
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
    fixture = TestBed.createComponent(NestAppNavTestHostComponent);
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
      version: '0.4.2',
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

  it('should emit an aboutClicked event when the About mat-list-item is clicked', () => {
    const aboutNavListItem: HTMLElement =
      element.querySelector('.nest-app-nav-about-button') || new HTMLElement();

    component.childComponent.aboutClicked.subscribe((event: MouseEvent) =>
      expect(event.type).toEqual('click'),
    );

    aboutNavListItem.click();
  });
});
