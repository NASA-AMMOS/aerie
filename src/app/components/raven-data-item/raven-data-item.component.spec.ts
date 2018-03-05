import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RavenDataItemComponent } from './raven-data-item.component';

describe('RavenDataItemComponent', () => {
  let component: RavenDataItemComponent;
  let fixture: ComponentFixture<RavenDataItemComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RavenDataItemComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenDataItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
