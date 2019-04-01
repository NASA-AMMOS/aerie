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

import { NestAppHeaderComponent } from './nest-app-header.component';
import { NestAppHeaderModule } from './nest-app-header.module';

@Component({
  selector: 'nest-app-header-test-host',
  template: `
    <nest-app-header (menuClicked)="onMenuClicked()"></nest-app-header>
  `,
})
class NestAppHeaderTestHostComponent {
  @ViewChild(NestAppHeaderComponent)
  childComponent: NestAppHeaderComponent;

  onMenuClicked() {}
}

describe('NestAppHeaderComponent', () => {
  let component: NestAppHeaderTestHostComponent;
  let fixture: ComponentFixture<NestAppHeaderTestHostComponent>;
  let element: HTMLElement;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [NestAppHeaderTestHostComponent],
      imports: [NestAppHeaderModule],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NestAppHeaderTestHostComponent);
    element = fixture.nativeElement;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have the correct title', () => {
    const title = 'Penguins';
    component.childComponent.title = title;
    fixture.detectChanges();

    const titleElement: HTMLElement =
      element.querySelector('.top-bar-title') || new HTMLElement();

    expect(titleElement.innerText).toBe(title);
  });

  it('should call emit an event when the hamburger icon is clicked', () => {
    spyOn(component, 'onMenuClicked');

    const hamburgerButton: HTMLButtonElement | null =
      element.querySelector('.top-bar-nav-icon') || null;

    if (hamburgerButton) {
      hamburgerButton.click();
    }

    expect(component.onMenuClicked).toHaveBeenCalled();
  });
});
