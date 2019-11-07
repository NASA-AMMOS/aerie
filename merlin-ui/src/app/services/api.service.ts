import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import omit from 'lodash-es/omit';
import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  CActivityInstanceMap,
  CActivityInstanceParameterMap,
  CActivityTypeMap,
  CActivityTypeParameter,
  CAdaptationMap,
  CPlan,
  CPlanMap,
  Id,
  SActivityInstance,
  SActivityInstanceMap,
  SActivityTypeMap,
  SAdaptationMap,
  SCreateAdaption,
  SPlan,
  SPlanMap,
} from '../types';

const { adaptationServiceBaseUrl, planServiceBaseUrl } = environment;

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  constructor(private http: HttpClient) {}

  getActivityInstances(planId: string): Observable<CActivityInstanceMap> {
    return this.http
      .get<SActivityInstanceMap>(
        `${planServiceBaseUrl}/plans/${planId}/activity_instances`,
      )
      .pipe(
        map((sActivityInstanceMap: SActivityInstanceMap) => {
          return Object.keys(sActivityInstanceMap).reduce(
            (cActivityInstanceMap: CActivityInstanceMap, id: string) => {
              cActivityInstanceMap[id] = {
                ...sActivityInstanceMap[id],
                id,
                parameters: Object.keys(
                  sActivityInstanceMap[id].parameters,
                ).reduce(
                  (
                    cActivityInstanceParameterMap: CActivityInstanceParameterMap,
                    name: string,
                  ) => {
                    cActivityInstanceParameterMap[name] = {
                      ...sActivityInstanceMap[id].parameters[name],
                      name,
                    };
                    return cActivityInstanceParameterMap;
                  },
                  {},
                ),
              };
              return cActivityInstanceMap;
            },
            {},
          );
        }),
      );
  }

  getPlanAndActivityTypes(
    planId: string,
  ): Observable<{ activityTypes: CActivityTypeMap; plan: CPlan }> {
    return this.http.get<SPlan>(`${planServiceBaseUrl}/plans/${planId}`).pipe(
      switchMap((sPlan: SPlan) => {
        const plan = {
          ...omit(sPlan, 'activityInstances'),
          id: planId,
          activityInstanceIds: Object.keys(sPlan.activityInstances),
        };

        return this.http
          .get<SActivityTypeMap>(
            `${adaptationServiceBaseUrl}/adaptations/${plan.adaptationId}/activities`,
          )
          .pipe(
            map((sActivityTypeMap: SActivityTypeMap) => {
              const activityTypes = Object.keys(sActivityTypeMap).reduce(
                (
                  cActivityTypeMap: CActivityTypeMap,
                  activityTypeName: string,
                ) => {
                  cActivityTypeMap[activityTypeName] = {
                    ...sActivityTypeMap[activityTypeName],
                    name: activityTypeName,
                    parameters: Object.keys(
                      sActivityTypeMap[activityTypeName].parameters,
                    ).reduce(
                      (
                        cActivityTypeParameters: CActivityTypeParameter[],
                        parameterName: string,
                      ) => {
                        cActivityTypeParameters.push({
                          ...sActivityTypeMap[activityTypeName].parameters[
                            parameterName
                          ],
                          name: parameterName,
                        });
                        return cActivityTypeParameters;
                      },
                      [],
                    ),
                  };
                  return cActivityTypeMap;
                },
                {},
              );
              return { activityTypes, plan };
            }),
          );
      }),
    );
  }

  getAdaptations(): Observable<CAdaptationMap> {
    return this.http
      .get<SAdaptationMap>(`${adaptationServiceBaseUrl}/adaptations`)
      .pipe(
        map((sAdaptationMap: SAdaptationMap) => {
          return Object.keys(sAdaptationMap).reduce(
            (cAdaptationMap: CAdaptationMap, id: string) => {
              cAdaptationMap[id] = {
                ...sAdaptationMap[id],
                id,
              };
              return cAdaptationMap;
            },
            {},
          );
        }),
      );
  }

  createActivityInstances(
    planId: string,
    activityInstances: SActivityInstance[],
  ): Observable<string[]> {
    return this.http.post<string[]>(
      `${planServiceBaseUrl}/plans/${planId}/activity_instances`,
      activityInstances,
    );
  }

  createAdaptation(adaptation: SCreateAdaption): Observable<Id> {
    const formData = new FormData();
    formData.append('file', adaptation.file, adaptation.file.name);
    formData.append('mission', adaptation.mission);
    formData.append('name', adaptation.name);
    formData.append('owner', adaptation.owner);
    formData.append('version', adaptation.version);

    return this.http.post<Id>(
      `${adaptationServiceBaseUrl}/adaptations`,
      formData,
    );
  }

  createPlan(plan: SPlan): Observable<Id> {
    return this.http.post<Id>(`${planServiceBaseUrl}/plans`, plan);
  }

  deleteActivityInstance(
    planId: string,
    activityInstanceId: string,
  ): Observable<{}> {
    return this.http.delete(
      `${planServiceBaseUrl}/plans/${planId}/activity_instances/${activityInstanceId}`,
    );
  }

  deleteAdaptation(id: string): Observable<{}> {
    return this.http.delete(`${adaptationServiceBaseUrl}/adaptations/${id}`);
  }

  deletePlan(id: string): Observable<{}> {
    return this.http.delete(`${planServiceBaseUrl}/plans/${id}`);
  }

  getPlans(): Observable<CPlanMap> {
    return this.http.get<SPlanMap>(`${planServiceBaseUrl}/plans`).pipe(
      map((sPlanMap: SPlanMap) => {
        return Object.keys(sPlanMap).reduce(
          (cPlanMap: CPlanMap, id: string) => {
            cPlanMap[id] = {
              ...omit(sPlanMap[id], 'activityInstances'),
              id,
              activityInstanceIds: Object.keys(sPlanMap[id].activityInstances),
            };
            return cPlanMap;
          },
          {},
        );
      }),
    );
  }

  getPlan(planId: string): Observable<CPlan> {
    return this.http.get<SPlan>(`${planServiceBaseUrl}/plans/${planId}`).pipe(
      map((sPlan: SPlan) => {
        return {
          ...omit(sPlan, 'activityInstances'),
          id: planId,
          activityInstanceIds: Object.keys(sPlan.activityInstances),
        };
      }),
    );
  }
}
