import { MerlinActions } from '../actions';
import {
  activityInstanceId,
  adaptationId,
  cActivityInstanceMap,
  cActivityTypeMap,
  cAdaptation,
  cAdaptationMap,
  cPlan,
  cPlanMap,
  planId,
  sActivityInstance,
  sAdaptation,
  sPlan,
} from '../mocks';
import { SCreateAdaption, SPlan } from '../types';
import { initialState, MerlinState, reducer } from './merlin.reducer';

describe('merlin reducer', () => {
  describe('createAdaptationSuccess', () => {
    it('should set adaptations', () => {
      const adaptation: SCreateAdaption = {
        ...sAdaptation,
        file: new File([], ''),
      };
      const state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.createAdaptationSuccess({ id: adaptationId, adaptation }),
      );
      expect(state).toEqual({
        ...initialState,
        adaptations: {
          [adaptationId]: {
            ...cAdaptation,
          },
        },
      });
    });
  });

  describe('createPlanSuccess', () => {
    it('should set plan', () => {
      const plan: SPlan = {
        ...sPlan,
      };
      const state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.createPlanSuccess({ id: planId, plan }),
      );
      expect(state).toEqual({
        ...initialState,
        plans: {
          [planId]: {
            ...cPlan,
            activityInstanceIds: [],
          },
        },
      });
    });
  });

  describe('deleteActivityInstanceSuccess', () => {
    it('it should delete an activity instance', () => {
      let state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setActivityInstances({
          activityInstances: cActivityInstanceMap,
          planId,
        }),
      );
      state = reducer(
        state,
        MerlinActions.setSelectedActivityInstanceId({
          selectedActivityInstanceId: activityInstanceId,
        }),
      );
      expect(state).toEqual({
        ...initialState,
        activityInstances: cActivityInstanceMap,
        selectedActivityInstanceId: activityInstanceId,
      });
      state = reducer(
        state,
        MerlinActions.deleteActivityInstanceSuccess({ activityInstanceId }),
      );
      expect(state).toEqual({
        ...initialState,
        activityInstances: {},
        selectedActivityInstanceId: null,
      });
    });
  });

  describe('deleteAdaptationSuccess', () => {
    it('it should delete an adaptation', () => {
      let state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setAdaptations({
          adaptations: cAdaptationMap,
        }),
      );
      state = reducer(
        state,
        MerlinActions.deleteAdaptationSuccess({ id: adaptationId }),
      );
      expect(state).toEqual({
        ...initialState,
        adaptations: {},
      });
    });
  });

  describe('deletePlanSuccess', () => {
    it('it should delete a plan', () => {
      let state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setPlans({
          plans: cPlanMap,
        }),
      );
      state = reducer(state, MerlinActions.deletePlanSuccess({ id: planId }));
      expect(state).toEqual({
        ...initialState,
        plans: {},
      });
    });
  });

  describe('setActivityInstances', () => {
    it('it should set activityInstances', () => {
      const state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setActivityInstances({
          activityInstances: cActivityInstanceMap,
          planId,
        }),
      );
      expect(state).toEqual({
        ...initialState,
        activityInstances: cActivityInstanceMap,
      });
    });
  });

  describe('setAdaptations', () => {
    it('it should set adaptations', () => {
      const state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setAdaptations({
          adaptations: cAdaptationMap,
        }),
      );
      expect(state).toEqual({
        ...initialState,
        adaptations: cAdaptationMap,
      });
    });
  });

  describe('setLoading', () => {
    it('it should set loading', () => {
      const loading = true;
      const state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setLoading({ loading }),
      );
      expect(state).toEqual({
        ...initialState,
        loading,
      });
    });
  });

  describe('setPlans', () => {
    it('it should set plans', () => {
      const state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setPlans({
          plans: cPlanMap,
        }),
      );
      expect(state).toEqual({
        ...initialState,
        plans: cPlanMap,
      });
    });
  });

  describe('setSelectedActivityInstanceId', () => {
    it('it should set setSelectedActivityInstanceId', () => {
      const selectedActivityInstanceId = '42';
      const state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setSelectedActivityInstanceId({
          selectedActivityInstanceId,
        }),
      );
      expect(state).toEqual({
        ...initialState,
        selectedActivityInstanceId,
      });
    });
  });

  describe('setSelectedPlanAndActivityTypes', () => {
    it('it should set plans and activity types', () => {
      const state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setSelectedPlanAndActivityTypes({
          activityTypes: cActivityTypeMap,
          selectedPlan: cPlan,
        }),
      );
      expect(state).toEqual({
        ...initialState,
        activityTypes: cActivityTypeMap,
        selectedPlan: cPlan,
      });
    });
  });

  describe('updateActivityInstanceSuccess', () => {
    it('it should update activity instances', () => {
      let state: MerlinState = reducer(
        { ...initialState },
        MerlinActions.setActivityInstances({
          activityInstances: cActivityInstanceMap,
          planId,
        }),
      );
      state = reducer(
        state,
        MerlinActions.updateActivityInstanceSuccess({
          activityInstance: sActivityInstance,
          activityInstanceId,
        }),
      );
      expect(state).toEqual({
        ...initialState,
        activityInstances: cActivityInstanceMap,
      });
    });
  });
});
