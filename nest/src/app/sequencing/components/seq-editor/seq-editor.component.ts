/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import * as CodeMirror from 'codemirror';
import { MpsCommand, StringTMap } from '../../../shared/models';
import {
  buildMpsHint,
  buildMpsLint,
  buildMpsMode,
} from '../../code-mirror-languages/mps';
import {
  getCommandParameterDescriptionTemplate,
  getCommandParameterHelpTemplate,
} from '../../code-mirror-languages/mps/helpers';
import { CurrentLine, Editor, EditorOptions, SequenceTab } from '../../models';
import { initialState } from '../../reducers/editor.reducer';
import { defaultEditorId } from '../../reducers/file.reducer';
import { SeqEditorService } from '../../services/seq-editor.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'seq-editor',
  styleUrls: ['./seq-editor.component.css'],
  templateUrl: `./seq-editor.component.html`,
})
export class SeqEditorComponent implements AfterViewInit, OnChanges {
  @Input()
  autofocus = true;

  @Input()
  commandsByName: StringTMap<MpsCommand> = {};

  @Input()
  commands: MpsCommand[] = [];

  @Input()
  currentLine: CurrentLine | null;

  @Input()
  extraKeys = {
    'Ctrl-Space': 'autocomplete',
  };

  @Input()
  gutters: string[] = ['CodeMirror-lint-markers'];

  @Input()
  lineNumbers = true;

  @Input()
  lineWrapping = true;

  @Input()
  mode = 'mps';

  @Input()
  value = '';

  @Input()
  file: SequenceTab | null;

  @Input()
  currentTab: string | null;

  @Input()
  editors: Editor[];

  @Input()
  editorState: Editor;

  @Input()
  editorOptions: EditorOptions;

  @Output()
  updateTab: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  setCurrentLine: EventEmitter<any> = new EventEmitter<any>();

  @ViewChild('editor', { static: true })
  editorMount: ElementRef;

  fullscreen = false;
  tooltip: HTMLElement;
  defaultTheme = 'monokai';

  editor: CodeMirror.Editor | null;

  constructor(private seqEditorService: SeqEditorService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.commands || changes.mode) {
      this.setMode();
    }

    if (this.editor) {
      if (changes.lineNumbers) {
        this.editor.setOption('lineNumbers', this.lineNumbers);
      }

      if (changes.lineWrapping) {
        this.editor.setOption('lineWrapping', this.lineWrapping);
      }

      if (changes.value) {
        this.editor.getDoc().setValue(this.value);
      }

      if (changes.currentTab) {
        const text = this.file ? this.file.text : '';

        this.editor.setValue(text);
        this.editor.focus();
      }

      if (changes.editorOptions) {
        const isDarkTheme = changes.editorOptions.currentValue.darkTheme;

        if (isDarkTheme) {
          this.editor.setOption('theme', 'monokai');
        } else {
          this.editor.setOption('theme', 'default');
        }
      }

      if (changes.currentLine) {
        this.updateCurrentLine();
      }
    }
  }

  ngAfterViewInit() {
    this.setupEditor();
    this.setupAutocomplete();
    this.setupTooltip();
    this.setupTooltipHandler();
    this.setupCommandFormEditor();
    this.populateEditors();
  }

  /**
   * Set Code Mirror mode and addons.
   * Order matters here! Make sure we register the mode first with Code Mirror
   * before registering the hinter and linter.
   */
  private setMode() {
    if (this.editor) {
      switch (this.mode) {
        case 'ait':
          // TODO.
          break;
        case 'mps':
        default:
          buildMpsMode(this.commandsByName);
          this.editor.setOption('mode', 'mps');
          buildMpsHint(this.commandsByName);
          buildMpsLint(this.commandsByName);
          break;
      }

      // We reset the linter so it clears previous lint messages
      // Useful for when switching between command dictionaries
      this.editor.setOption('lint', false);
      this.editor.setOption('lint', true);
    }
  }

  undo() {
    if (this.editor) {
      this.editor.execCommand('undo');
      this.editor.focus();
    }
  }

  redo() {
    if (this.editor) {
      this.editor.execCommand('redo');
      this.editor.focus();
    }
  }

  toggleFullscreen() {
    if (this.editor) {
      this.fullscreen = !this.fullscreen;
      this.editor.setOption('fullScreen', this.fullscreen);
      this.editor.focus();
    }
  }

  find() {
    if (this.editor) {
      this.editor.execCommand('find');
    }
  }

  replace() {
    if (this.editor) {
      this.editor.execCommand('replace');
    }
  }

  /**
   * Mounts and sets up the CodeMirror instance
   * Each CodeMirror instance is assigned an ID
   * The store is updated when the text changes
   */
  setupEditor() {
    const id = (this.editorState && this.editorState.id) || defaultEditorId;

    this.seqEditorService.setEditor(this.editorMount, id, {
      autofocus: this.autofocus,
      extraKeys: {
        ...this.extraKeys,
        'Ctrl-/': this.toggleComment.bind(this),
        'Ctrl-R': this.redo.bind(this),
        'Ctrl-Z': this.undo.bind(this),
        Esc: this.toggleFullscreen.bind(this),
      },
      gutters: this.gutters,
      lineNumbers: this.lineNumbers,
      lineWrapping: this.lineWrapping,
      lint: true,
      mode: this.mode,
      theme: this.defaultTheme,
      value: this.value,
    });

    this.editor = this.seqEditorService.getEditor(id);

    if (this.editor) {
      this.editor.on('change', () => {
        if (this.editor && this.file) {
          this.onUpdateTab(
            this.file.id,
            this.editor.getValue(),
            this.editorState.id,
          );
        }
      });
    }
  }

  setupAutocomplete() {
    if (this.editor) {
      // Disables the autocomplete from automatically autocompleting when there is only 1 option
      this.editor.setOption('hintOptions', {
        completeSingle: false,
      });

      this.editor.on('keyup', (cm: CodeMirror.Editor, event: KeyboardEvent) => {
        // Don't open autocomplete menu again if the menu is open
        // and if user is moving up/down options
        if (
          this.editorOptions.autocomplete &&
          this.editor &&
          !cm.state.completeActive &&
          event.key !== 'Enter' &&
          event.key !== 'ArrowDown' &&
          event.key !== 'ArrowUp'
        ) {
          // @ts-ignore: CodeMirror types are not up to date, doesn't have getCursor()
          const cursor = this.editor.getCursor();
          const currentLine = this.editor.getLineTokens(cursor.line);

          // Only show the autocomplete prompt if there is at least 1 character
          if (currentLine.length > 0) {
            this.editor.execCommand('autocomplete');
          }
        }
      });
    }
  }

  setupTooltip() {
    this.tooltip = document.createElement('div');
    this.tooltip.className = 'CodeMirror-tooltip';
    document.body.appendChild(this.tooltip);
    this.tooltip.addEventListener('mouseleave', () => {
      this.showTooltip(false);
    });
  }

  setupTooltipHandler() {
    if (this.editor) {
      this.editor
        .getWrapperElement()
        .addEventListener('mousemove', (e: MouseEvent) => {
          // If there is an error tooltip, don't show the description tooltip
          const errorTooltip = document.querySelector(
            '.CodeMirror-lint-tooltip',
          );

          if (
            this.commandsByName &&
            this.editorOptions.showTooltips &&
            this.editor &&
            !errorTooltip
          ) {
            // Used to give a tolerance for the mouse to be nearby the text, mouse doesn't have to be exactly on the text
            const nearby = [0, 0, 0, 5, 0, -5, 5, 0, -5, 0];
            // tslint:disable-next-line: deprecation
            const node = (e.target || e.srcElement) as HTMLElement;
            const text = node.innerText || node.textContent;
            const cm = this.editor;
            let target;

            // Check nearby positions to see if text is nearby mouse
            for (let i = 0, length = nearby.length; i < length; i += 2) {
              const pos = cm.coordsChar({
                left: e.clientX + nearby[i],
                top: e.clientY + nearby[i + 1],
              });

              // CodeMirror token that contains the context
              const token = cm.getTokenAt(pos);

              if (token && token.string === text) {
                target = {
                  pos,
                  token,
                };

                // Hides the tooltip when the user moves off the token
                node.addEventListener(
                  'mouseleave',
                  (mouseLeaveEvent: MouseEvent) => {
                    const hoverTarget = mouseLeaveEvent.relatedTarget as HTMLElement;

                    if (hoverTarget.className !== 'CodeMirror-tooltip') {
                      this.showTooltip(false);
                      node.removeEventListener('mouseleave', () => {});
                    }
                  },
                );
                break;
              }
            }

            if (target && target.token.string !== '') {
              const commandName = target.token.string;
              const commandObject = this.commandsByName[commandName];

              if (commandObject) {
                // If the mouse is on a command
                const tooltipMessage = getCommandParameterHelpTemplate(
                  commandName,
                  this.commandsByName,
                );
                this.tooltip.innerHTML = tooltipMessage;
                this.positionTooltip(node);

                this.showTooltip(true);
              } else if (target.token.string !== '') {
                // If the mouse is on an argument
                // CodeMirror indexes line numbers starting at 0, need to offset
                const hoveredParameter = target.token.string;

                const currentLine = target.pos.line;
                const lineTokens = cm.getLineTokens(currentLine);

                const newTooltipContent = getCommandParameterDescriptionTemplate(
                  lineTokens[0].string,
                  hoveredParameter,
                  lineTokens,
                  this.commandsByName,
                );

                this.tooltip.innerHTML = newTooltipContent;
                this.positionTooltip(node);
                this.showTooltip(true);
              } else {
                // If the mouse is on blank space
                this.showTooltip(false);
              }
            }
          }
        });
    }
  }

  /**
   * Sets up the event listener for the cursor
   * When the cursor moves, it will check the current line for it's content
   * This is used for the parameter form editor
   */
  setupCommandFormEditor() {
    if (this.editor) {
      this.editor.on('cursorActivity', (doc: CodeMirror.Editor) => {
        const currentLine = this.seqEditorService.getCurrentLine(
          this.editorState.id,
        );

        // If the line is blank, don't do anything
        if (currentLine.length === 0) {
          this.setCurrentLine.emit(initialState);
          return;
        }

        const commandName = currentLine[0].string.trim();

        // If the line content is the same as the previous state, don't do anything
        if (this.currentLine && commandName === this.currentLine.commandName)
          return;

        const commandDescription = this.commandsByName[commandName];

        // This happens when
        // 1. The user is typing
        if (!commandDescription) return;

        const parameterDefinitions = commandDescription.parameters;
        // Index 0 contains the command name, we exclude so we can line up the parameter definitions with the actual parameters
        const lineParameters = currentLine
          .filter(token => token.string !== ' ')
          .slice(1);

        const formPayload: CurrentLine = {
          commandName,
          parameters: [],
        };

        // Transform the line into the expected shape
        for (let i = 0, length = parameterDefinitions.length; i < length; i++) {
          const currentParameter = parameterDefinitions[i];
          const { name, type, units, help } = currentParameter;
          const value = lineParameters[i].string;

          if (name) {
            formPayload['parameters'].push({
              help,
              name,
              type,
              units,
              value,
            });
          }
        }

        this.setCurrentLine.emit(formPayload);
      });
    }
  }
  /**
   * Used to position the hover tooltips near the mouse
   */
  positionTooltip(node: HTMLElement) {
    const target_rect = node.getBoundingClientRect();
    const tooltip_rect = this.tooltip.getBoundingClientRect();
    const yOffset = -20;

    if (tooltip_rect.height <= target_rect.top) {
      this.tooltip.style.top =
        target_rect.top - tooltip_rect.height + yOffset + 'px';
    } else {
      this.tooltip.style.top = target_rect.bottom + 'px';
    }
    this.tooltip.style.left = target_rect.left + 'px';
  }

  /**
   * Used to show/hide the hover tooltips
   * We are using setTimeout to remove the tooltip from the DOM so
   * it doesn't block MouseEvents for displaying tooltips for other tokens
   */
  private showTooltip(state: boolean) {
    if (state) {
      this.tooltip.style.opacity = '1';
      this.tooltip.style.visibility = 'visible';
    } else {
      this.tooltip.style.opacity = '0';
      this.tooltip.style.visibility = 'hidden';
    }
  }

  onUpdateTab(id: string, text: string, editorId: string) {
    this.updateTab.emit({ id, text, editorId });
  }

  toggleComment() {
    if (this.editor) {
      this.editor.execCommand('toggleComment');
    }
  }

  /**
   * Fills the editors with the opened files values
   * Use Case: A user edits files then switches to another app
   * When the user comes back, the editors need to be repopulated with the previous text
   */
  private populateEditors() {
    if (this.file) {
      this.onUpdateTab(this.file.id, this.file.text, this.editorState.id);
    }
  }

  /**
   * Called when the parameter form is used to update the parameter values
   */
  updateCurrentLine() {
    if (this.currentLine) {
      const { commandName, parameters } = this.currentLine;
      if (parameters) {
        const newLine = `${commandName} ${parameters
          .map(parameter => {
            return `${parameter.value}`;
          })
          .join(' ')
          .split(',')
          .join(' ')}`.trim();
        this.seqEditorService.replaceText(newLine, this.editorState.id);
      }
    }
  }
}
