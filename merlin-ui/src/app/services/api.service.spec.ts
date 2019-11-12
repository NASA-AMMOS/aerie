import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TestScheduler } from 'rxjs/testing';
import { environment } from '../../environments/environment';
import {
  activityInstanceId,
  adaptationId,
  cActivityInstanceMap,
  cAdaptationMap,
  cPlan,
  cPlanMap,
  planId,
  sActivityInstance,
  sActivityInstanceMap,
  sAdaptation,
  sAdaptationMap,
  sPlan,
  sPlanMap,
} from '../mocks';
import { ApiService } from './api.service';

const { adaptationServiceBaseUrl, planServiceBaseUrl } = environment;

describe('api service', () => {
  let apiService: ApiService;
  let httpTestingController: HttpTestingController;
  let testScheduler: TestScheduler;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    apiService = TestBed.inject(ApiService);
    httpTestingController = TestBed.inject(HttpTestingController);
    testScheduler = new TestScheduler((actual, expected) => {
      expect(actual).toEqual(expected);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('getActivityInstances', () => {
    apiService.getActivityInstances(planId).subscribe(activityInstances => {
      expect(activityInstances).toEqual(cActivityInstanceMap);
    });
    const req = httpTestingController.expectOne(
      `${planServiceBaseUrl}/plans/${planId}/activity_instances`,
    );
    req.flush(sActivityInstanceMap);
  });

  it('getAdaptations', () => {
    apiService.getAdaptations().subscribe(adaptations => {
      expect(adaptations).toEqual(cAdaptationMap);
    });
    const req = httpTestingController.expectOne(
      `${adaptationServiceBaseUrl}/adaptations`,
    );
    req.flush(sAdaptationMap);
  });

  it('createActivityInstances', () => {
    apiService
      .createActivityInstances(planId, [sActivityInstance])
      .subscribe(ids => {
        expect(ids).toEqual([activityInstanceId]);
      });
    const req = httpTestingController.expectOne(
      `${planServiceBaseUrl}/plans/${planId}/activity_instances`,
    );
    req.flush([activityInstanceId]);
  });

  it('createAdaptation', () => {
    apiService
      .createAdaptation({ ...sAdaptation, file: new File([], '') })
      .subscribe(res => {
        expect(res).toEqual({ id: adaptationId });
      });
    const req = httpTestingController.expectOne(
      `${adaptationServiceBaseUrl}/adaptations`,
    );
    req.flush({ id: adaptationId });
  });

  it('createPlan', () => {
    apiService.createPlan(sPlan).subscribe(res => {
      expect(res).toEqual({ id: planId });
    });
    const req = httpTestingController.expectOne(`${planServiceBaseUrl}/plans`);
    req.flush({ id: planId });
  });

  it('deleteActivityInstance', () => {
    apiService
      .deleteActivityInstance(planId, activityInstanceId)
      .subscribe(res => {
        expect(res).toEqual({});
      });
    const req = httpTestingController.expectOne(
      `${planServiceBaseUrl}/plans/${planId}/activity_instances/${activityInstanceId}`,
    );
    req.flush({});
  });

  it('deleteAdaptation', () => {
    apiService.deleteAdaptation(adaptationId).subscribe(res => {
      expect(res).toEqual({});
    });
    const req = httpTestingController.expectOne(
      `${adaptationServiceBaseUrl}/adaptations/${adaptationId}`,
    );
    req.flush({});
  });

  it('deletePlan', () => {
    apiService.deletePlan(planId).subscribe(res => {
      expect(res).toEqual({});
    });
    const req = httpTestingController.expectOne(
      `${planServiceBaseUrl}/plans/${planId}`,
    );
    req.flush({});
  });

  it('getPlans', () => {
    apiService.getPlans().subscribe(plans => {
      expect(plans).toEqual(cPlanMap);
    });
    const req = httpTestingController.expectOne(`${planServiceBaseUrl}/plans`);
    req.flush(sPlanMap);
  });

  it('getPlan', () => {
    apiService.getPlan(planId).subscribe(plan => {
      expect(plan).toEqual(cPlan);
    });
    const req = httpTestingController.expectOne(
      `${planServiceBaseUrl}/plans/${planId}`,
    );
    req.flush(sPlan);
  });
});
