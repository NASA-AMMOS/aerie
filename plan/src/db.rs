use rocket_contrib::databases::mongodb::db::{Database,ThreadedDatabase};
use rocket_contrib::databases::mongodb::{Bson,Document,bson,doc,oid::{ObjectId}};

use models::{Plan,PatchPlan};

const PLANS_COLLECTION: &str = "plans";

#[database("plan_service")]
pub struct PlanServiceDatabase(Database);

pub fn get_plans(conn: &PlanServiceDatabase) -> Result<Vec<Plan>, &str> {
  let coll = conn.0.collection(PLANS_COLLECTION);
  let cursor = coll.find(None, None).unwrap();
  let mut plans: Vec<Plan> = Vec::new();

  for result in cursor {
    if let Ok(item) = result {
      plans.push(Plan {
        adaptation_id: item.get("adaptation_id").unwrap().to_string(),
        end: item.get("end").unwrap().to_string(),
        id: item.get_object_id("_id").unwrap().to_string(),
        name: item.get("name").unwrap().to_string(),
        start: item.get("start").unwrap().to_string(),
      });
    }
  }

  Ok(plans)
}

pub fn create_plan(conn: &PlanServiceDatabase, plan: Plan) -> Result<Plan, &str> {
    let coll = conn.0.collection(PLANS_COLLECTION);
    let new_plan = doc! {
        "adaptation_id": plan.adaptation_id.clone(),
        "end": plan.end.clone(),
        "name": plan.name.clone(),
        "start": plan.start.clone(),
    };

    match coll.insert_one(new_plan, None) {
      // All of this is to get just a simple id string back from the insert_id BSON ObjectId API
      Ok(result) => {
        match result.inserted_id {
          Some(bson) => match bson.as_object_id() {
            Some(object_id) => Ok(Plan {
              id: object_id.to_hex(),
              adaptation_id: plan.adaptation_id.clone(),
              end: plan.end.clone(),
              name: plan.name.clone(),
              start: plan.start.clone(),
            }),
            None => return Err("Invalid ObjectId") 
          },
          None => return Err("Invalid ObjectId")
        }
      },
      Err(_) => Err("Database error"),
    }
}

pub fn get_plan(conn: &PlanServiceDatabase, id: String) -> Result<Plan, &str> {

    // MongoDB IDs are ObjectIds not strings
    let object_id = ObjectId::with_string(&id).unwrap();
    let filter = doc!{ "_id": object_id };

    let coll = conn.0.collection(PLANS_COLLECTION);
    match coll.find_one(Some(filter), None).expect("fail") {
        Some(result) => Ok(Plan {
            id: result.get_object_id("_id").unwrap().to_string(),
            adaptation_id: result.get("adaptation_id").unwrap().to_string(),
            end: result.get("end").unwrap().to_string(),
            name: result.get("name").unwrap().to_string(),
            start: result.get("start").unwrap().to_string(),
        }),
        None => Err("Plan not found")
    }
}

pub fn update_plan(conn: &PlanServiceDatabase, id: String, plan: PatchPlan) -> Result<(), &str> {
    let coll = conn.0.collection(PLANS_COLLECTION);

    // MongoDB IDs are ObjectIds not strings
    let object_id = ObjectId::with_string(&id).unwrap();
    let filter = doc!{ "_id": object_id };

    // Only insert the changed values into a document for updating
    let mut props = Document::new();

    if let Some(v) = plan.adaptation_id {
        props.insert("adaptation_id".to_owned(), Bson::String(v.to_owned()));
    };

    if let Some(v) = plan.end {
        props.insert("end".to_owned(), Bson::String(v.to_owned()));
    };

    if let Some(v) = plan.name {
        props.insert("name".to_owned(), Bson::String(v.to_owned()));
    };

    if let Some(v) = plan.start {
        props.insert("start".to_owned(), Bson::String(v.to_owned()));
    };

    // Updates have to include the $set operator
    // https://docs.mongodb.com/manual/reference/operator/update/set/#up._S_set
    let update = doc! {
        "$set": props
    };

    match coll.find_one_and_update(filter, update, None) {
        Ok(_) => Ok(()),
        Err(_) => Err("Plan not found")
    }
}

pub fn delete_plan(conn: &PlanServiceDatabase, id: String) -> Result<(), &str> {
    let coll = conn.0.collection(PLANS_COLLECTION);

    // MongoDB IDs are ObjectIds not strings
    let object_id = ObjectId::with_string(&id).unwrap();
    let filter = doc!{ "_id": object_id };

    match coll.find_one_and_delete(filter, None) {
        Ok(_) => Ok(()),
        Err(_) => Err("Database error"),
    }
}