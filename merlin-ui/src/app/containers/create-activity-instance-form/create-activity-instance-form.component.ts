import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
} from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { SubSink } from 'subsink';
import { MerlinActions } from '../../actions';
import { AppState } from '../../app-store';
import { getActivityTypes, getActivityTypesMap } from '../../selectors';
import {
  CActivityType,
  CActivityTypeMap,
  SActivityInstance,
} from '../../types';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-create-activity-instance-form',
  styles: [''],
  templateUrl: './create-activity-instance-form.component.html',
})
export class CreateActivityInstanceFormComponent implements OnDestroy {
  activityTypes: CActivityType[] = [];
  activityTypesMap: CActivityTypeMap | null = null;
  form: FormGroup;

  private subs = new SubSink();

  constructor(
    private fb: FormBuilder,
    private ref: ChangeDetectorRef,
    private route: ActivatedRoute,
    private store: Store<AppState>,
  ) {
    this.form = this.fb.group({
      parameters: this.fb.array([]),
      startTimestamp: ['', Validators.required],
      type: ['', Validators.required],
    });
    const { controls } = this.form;

    this.subs.add(
      controls.type.valueChanges.subscribe(type => {
        if (this.activityTypesMap) {
          const { parameters } = this.activityTypesMap[type];
          this.formParameters.clear();
          parameters.forEach(parameter => {
            this.formParameters.push(
              this.fb.group({
                name: parameter.name,
                type: parameter.type,
                value: '',
              }),
            );
          });
        }
      }),
      this.store.pipe(select(getActivityTypes)).subscribe(activityTypes => {
        this.activityTypes = activityTypes;
        this.ref.markForCheck();
      }),
      this.store
        .pipe(select(getActivityTypesMap))
        .subscribe(activityTypesMap => {
          this.activityTypesMap = activityTypesMap;
          this.ref.markForCheck();
        }),
    );
  }

  ngOnDestroy() {
    this.subs.unsubscribe();
  }

  get formParameters() {
    return this.form.get('parameters') as FormArray;
  }

  onSubmit() {
    if (this.form.valid) {
      const { id: planId } = this.route.snapshot.params;
      const parameters = this.form.value.parameters.reduce(
        (parameterMap, parameter) => {
          if (parameter.value !== '') {
            if (parameter.type === 'double') {
              parameterMap[parameter.name] = parseFloat(parameter.value);
            } else {
              parameterMap[parameter.name] = parameter.value;
            }
          }
          return parameterMap;
        },
        {},
      );
      const activityInstance: SActivityInstance = {
        ...this.form.value,
        parameters,
      };
      this.store.dispatch(
        MerlinActions.createActivityInstance({ planId, activityInstance }),
      );
    }
  }
}
