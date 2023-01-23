import { CommandStem, Sequence } from './lib/codegen/CommandEDSLPreface.js';
import type { SeqBuilder } from './types/seqBuilder';

export const defaultSeqBuilder: SeqBuilder = (
  sortedActivityInstancesWithCommands,
  seqId,
  seqMetadata,
  simulationDatasetId,
) => {
  let planId;

  const commands = sortedActivityInstancesWithCommands.flatMap(ai => {
    // No associated Expansion
    if (ai.errors === null) {
      return [];
    }

    planId = ai?.simulationDataset.simulation?.planId;

    if (ai.errors.length > 0) {
      return ai.errors.map(e =>
        CommandStem.new({
          stem: '$$ERROR$$',
          arguments: [e.message],
          metadata: {
            simulatedActivityId: ai.id,
          },
        }),
      );
    }
    // Typeguard only
    if (ai.commands === null) {
      return [];
    }
    return ai.commands;
  });

  return Sequence.new({
    seqId: seqId,
    metadata: {
      ...seqMetadata,
      planId,
      simulationDatasetId,
    },
    commands,
  });
};
