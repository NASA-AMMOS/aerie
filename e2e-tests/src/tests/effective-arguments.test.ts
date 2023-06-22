import { expect, test } from "@playwright/test";
import req from "../utilities/requests.js";
import time from "../utilities/time.js";

let jar_id: number;
let mission_model_id: number;
const rd = Math.random() * 100;

test.describe("Initialize", async () => {
  test("Upload jar and create mission model", async ({ request }) => {
    jar_id = await req.uploadJarFile(request);

    const model: MissionModelInsertInput = {
      jar_id,
      mission: "aerie_e2e_tests" + rd,
      name: "Banananation (e2e tests)"+rd,
      version: "0.0.0"+ rd,
    };
    mission_model_id = await req.createMissionModel(request, model);

    expect(mission_model_id).not.toBeNull();
    expect(mission_model_id).toBeDefined();
    expect(typeof mission_model_id).toEqual("number");

    // delay 2000ms for model generation
    await time.delay(2000);
  });
});

test.describe.serial("Query Effect Argument", async () => {
  test("Query single activity for default arguments", async ({ request }) => {
    const resp = await req.getEffectiveArguments(request, mission_model_id, "BiteBanana", {});

    expect(resp).not.toBeNull();
    expect(resp).toBeDefined();
    expect(resp.success).toEqual(true);
    expect(resp.arguments).not.toBeNull();
    expect(resp.arguments).toBeDefined();
    expect(resp.arguments.biteSize).toEqual(1);
  });

  test("Query single activity for passed arguments", async ({ request }) => {
    const resp = await req.getEffectiveArguments(request, mission_model_id, "BiteBanana", { biteSize: 2 });

    expect(resp).not.toBeNull();
    expect(resp).toBeDefined();
    expect(resp.success).toEqual(true);
    expect(resp.arguments).not.toBeNull();
    expect(resp.arguments).toBeDefined();
    expect(resp.arguments.biteSize).toEqual(2);
  });

  test("Query bulk activities with proper passed arguments", async ({ request }) => {
    const bite_banana_bulk_item: EffectiveArgumentItem = {
      activityTypeName: "BiteBanana",
      activityArguments: { biteSize: 1 },
    };
    const bake_banana_bulk_item_1: EffectiveArgumentItem = {
      activityTypeName: "BakeBananaBread",
      activityArguments: {
        tbSugar: 1,
        glutenFree: true,
      },
    };
    const bake_banana_bulk_item_2: EffectiveArgumentItem = {
      activityTypeName: "BakeBananaBread",
      activityArguments: {
        tbSugar: 2,
        glutenFree: true,
        temperature: 400,
      },
    };

    const bulk_input = [ bite_banana_bulk_item, bake_banana_bulk_item_1, bake_banana_bulk_item_2 ];
    const resp = await req.getEffectiveArgumentsBulk(request, mission_model_id, bulk_input);

    expect(resp).not.toBeNull();
    expect(resp).toBeDefined();
    expect(resp).toHaveLength(bulk_input.length);

    for (let i = 0; i < resp.length; i++) {
      expect(resp[i].success).toEqual(true);
      expect(resp[i].typeName).toEqual(bulk_input[i].activityTypeName);
    }

    expect(resp[1].arguments.temperature).toEqual(350); // default arg value
    expect(resp[2].arguments.temperature).toEqual(400); // passed arg value
  });

  test("Bulk query one activity with proper passed arguments", async ({ request }) => {
    const bite_banana_bulk_item: EffectiveArgumentItem = {
      activityTypeName: "BiteBanana",
      activityArguments: { biteSize: 1 },
    };
    const bulk_input = [ bite_banana_bulk_item ];
    const resp = await req.getEffectiveArgumentsBulk(request, mission_model_id, bulk_input);

    expect(resp).not.toBeNull();
    expect(resp).toBeDefined();
    expect(resp).toHaveLength(bulk_input.length);

    expect(resp[0].success).toEqual(true);
    expect(resp[0].typeName).toEqual(bulk_input[0].activityTypeName);
  });

  test("Query bulk activities with one activity missing arguments", async ({ request }) => {
    const bite_banana_bulk_item: EffectiveArgumentItem = {
      activityTypeName: "BiteBanana",
      activityArguments: { biteSize: 1 },
    };
    const bake_banana_bulk_item: EffectiveArgumentItem = {
      activityTypeName: "BakeBananaBread",
      activityArguments: {},
    };
    const bulk_input = [ bite_banana_bulk_item, bake_banana_bulk_item ];
    const resp = await req.getEffectiveArgumentsBulk(request, mission_model_id, bulk_input);

    expect(resp).not.toBeNull();
    expect(resp).toBeDefined();
    expect(resp).toHaveLength(bulk_input.length);

    expect(resp[1].success).toEqual(false);
  });

  test("Query bulk activities with multiple input errors", async ({ request }) => {
    const bite_banana_dne_bulk_item: EffectiveArgumentItem = {
      activityTypeName: "BiteBananaDOESNOTEXIST",
      activityArguments: { biteSize: 1 },
    };
    const bake_banana_bulk_item: EffectiveArgumentItem = {
      activityTypeName: "BakeBananaBread",
      activityArguments: {},
    };
    const bite_banana_bulk_item: EffectiveArgumentItem = {
      activityTypeName: "BiteBanana",
      activityArguments: {},
    };

    const bite_banana_dne_bulk_item_expected = {
      typeName: "BiteBananaDOESNOTEXIST",
      success: false,
      errors: "No such activity type",
    };
    const bake_banana_bulk_item_expected = {
      typeName: "BakeBananaBread",
      success: false,
      arguments: {
        temperature: 350,
      },
      errors: {
        extraneousArguments: [],
        missingArguments: [ "tbSugar", "glutenFree" ],
        unconstructableArguments: [],
      },
    };
    const bite_banana_bulk_item_expected = {
      typeName: "BiteBanana",
      success: true,
      arguments: {
        biteSize: 1,
      },
    };

    const bulk_input = [ bite_banana_dne_bulk_item, bake_banana_bulk_item, bite_banana_bulk_item ];
    const resp = await req.getEffectiveArgumentsBulk(request, mission_model_id, bulk_input);

    expect(resp).not.toBeNull();
    expect(resp).toBeDefined();
    expect(resp).toHaveLength(bulk_input.length);

    expect(resp[0]).toEqual(bite_banana_dne_bulk_item_expected);
    expect(resp[1]).toEqual(bake_banana_bulk_item_expected);
    expect(resp[2]).toEqual(bite_banana_bulk_item_expected);
  });

});

test.describe("Clean Up", async () => {
  test("Check model is deleted", async ({ request }) => {
    const id = await req.deleteMissionModel(request, mission_model_id);

    expect(id).not.toBeNull();
    expect(id).toBeDefined();
    expect(id).toEqual(mission_model_id);
  });
});
