# Valid Sequence JSON Format
We have discussed with the JSON Sequence Format team what will be the minimum needed to generate SCMFS for Europa Clipper.

In order to generate SCMFS, we need a Sequence JSON containing the mandatory fields below: 

```
{
  "id": "<seq_id>",
  "metadata": {
    "onboard_name": "<onboard_name>",
    "onboard_path": "<onboard_path>"
  },
  "steps": [
    {
      "type": "command",
      "stem": "<command_stem>",
      "args": [] || [ args1, args2, etc.],
      "time": {
        "type": "COMMAND_COMPLETE"
      } || {
        "tag": "YYYY-DDDThh:mm:ss.sss",
        "type": "ABSOLUTE"
      } || {
        "tag": "hh:mm:ss.sss",
        "type": "COMMAND_RELATIVE"
      } || {
        "tag": "hh:mm:ss.sss",
        "type": "EPOCH_RELATIVE"
      }
    }
  ]
}
```

### Real World Example with an Europa Clipper Command Dictionary

Here is an example expansion logic for a single Activity in a AERIE plan.

```js
REAS_TX_ENABLE("DIS_CHIRP_DIS_RF", "DIS_CHIRP_DIS_RF"),
SEQ_VAR_CMD({ dynamic_arg1: "dynamic_2", opcode: 2 }).absoluteTiming(
  Temporal.Instant.from("2022-04-20T20:17:13Z")
),
DDM_DMP_EHA_PERIODIC({
  criteria_ops_cat1: "ALL",
  channel_id1: 1,
  criteria_ops_cat2: "AVS",
  channel_id2: 2,
  eha_evaluation_rate: "8 hz",
  execution_duration: 3,
  dp_priority: 62,
}).relativeTiming(Temporal.Duration.from({ minutes: 5 })),
AVS_SS_READ_CHECK_EMEM_NAND(10, 10, 10, false, 10),
DDM_DMP_EHA_PERIODIC({
  channel_id2: 2,
  channel_id1: 1,
  criteria_ops_cat2: "AVS",
  criteria_ops_cat1: "ALL",
  eha_evaluation_rate: "8 hz",
  execution_duration: 3,
  dp_priority: 62,
}).epochTiming(Temporal.Duration.from({ minutes: 15 })),

```

Below is the Sequence JSON file generate by AERIE:

```json
{
  "id": "test0001",
  "metadata": {
    "onboard_name": "test.seq",
    "onboard_path": "/eng"
  },
  "steps": [
    {
      "type": "command",
      "stem": "REAS_TX_ENABLE",
      "args": ["DIS_CHIRP_DIS_RF", "DIS_CHIRP_DIS_RF"],
      "time": {
        "type": "COMMAND_COMPLETE"
      },
      "metadata": {}
    },
    {
      "type": "command",
      "stem": "SEQ_VAR_CMD",
      "args": ["dynamic_2", 2],
      "time": {
        "tag": "2022-110T20:17:13.000",
        "type": "ABSOLUTE"
      },
      "metadata": {}
    },
    {
      "type": "command",
      "stem": "DDM_DMP_EHA_PERIODIC",
      "args": ["ALL", 1, "AVS", 2, "8 hz", 3, 62],
      "time": {
        "tag": "00:05:00.000",
        "type": "COMMAND_RELATIVE"
      },
      "metadata": {}
    },
    {
      "type": "command",
      "stem": "AVS_SS_READ_CHECK_EMEM_NAND",
      "args": [10, 10, 10, 0, 10],
      "time": {
        "tag": "00:15:00.000",
        "type": "EPOCH_RELATIVE"
      },
      "metadata": {}
    }
  ]
}
```
