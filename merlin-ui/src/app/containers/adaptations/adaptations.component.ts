import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
} from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { select, Store } from '@ngrx/store';
import { SubSink } from 'subsink';
import { MerlinActions } from '../../actions';
import { AppState } from '../../app-store';
import { getAdaptations, getLoading } from '../../selectors';
import { CAdaptation, SCreateAdaption } from '../../types';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-adaptations',
  styleUrls: ['./adaptations.component.css'],
  templateUrl: './adaptations.component.html',
})
export class AdaptationsComponent implements OnDestroy {
  adaptations: CAdaptation[] = [];
  createAdaptationForm: FormGroup;
  displayedColumns: string[] = [
    'menu',
    'id',
    'name',
    'version',
    'mission',
    'owner',
  ];
  loading = false;

  private subs = new SubSink();

  constructor(
    private fb: FormBuilder,
    private ref: ChangeDetectorRef,
    private store: Store<AppState>,
  ) {
    this.createAdaptationForm = this.fb.group({
      name: new FormControl('', [Validators.required]),
      version: new FormControl('', [Validators.required]),
      mission: new FormControl('', [Validators.required]),
      owner: new FormControl('', [Validators.required]),
      file: new FormControl(null, [Validators.required]),
    });

    this.subs.add(
      this.store.pipe(select(getAdaptations)).subscribe(adaptations => {
        this.adaptations = adaptations;
        this.ref.markForCheck();
      }),
      this.store.pipe(select(getLoading)).subscribe(loading => {
        this.loading = loading;
        this.ref.markForCheck();
      }),
    );
  }

  ngOnDestroy() {
    this.subs.unsubscribe();
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
