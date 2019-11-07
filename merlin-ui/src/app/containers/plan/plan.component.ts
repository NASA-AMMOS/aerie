import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
} from '@angular/core';
import { Sort } from '@angular/material/sort';
import { ActivatedRoute } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { SubSink } from 'subsink';
import { MerlinActions } from '../../actions';
import { AppState } from '../../app-store';
import { compare } from '../../functions';
import {
  getActivityInstancesForSelectedPlan,
  getActivityTypes,
  getLoading,
  getSelectedPlan,
} from '../../selectors';
import { CActivityInstance, CActivityType, CPlan } from '../../types';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-plan',
  styleUrls: ['./plan.component.css'],
  templateUrl: './plan.component.html',
})
export class PlanComponent implements OnDestroy {
  activityInstances: CActivityInstance[] = [];
  activityTypes: CActivityType[] = [];
  displayedColumns: string[] = ['menu', 'type', 'startTimestamp'];
  loading = false;
  panels = {
    activityTypes: {
      order: 0,
      size: 20,
      visible: true,
    },
    createActivityInstance: {
      order: 1,
      size: 20,
      visible: true,
    },
    activityInstances: {
      order: 2,
      size: 20,
      visible: true,
    },
  };
  plan: CPlan | null = null;
  sortedActivityInstances: CActivityInstance[] = [];

  private subs = new SubSink();

  constructor(
    private ref: ChangeDetectorRef,
    private route: ActivatedRoute,
    private store: Store<AppState>,
  ) {
    this.subs.add(
      this.store
        .pipe(select(getActivityInstancesForSelectedPlan))
        .subscribe(activityInstances => {
          this.activityInstances = activityInstances;
          this.sortedActivityInstances = [...activityInstances];
          this.ref.markForCheck();
        }),
      this.store.pipe(select(getActivityTypes)).subscribe(activityTypes => {
        this.activityTypes = activityTypes;
        this.ref.markForCheck();
      }),
      this.store.pipe(select(getLoading)).subscribe(loading => {
        this.loading = loading;
        this.ref.markForCheck();
      }),
      this.store.pipe(select(getSelectedPlan)).subscribe(plan => {
        this.plan = plan;
        this.ref.markForCheck();
      }),
    );
  }

  ngOnDestroy() {
    this.subs.unsubscribe();
  }

  onAbout() {
    this.store.dispatch(MerlinActions.openAboutDialog());
  }

  onDeleteActivityInstance(activityInstanceId: string) {
    const { id: planId } = this.route.snapshot.params;
    this.store.dispatch(
      MerlinActions.deleteActivityInstance({ planId, activityInstanceId }),
    );
  }

  sortActivityInstances(sort: Sort): void {
    const activityInstances = [...this.activityInstances];

    if (!sort.active || sort.direction === '') {
      this.sortedActivityInstances = activityInstances;
      return;
    }

    this.sortedActivityInstances = activityInstances.sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case 'type':
          return compare(a.type, b.type, isAsc);
        case 'startTimestamp':
          return compare(a.startTimestamp, b.startTimestamp, isAsc);
        default:
          return 0;
      }
    });
  }

  togglePanelVisible(panel: string) {
    this.panels[panel].visible = !this.panels[panel].visible;
  }
}
