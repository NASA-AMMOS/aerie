import type { UserCodeError } from '@nasa-jpl/aerie-ts-user-code-runner';

import type { Command, Sequence } from '../lib/codegen/CommandEDSLPreface.js';
import type { SimulatedActivity } from '../lib/batchLoaders/simulatedActivityBatchLoader';

export interface SeqBuilder {
  (
    sortedActivityInstancesWithCommands: (SimulatedActivity & {
      commands: Command[] | null;
      errors: ReturnType<UserCodeError['toJSON']>[] | null;
    })[],
    seqId: string,
    seqMetadata: Record<string, any>,
    simulationDatasetId: number,
  ): Sequence;
}
