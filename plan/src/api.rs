use rocket::Request;
use rocket_contrib::json::{Json,JsonValue};

use db;
use models::{Plan,PatchPlan};

#[get("/")]
pub fn index() -> &'static str {
    "Hello, world!"
}

#[get("/plans")]
pub fn get_plans(conn: db::PlanServiceDatabase) -> JsonValue {
  match db::get_plans(&conn) {
    Ok(plans) => json!({
        "status": "success".to_string(),
        "data": plans,
    }),
    Err(e) => json!({
        "status": "error",
        "message": e.to_string(),
    })
  }
}

#[derive(Serialize, Deserialize)]
pub struct CreatePlanRequestBody {
    pub adaptation_id: String,
    pub end: String,
    pub name: String,
    pub start: String,
}

#[post("/plans", format = "json", data = "<body>")]
pub fn create_plan(conn: db::PlanServiceDatabase, body: Json<CreatePlanRequestBody>) -> JsonValue {
  match db::create_plan(&conn, Plan {
    // TODO: Make ID optional? Create a differnt struct with all optional values?
    id: "".to_string(),
    adaptation_id: body.adaptation_id.clone(),
    end: body.end.clone(),
    name: body.name.clone(),
    start: body.start.clone(),
  }) {
      Ok(plan) => json!({
          "status": "success",
          "data": plan
      }),
      Err(e) => json!({
          "status": "error",
          "message": e.to_string()
      })
  }
}

#[get("/plans/<id>")]
pub fn get_plan(conn: db::PlanServiceDatabase, id: String) -> JsonValue {
  match db::get_plan(&conn, id.clone()) {
      Ok(plan) => json!({
          "status": "success",
          "data": plan,
      }),
      Err(e) => json!({
          "status": "error",
          "message": e.to_string()
      })
  }
}

#[patch("/plans/<id>", format = "json", data = "<body>")]
pub fn update_plan(conn: db::PlanServiceDatabase, id: String, body: Json<PatchPlan>) -> JsonValue {
    let plan = body.into_inner();    
    match db::update_plan(&conn, id.clone(), plan) {
        Ok(_) => json!({
            "status": "success",
            "data": ()
        }),
        Err(e) => json!({
            "status": "error",
            "message": e.to_string()
        })
    }

}

#[delete("/plans/<id>")]
pub fn delete_plan(conn: db::PlanServiceDatabase, id: String) -> JsonValue {
    match db::delete_plan(&conn, id.clone()) {
        Ok(_) => json!({
            "status": "success",
            "data": ()
        }),
        Err(e) => json!({
            "status": "error",
            "message": e.to_string()
        })
    }
}

#[catch(404)]
pub fn not_found(_req: &Request) -> JsonValue {
  json!({
    "message": "",
    "status": "error",
  })
}

#[catch(500)]
pub fn server_error(_req: &Request) -> JsonValue {
  json!({
    "message": "",
    "status": "error",
  })
}