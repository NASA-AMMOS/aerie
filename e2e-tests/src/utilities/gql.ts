/**
 * GraphQL Query and Mutation strings.
 */
const gql = {
  CREATE_MISSION_MODEL: `#graphql
    mutation CreateMissionModel($model: mission_model_insert_input!) {
      insert_mission_model_one(object: $model) {
        id
      }
    }
  `,

  DELETE_MISSION_MODEL: `#graphql
    mutation DeleteModel($id: Int!) {
      delete_mission_model_by_pk(id: $id) {
        id
      }
    }
  `,
};

export default gql;
