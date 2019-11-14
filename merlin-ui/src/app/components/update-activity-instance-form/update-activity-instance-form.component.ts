import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  CActivityInstance,
  CActivityTypeMap,
  SActivityInstance,
} from '../../types';

export interface UpdateActivityInstance {
  activityInstance: Partial<SActivityInstance>;
  activityInstanceId: string;
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-update-activity-instance-form',
  styles: [''],
  templateUrl: './update-activity-instance-form.component.html',
})
export class UpdateActivityInstanceFormComponent implements OnChanges {
  @Input()
  activityInstance: CActivityInstance;

  @Input()
  activityTypesMap: CActivityTypeMap | null = null;

  @Output()
  delete: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  update: EventEmitter<UpdateActivityInstance> = new EventEmitter<
    UpdateActivityInstance
  >();

  form: FormGroup;

  constructor(private fb: FormBuilder) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.activityInstance) {
      this.form = this.fb.group({
        parameters: this.fb.array([]),
        startTimestamp: [
          this.activityInstance.startTimestamp,
          Validators.required,
        ],
        type: [
          { value: this.activityInstance.type, disabled: true },
          Validators.required,
        ],
      });

      if (this.activityTypesMap) {
        const { type } = this.activityInstance;
        const { parameters } = this.activityTypesMap[type];
        this.formParameters.clear();
        parameters.forEach(parameter => {
          this.formParameters.push(
            this.fb.group({
              name: parameter.name,
              type: parameter.type,
              value: this.activityInstance.parameters[parameter.name].value,
            }),
          );
        });
      }
    }
  }

  get formParameters() {
    return this.form.get('parameters') as FormArray;
  }

  onDelete() {
    this.delete.emit(this.activityInstance.id);
  }

  onSubmit() {
    if (this.form.valid) {
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
      const activityInstance: Partial<SActivityInstance> = {
        ...this.form.value,
        parameters,
      };
      this.update.emit({
        activityInstance,
        activityInstanceId: this.activityInstance.id,
      });
    }
  }
}
