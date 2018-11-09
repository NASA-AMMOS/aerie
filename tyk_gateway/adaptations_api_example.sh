# Query the adaptations endpoint for all adaptations
curl -i -H "x-tyk-authorization: 17584bafc66442bc5da0acdc52dfa7c0" http://localhost:8081/adaptations/

# Query the adaptations endpoint for a single adaptation
curl -i -H "x-tyk-authorization: 17584bafc66442bc5da0acdc52dfa7c0" http://localhost:8081/adaptations/1
