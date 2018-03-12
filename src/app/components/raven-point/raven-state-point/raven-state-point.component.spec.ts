import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RavenStatePointComponent } from './raven-state-point.component';

describe('RavenStatePointComponent', () => {
  let component: RavenStatePointComponent;
  let fixture: ComponentFixture<RavenStatePointComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RavenStatePointComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenStatePointComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
