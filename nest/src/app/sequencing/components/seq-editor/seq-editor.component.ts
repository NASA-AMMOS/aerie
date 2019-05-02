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
  filename = 'FileName.mps';

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

  @Output()
  openHelpDialog: EventEmitter<null> = new EventEmitter<null>();

  @ViewChild('editor')
  editorMount: ElementRef;

  autocomplete = false;
  fullscreen = false;
  userTheme = 'dark';
  tooltip: HTMLElement;
  showTooltips = false;

  constructor(private seqEditorService: SeqEditorService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.commandsByName || changes.mode) {
      this.setMode();
    }

    if (this.seqEditorService.editor && changes.lineNumbers) {
      this.seqEditorService.editor.setOption('lineNumbers', this.lineNumbers);
    }

    if (this.seqEditorService.editor && changes.lineWrapping) {
      this.seqEditorService.editor.setOption('lineWrapping', this.lineWrapping);
    }

    if (this.seqEditorService.editor && changes.theme) {
      this.seqEditorService.editor.setOption('theme', this.theme);
    }

    if (this.seqEditorService.editor && changes.value) {
      this.seqEditorService.editor.getDoc().setValue(this.value);
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
    if (this.seqEditorService.editor) {
      switch (this.mode) {
        case 'ait':
          // TODO.
          break;
        case 'mps':
        default:
          buildMpsMode(this.commandsByName);
          this.seqEditorService.editor.setOption('mode', 'mps');
          buildMpsHint(this.commandsByName);
          buildMpsLint(this.commandsByName);
          break;
      }
    }
  }

  undo() {
    if (this.seqEditorService.editor) {
      this.seqEditorService.editor.execCommand('undo');
      this.seqEditorService.editor.focus();
    }
  }

  redo() {
    if (this.seqEditorService.editor) {
      this.seqEditorService.editor.execCommand('redo');
      this.seqEditorService.editor.focus();
    }
  }

  toggleAutocomplete() {
    if (this.seqEditorService.editor) {
      this.autocomplete = !this.autocomplete;
      this.seqEditorService.editor.focus();
    }
  }

  toggleFullscreen() {
    if (this.seqEditorService.editor) {
      this.fullscreen = !this.fullscreen;
      this.seqEditorService.editor.setOption('fullScreen', this.fullscreen);
      this.seqEditorService.editor.focus();
    }
  }

  toggleTheme() {
    if (this.seqEditorService.editor) {
      switch (this.userTheme) {
        case 'dark':
          this.userTheme = 'light';
          this.seqEditorService.editor.setOption('theme', 'default');
          break;
        case 'light':
          this.userTheme = 'dark';
          this.seqEditorService.editor.setOption('theme', 'monokai');
          break;
        default:
          this.userTheme = 'dark';
          this.seqEditorService.editor.setOption('theme', 'monokai');
      }
      this.seqEditorService.editor.focus();
    }
  }

  toggleTooltips() {
    if (this.seqEditorService.editor) {
      this.showTooltips = !this.showTooltips;
      this.seqEditorService.editor.focus();
    }
  }

  setupEditor() {
    this.seqEditorService.setEditor(this.editorMount, {
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
  }

  setupAutocomplete() {
    if (this.seqEditorService.editor) {
      // Disables the autocomplete from automatically autocompleting when there is only 1 option
      this.seqEditorService.editor.setOption('hintOptions', {
        completeSingle: false,
      });

      this.seqEditorService.editor.on(
        'keyup',
        (cm: CodeMirror.Editor, event: KeyboardEvent) => {
          // Don't open autocomplete menu again if the menu is open
          // and if user is moving up/down options
          if (
            this.autocomplete &&
            this.seqEditorService.editor &&
            !cm.state.completeActive &&
            event.key !== 'Enter' &&
            event.key !== 'ArrowDown' &&
            event.key !== 'ArrowUp'
          ) {
            this.seqEditorService.editor.execCommand('autocomplete');
          }
        },
      );
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
    if (this.seqEditorService.editor) {
      this.seqEditorService.editor
        .getWrapperElement()
        .addEventListener('mousemove', (e: MouseEvent) => {
          if (
            this.commandsByName &&
            this.showTooltips &&
            this.seqEditorService.editor
          ) {
            // Used to give a tolerance for the mouse to be nearby the text, mouse doesn't have to be exactly on the text
            const nearby = [0, 0, 0, 5];
            // tslint:disable-next-line: deprecation
            const node = (e.target || e.srcElement) as HTMLElement;
            const text = node.innerText || node.textContent;
            const cm = this.seqEditorService.editor;
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
}
