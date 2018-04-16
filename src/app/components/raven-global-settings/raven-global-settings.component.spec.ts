import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RavenGlobalSettingsComponent } from './raven-global-settings.component';

describe('RavenGlobalSettingsComponent', () => {
  let component: RavenGlobalSettingsComponent;
  let fixture: ComponentFixture<RavenGlobalSettingsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RavenGlobalSettingsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenGlobalSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
