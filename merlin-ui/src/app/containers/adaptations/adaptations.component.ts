import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { SubSink } from 'subsink';
import { MerlinActions } from '../../actions';
import { AppState } from '../../app-store';
import { getAdaptations } from '../../selectors';
import { CAdaptation, SCreateAdaption } from '../../types';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-adaptations',
  styleUrls: ['./adaptations.component.css'],
  templateUrl: './adaptations.component.html',
})
export class AdaptationsComponent implements OnDestroy {
  adaptations: CAdaptation[] | null = null;
  createAdaptationForm: FormGroup;

  private subs = new SubSink();

  constructor(
    private fb: FormBuilder,
    private ref: ChangeDetectorRef,
    private router: Router,
    private store: Store<AppState>,
  ) {
    this.createAdaptationForm = this.fb.group({
      file: [null, Validators.required],
      mission: ['', Validators.required],
      name: ['', Validators.required],
      owner: ['', Validators.required],
      version: ['', Validators.required],
    });

    this.subs.add(
      this.store.pipe(select(getAdaptations)).subscribe(adaptations => {
        this.adaptations = adaptations;
        this.ref.markForCheck();
      }),
    );
  }

  ngOnDestroy() {
    this.subs.unsubscribe();
  }

  onCreatePlan(adaptationId: string): void {
    this.router.navigate(['plans'], { state: { adaptationId } });
  }

  onDeleteAdaptation(id: string) {
    this.store.dispatch(MerlinActions.deleteAdaptation({ id }));
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files.length) {
      const file = input.files.item(0);
      this.createAdaptationForm.patchValue({
        file,
      });
    }
  }

  onSubmit() {
    if (this.createAdaptationForm.valid) {
      const adaptation: SCreateAdaption = {
        ...this.createAdaptationForm.value,
      };
      this.store.dispatch(MerlinActions.createAdaptation({ adaptation }));
    }
  }
}
