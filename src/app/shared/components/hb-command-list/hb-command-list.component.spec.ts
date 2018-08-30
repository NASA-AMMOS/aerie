import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { HBCommandListComponent } from './hb-command-list.component';
import { HBCommandListModule } from './hb-command-list.module';

describe('HBCommandListComponent', () => {
  let component: HBCommandListComponent;
  let fixture: ComponentFixture<HBCommandListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HBCommandListModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HBCommandListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
