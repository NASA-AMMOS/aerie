import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RavenActivityPointComponent } from './raven-activity-point.component';

describe('RavenActivityPointComponent', () => {
  let component: RavenActivityPointComponent;
  let fixture: ComponentFixture<RavenActivityPointComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RavenActivityPointComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenActivityPointComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
