import { CommandStem, Sequence } from './lib/codegen/CommandEDSLPreface.js';
import type { SeqBuilder } from './types/seqBuilder';

export const defaultSeqBuilder: SeqBuilder = (
  sortedActivityInstancesWithCommands,
  seqId,
  seqMetadata,
  simulationDatasetId,
) => {
  const convertedCommands: CommandStem<{} | []>[] = [];

  // A combined list of all the activity instance commands.
  let allCommands: CommandStem<{} | []>[] = [];
  let activityInstaceCount = 0;
  let planId;
  // Keep track if we should try and sort the commands.
  let shouldSort = true;
  let timeSorted = false;
  let previousTime: Temporal.Instant | null = null;

  for (const ai of sortedActivityInstancesWithCommands) {
    // If errors, no associated Expansion
    if (ai.errors !== null) {
      planId = ai?.simulationDataset.simulation?.planId;

      if (ai.errors.length > 0) {
        allCommands = allCommands.concat(
          ai.errors.map(e =>
            CommandStem.new({
              stem: '$$ERROR$$',
              arguments: [{ message: e.message }],
            }).METADATA({ simulatedActivityId: ai.id }),
          ),
        );
      }

      // Typeguard only
      if (ai.commands === null) {
        break;
      }

      /**
       * Look at the first command for each activity and check if it's relative, if so we shouldn't
       * sort later. Also convert any relative commands to absolute.
       */
      for (const command of ai.commands) {
        // If the command is epoch-relative, complete, or the first command and relative then short circuit and don't try and sort.
        if (
          command.epochTime !== null ||
          (command.absoluteTime === null && command.epochTime === null && command.relativeTime === null) ||
          (command.relativeTime !== null && ai.commands.indexOf(command) === 0)
        ) {
          shouldSort = false;
          break;
        }

        // If we come across a relative command, convert it to absolute.
        if (command.relativeTime !== null && previousTime !== null) {
          convertedCommands.push(command.absoluteTiming(previousTime.add(command.relativeTime)));
        } else {
          convertedCommands.push(command);
          previousTime = command.absoluteTime;
        }
      }

      allCommands = allCommands.concat(ai.commands);
      // Keep track of the number of times we add commands to the allCommands list.
      activityInstaceCount++;
    }
  }

  // Now that we have all the commands for each activity instance, sort if there's > 1 activity instance and we didn't short circuit above.
  if (activityInstaceCount > 1 && shouldSort) {
    timeSorted = true;
    allCommands = convertedCommands.sort((a, b) => {
      if (a.absoluteTime !== null && b.absoluteTime !== null) {
        return Temporal.Instant.compare(a.absoluteTime, b.absoluteTime);
      }

      return 0;
    });
  }

  return Sequence.new({
    seqId: seqId,
    metadata: {
      ...seqMetadata,
      planId,
      simulationDatasetId,
      timeSorted,
    },
    steps: allCommands,
  });
};
