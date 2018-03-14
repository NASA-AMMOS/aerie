import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RavenResourcePointComponent } from './raven-resource-point.component';

describe('RavenDataItemComponent', () => {
  let component: RavenResourcePointComponent;
  let fixture: ComponentFixture<RavenResourcePointComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RavenResourcePointComponent ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenResourcePointComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
