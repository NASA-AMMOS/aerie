import { CommandStem, Sequence } from './lib/codegen/CommandEDSLPreface.js';
import type { SeqBuilder } from './types/seqBuilder';

export const defaultSeqBuilder: SeqBuilder = (
  sortedActivityInstancesWithCommands,
  seqId,
  seqMetadata,
  simulationDatasetId,
) => {
  let planId;
  let timeSorted = false;

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
          arguments: [{ message: e.message }],
        }).METADATA({ simulatedActivityId: ai.id }),
      );
    }

    // Typeguard only
    if (ai.commands === null) {
      return [];
    }

    const result = sortCommandsByTime(ai.commands);
    timeSorted = result.timeSorted;

    return result.sortedCommands;
  });

  return Sequence.new({
    seqId: seqId,
    metadata: {
      ...seqMetadata,
      planId,
      simulationDatasetId,
      timeSorted,
    },
    steps: commands,
  });
};

// Order commands by time in expanded sequences.
function sortCommandsByTime(commands: CommandStem<{} | []>[]): {
  sortedCommands: CommandStem<{} | []>[];
  timeSorted: boolean;
} {
  const relativeTimeTracker: Record<string, Temporal.Instant> = {};
  let previousAbsoluteTimestamp: Temporal.Instant | null = null;

  if (areAllEpochRelative(commands)) {
    return {
      sortedCommands: commands.sort((a, b) => {
        // Check for null values here, but areAllEpochRelative already checked to make sure they're not null.
        if (a.epochTime !== null && b.epochTime !== null) {
          return Temporal.Duration.compare(a.epochTime, b.epochTime);
        }

        return 0;
      }),
      timeSorted: true,
    };
  }

  // If we have mixed time commands then we only sort if they're all absolute and relative.
  for (const command of commands) {
    // If the command is epoch-relative, complete, or the first command and relative then short circuit and don't try and sort.
    if (
      command.epochTime !== null ||
      (command.absoluteTime === null && command.epochTime === null && command.relativeTime === null) ||
      (command.relativeTime !== null && commands.indexOf(command) === 0)
    ) {
      return { sortedCommands: commands, timeSorted: false };
    }

    // Keep track of the previously seen absolute time so we can convert the next relative timestamp we come across.
    if (command.absoluteTime !== null) {
      previousAbsoluteTimestamp = command.absoluteTime;
    } else if (command.relativeTime !== null && previousAbsoluteTimestamp !== null) {
      relativeTimeTracker[command.stem + command.relativeTime?.toString()] = previousAbsoluteTimestamp.add(
        command.relativeTime,
      );
    }
  }

  // The commands will either be absolute or relative at this point, so we just compare their times.
  commands.sort((a, b) => {
    const firstCommandTime = a.absoluteTime ?? relativeTimeTracker[a.stem + a.relativeTime?.toString()];
    const secondCommandTime = b.absoluteTime ?? relativeTimeTracker[b.stem + b.relativeTime?.toString()];

    if (firstCommandTime !== undefined && secondCommandTime !== undefined) {
      return Temporal.Instant.compare(firstCommandTime, secondCommandTime);
    }

    return 0;
  });

  return { sortedCommands: commands, timeSorted: true };
}

/**
 * Checks the commands and returns true if they're all epoch-relative.
 *
 * @param commands The commands we're trying to sort.
 * @returns true if all commands are epoch-relative, otherwise false.
 */
function areAllEpochRelative(commands: CommandStem<{} | []>[]): boolean {
  for (const command of commands) {
    if (command.epochTime === null) {
      return false;
    }
  }

  return true;
}
