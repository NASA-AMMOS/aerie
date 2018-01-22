import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BandsComponent } from './bands.component';

describe('BandsComponent', () => {
  let component: BandsComponent;
  let fixture: ComponentFixture<BandsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ BandsComponent ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BandsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
