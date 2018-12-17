#![feature(proc_macro_hygiene, decl_macro)]

#[macro_use] extern crate rocket;
#[macro_use] extern crate rocket_contrib;
#[macro_use] extern crate serde_derive;

mod db;
mod api;
mod models;

fn main() {
    rocket::ignite()
        .attach(db::PlanServiceDatabase::fairing())
        .mount(
            "/api",
            routes![
                api::index,
                api::get_plans,
                api::get_plan,
                api::create_plan,
                api::update_plan,
                api::delete_plan
            ],
        )
        .register(
            catchers![
                api::not_found,
                api::server_error
            ]
        )
        .launch();
}
