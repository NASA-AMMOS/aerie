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
import { FloatingActionButton } from 'ng-floating-action-menu';
import { v4 as uuid } from 'uuid';
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
import { SequenceTab } from '../../models';
import { Editor } from '../../reducers/file.reducer';
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
  theme = 'monokai';

  @Input()
  value = '';

  @Input()
  file: SequenceTab | null;

  @Input()
  currentTab: string | null;

  @Input()
  editors: any;

  @Input()
  editorState: Editor;

  @Output()
  openHelpDialog: EventEmitter<null> = new EventEmitter<null>();

  @Output()
  updateTab: EventEmitter<any> = new EventEmitter<any>();

  @ViewChild('editor', { static: true })
  editorMount: ElementRef;

  autocomplete = true;
  fullscreen = false;
  userTheme = 'dark';
  tooltip: HTMLElement;
  showTooltips = false;
  buttons: Array<FloatingActionButton> = [
    {
      iconClass: 'fa fa-undo',
      label: 'Undo',
      onClick: () => this.undo(),
    },
    {
      iconClass: 'fa fa-repeat',
      label: 'Redo',
      onClick: () => this.redo(),
    },
    {
      iconClass: 'fa fa-magic',
      label: 'Toggle Autocomplete Off',
      onClick: () => this.toggleAutocomplete(),
    },
    {
      iconClass: 'fa fa-paint-brush',
      label: 'Color Scheme',
      onClick: () => this.toggleTheme(),
    },
    {
      iconClass: 'fa fa-arrows-alt',
      label: 'Toggle Fullscreen',
      onClick: () => this.toggleFullscreen(),
    },
    {
      iconClass: 'fa fa-info-circle',
      label: 'Toggle Tooltips On',
      onClick: () => this.toggleTooltips(),
    },
    {
      iconClass: 'fa fa-question',
      label: 'Help',
      onClick: () => this.openHelpDialog.emit(),
    },
  ];
  editor: CodeMirror.Editor | null;

  constructor(private seqEditorService: SeqEditorService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.commandsByName || changes.mode) {
      this.setMode();
    }

    if (this.editor) {
      if (changes.lineNumbers) {
        this.editor.setOption('lineNumbers', this.lineNumbers);
      }

      if (changes.lineWrapping) {
        this.editor.setOption('lineWrapping', this.lineWrapping);
      }

      if (changes.theme) {
        this.editor.setOption('theme', this.theme);
      }

      if (changes.value) {
        this.editor.getDoc().setValue(this.value);
      }

      // Updates the view of the text value if the file is being edited elsewhere
      // if (changes.file && this.file) {
      //   this.editor.getDoc().setValue(this.file.text);
      //   // Moves the cursor to the end of the line
      //   // Original behavior makes the cursor move to the beginning of the line
      //   this.editor.getDoc().setCursor(this.editor.getDoc().lineCount(), 0);
      // }

      if (changes.currentTab) {
        const text = this.file ? this.file.text : '';

        this.editor.setValue(text);
      }
    }
  }

  ngAfterViewInit() {
    this.setupEditor();
    this.setupAutocomplete();
    this.setupTooltip();
    this.setupTooltipHandler();
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
    }
  }

  undo() {
    console.debug('undo', this.editor);
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

  toggleAutocomplete() {
    if (this.editor) {
      this.autocomplete = !this.autocomplete;

      if (this.autocomplete) {
        this.buttons[2].label = 'Toggle Autocomplete Off';
      } else {
        this.buttons[2].label = 'Toggle Autocomplete On';
      }
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

  toggleTheme() {
    if (this.editor) {
      switch (this.userTheme) {
        case 'dark':
          this.userTheme = 'light';
          this.editor.setOption('theme', 'default');
          break;
        case 'light':
          this.userTheme = 'dark';
          this.editor.setOption('theme', 'monokai');
          break;
        default:
          this.userTheme = 'dark';
          this.editor.setOption('theme', 'monokai');
      }
      this.editor.focus();
    }
  }

  toggleTooltips() {
    if (this.editor) {
      this.showTooltips = !this.showTooltips;

      if (this.showTooltips) {
        this.buttons[5].label = 'Toggle Tooltips Off';
      } else {
        this.buttons[5].label = 'Toggle Tooltips On';
      }
      this.editor.focus();
    }
  }

  /**
   * Mounts and sets up the CodeMirror instance
   * Each CodeMirror instance is assigned an ID
   * The store is updated when the text changes
   */
  setupEditor() {
    const id = uuid();

    this.seqEditorService.setEditor(this.editorMount, id, {
      autofocus: this.autofocus,
      extraKeys: {
        ...this.extraKeys,
        'Ctrl-R': this.redo.bind(this),
        'Ctrl-Z': this.undo.bind(this),
        Esc: this.toggleFullscreen.bind(this),
      },
      gutters: this.gutters,
      lineNumbers: this.lineNumbers,
      lineWrapping: this.lineWrapping,
      lint: true,
      mode: this.mode,
      theme: this.theme,
      value: this.value,
    });

    this.editor = this.seqEditorService.editors[id];

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
          this.autocomplete &&
          this.editor &&
          !cm.state.completeActive &&
          event.key !== 'Enter' &&
          event.key !== 'ArrowDown' &&
          event.key !== 'ArrowUp'
        ) {
          this.editor.execCommand('autocomplete');
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
          if (this.commandsByName && this.showTooltips && this.editor) {
            // Used to give a tolerance for the mouse to be nearby the text, mouse doesn't have to be exactly on the text
            const nearby = [0, 0, 0, 5];
            // tslint:disable-next-line: deprecation
            const node = (e.target || e.srcElement) as HTMLElement;
            const text = node.innerText || node.textContent;
            const cm = this.editor;
            let target;

            // Check nearby positions to see if text is nearby mouse
            for (let i = 0; i < nearby.length; i += 2) {
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

                const currentLine = target.pos.line + 1;
                const lineTokens = cm.getLineTokens(currentLine);

                const newTooltipContent = getCommandParameterDescriptionTemplate(
                  lineTokens[0].string,
                  hoveredParameter,
                  lineTokens,
                  this.commandsByName,
                );

                // Only update the content if it's different than the current content
                // Handles the case where the user is moving their mouse over the same token
                if (this.tooltip.innerHTML !== newTooltipContent) {
                  this.tooltip.innerHTML = newTooltipContent;
                  this.positionTooltip(node);
                  this.showTooltip(true);
                }
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
   * Used to position the hover tooltips near the mouse
   */
  positionTooltip(node: HTMLElement) {
    const target_rect = node.getBoundingClientRect();
    const tooltip_rect = this.tooltip.getBoundingClientRect();

    if (tooltip_rect.height <= target_rect.top) {
      this.tooltip.style.top =
        target_rect.top - tooltip_rect.height + 20 + 'px';
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
      setTimeout(() => {
        this.tooltip.style.display = 'block';
      }, 250);
    } else {
      this.tooltip.style.opacity = '0';
      setTimeout(() => {
        this.tooltip.style.display = 'none';
      }, 250);
    }
  }

  onUpdateTab(id: string, text: string, editorId: string) {
    this.updateTab.emit({ id, text, editorId });
  }
}
