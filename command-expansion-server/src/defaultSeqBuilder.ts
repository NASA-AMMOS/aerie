import { Command, Sequence } from './lib/codegen/CommandEDSLPreface.js';
import type { SeqBuilder } from './app.js';

export const defaultSeqBuilder: SeqBuilder = (sortedActivityInstancesWithCommands, seqId, seqMetadata) => {
  const commands = sortedActivityInstancesWithCommands.flatMap(ai => {
    // No associated Expansion
    if (ai.errors === null) {
      return [];
    }

    if (ai.errors.length > 0) {
      return ai.errors.map(e => Command.new({
        stem: '$$ERROR$$',
        arguments: [e.message],
        metadata: {
          simulatedActivityId: ai.id
        },
      }));
    }
    // Typeguard only
    if (ai.commands === null) {
      return [];
    }
    return ai.commands;
  });

  return Sequence.new({
    seqId: seqId,
    metadata: seqMetadata,
    commands,
  });
};
