import { expect, test } from '@playwright/test';
import req, { awaitSimulation } from '../utilities/requests.js';

test.describe.serial('Check Constraints Against Specific Sim Datasets', () => {
    const constraintName = 'fruit_equal_peel';
    const activity_offset_hours = 1;

    const plan_start_timestamp = "2023-01-01T00:00:00+00:00";

    let mission_model_id: number;
    let plan_id: number;
    let constraint_id: number;
    let violation: Violation;
    let activity_id: number;

    test.beforeAll(async ({request}) => {
        let rd = Math.random() * 100000;
        let jar_id = await req.uploadJarFile(request);
        // Add Mission Model
        const model: MissionModelInsertInput = {
            jar_id,
            mission: 'aerie_e2e_tests',
            name: 'Banananation (e2e tests)',
            version: rd + "",
        };
        mission_model_id = await req.createMissionModel(request, model);

        // Add Plan
        const plan_input: CreatePlanInput = {
            model_id: mission_model_id,
            name: 'test_plan' + rd,
            start_time: plan_start_timestamp,
            duration: "24:00:00"
        };
        plan_id = await req.createPlan(request, plan_input);

        // Add Activity
        const activityToInsert: ActivityInsertInput = {
            arguments: {
                biteSize: 2,
            },
            plan_id: plan_id,
            type: 'BiteBanana',
            start_offset: activity_offset_hours + 'h',
        };
        activity_id = await req.insertActivity(request, activityToInsert);

        // Add Constraint
        const constraint: ConstraintInsertInput = {
            name: constraintName,
            definition: 'export default (): Constraint => Real.Resource("/fruit").equal(Real.Resource("/peel"))',
            description: '',
            model_id: null,
            plan_id,
        };
        constraint_id = await req.insertConstraint(request, constraint);
    });

    test.afterAll(async ({request}) => {
        // Delete Constraint
        await req.deleteConstraint(request, constraint_id);

        // Deleting Plan and Model cascades the rest of the cleanup
        await req.deleteMissionModel(request, mission_model_id);
        await req.deletePlan(request, plan_id);
    });

    test('Constraints Loads Specified Outdated Sim Dataset', async ({request}) => {
        // Simulate
        const oldSimDatasetId = (await awaitSimulation(request, plan_id)).simulationDatasetId;

        // Invalidate Results and Resim
        await req.deleteActivity(request, plan_id, activity_id);
        const newSimulationDatasetId = (await awaitSimulation(request, plan_id)).simulationDatasetId;

        // Assert No Violations on Newest Results
        const newConstraintResults = await req.checkConstraints(request, plan_id, newSimulationDatasetId);
        expect(newConstraintResults).toHaveLength(0);

        // Assert One Violation on Old Results
        const oldConstraintResults = await req.checkConstraints(request, plan_id, oldSimDatasetId);
        expect(oldConstraintResults).toHaveLength(1);

        // Assert the Results to be as expected
        const result = oldConstraintResults.pop();
        expect(result.constraintName).toEqual(constraintName);
        expect(result.constraintId).toEqual(constraint_id);

        // Resources
        expect(result.resourceIds).toHaveLength(2);
        expect(result.resourceIds).toContain('/fruit');
        expect(result.resourceIds).toContain('/peel');

        // Violation
        expect(result.violations).toHaveLength(1);

        // Violation Window
        const plan_duration_micro = 24 * 60 * 60 * 1000 * 1000;
        const activity_offset_micro = activity_offset_hours * 60 * 60 * 1000 * 1000;

        violation = result.violations[0];
        expect(violation.windows[0].start).toEqual(activity_offset_micro);
        expect(violation.windows[0].end).toEqual(plan_duration_micro);

        // Gaps
        expect(result.gaps).toHaveLength(0);
    });
});
