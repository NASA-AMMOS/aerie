#[derive(Debug,Serialize,Deserialize)]
pub struct Plan {
  pub adaptation_id: String,
  pub end: String,
  pub id: String,
  pub name: String,
  pub start: String,
  // pub activity_instances: Vec<ActivityInstance>
}

// pub struct ActivityInstance {
//     pub parameters: Vec<Parameter>,
// }

// pub struct Parameter {
//     pub type_name: String, // ProtoBuffer type (e.g. String, Double, Duration)
//     pub value: String, // ProtoBuffer value, e.g. String "foo", Double 2.00, Duration 12:00
// }

/// A Plan which contains all Option properties used to patch a resource
#[derive(Debug,Serialize,Deserialize)]
pub struct PatchPlan {
    pub adaptation_id: Option<String>,
    pub end: Option<String>,
    pub name: Option<String>,
    pub start: Option<String>
}
