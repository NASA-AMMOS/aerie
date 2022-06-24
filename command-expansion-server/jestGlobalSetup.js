import { config } from 'dotenv';
config();

process.env.MERLIN_GATEWAY_URL = process.env.MERLIN_GATEWAY_URL ?? 'http://localhost:9000';
process.env.MERLIN_GRAPHQL_URL = process.env.MERLIN_GRAPHQL_URL ?? 'http://localhost:8080/v1/graphql';

export default async () => {
};
