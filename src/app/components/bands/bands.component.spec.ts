import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SortablejsModule } from 'angular-sortablejs';
import { BandsComponent } from './bands.component';

describe('BandsComponent', () => {
  let component: BandsComponent;
  let fixture: ComponentFixture<BandsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        BandsComponent,
      ],
      imports: [
        SortablejsModule,
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
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
