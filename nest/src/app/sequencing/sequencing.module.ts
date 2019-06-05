/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { NgModule } from '@angular/core';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { NestConfirmDialogModule } from '../shared/components';
import { SequencingAppModule } from './containers/sequencing-app/sequencing-app.module';
import { CommandDictionaryEffects } from './effects/command-dictionary.effects';
import { EditorEffects } from './effects/editor.effects';
import { FileEffects } from './effects/file.effects';
import { SequencingRoutingModule } from './sequencing-routing.module';
import { reducers } from './sequencing-store';

@NgModule({
  imports: [
    SequencingRoutingModule,
    StoreModule.forFeature('sequencing', reducers),
    EffectsModule.forFeature([
      CommandDictionaryEffects,
      EditorEffects,
      FileEffects,
    ]),
    NestConfirmDialogModule,
    SequencingAppModule,
  ],
})
export class SequencingModule {}
