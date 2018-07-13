/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { v4 as uuidv4 } from 'uuid';

import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  Inject,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';

import {
  HttpClient,
} from '@angular/common/http';

import {
  FormControl,
  Validators,
} from '@angular/forms';

import {
  MAT_DIALOG_DATA,
  MatDialogRef,
} from '@angular/material';

import {
  combineLatest,
  Subject,
} from 'rxjs';

import {
  map,
  takeUntil,
} from 'rxjs/operators';

import {
  AppState,
} from './../../../../store';

import {
  MpsServerSource,
} from './../../../models';

import {
  getState,
} from './../../../util/state';

@Component({
  selector: 'raven-shareable-link-dialog',
  styleUrls: ['./raven-shareable-link-dialog.component.css'],
  templateUrl: './raven-shareable-link-dialog.component.html',
})
export class RavenShareableLinkDialogComponent implements AfterViewInit, OnDestroy, OnInit {
  @ViewChild('shareableLinkInput') shareableLinkInput: ElementRef;
  @ViewChild('shareableNameInput') shareableNameInput: ElementRef;

  invalidShareableName = false;
  overwriteWarning = false;
  shareableLinkControl: FormControl = new FormControl('', []);
  shareableNameControl: FormControl = new FormControl('', [
    Validators.required,
    Validators.pattern('^([(a-zA-Z0-9\-\_\s)]*){1,30}$'),
  ]);
  titleMessage = 'Copy Shareable Link';

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    public dialogRef: MatDialogRef<RavenShareableLinkDialogComponent>,
    public http: HttpClient,
    @Inject(MAT_DIALOG_DATA) public data: { state: AppState },
  ) {
    combineLatest(
      this.shareableNameControl.valueChanges,
      this.http.get(this.getStateUrl()),
    ).pipe(
      map(([value, sources]) => ({ value, sources })),
      takeUntil(this.ngUnsubscribe),
    ).subscribe(({ value, sources }) => {
      this.shareableLinkControl.setValue(this.getShareableLink(value));

      const children = (sources as MpsServerSource[]).map(source => source.name);

      // If the current source has a child with the name we are trying to save,
      // then display a proper overwrite warning.
      if (children.find(name => name === value)) {
        this.overwriteWarning = true;
      } else {
        this.overwriteWarning = false;
      }

      // This is to make sure the input field turns red if the user deletes the text.
      // If we dont blur and focus we don't see the input turn red right away.
      if (value === '') {
        this.shareableNameInput.nativeElement.blur();
        this.shareableNameInput.nativeElement.focus();
      }

      if (this.shareableNameControl.valid) {
        this.invalidShareableName = false;
      } else {
        this.invalidShareableName = true;
      }
    });
  }

  /**
   * Listens for a copy event on the document.
   * If our input fields are valid and the shareable link input is focused then we post the current state to the server for the given link, and then close the dialog.
   */
  @HostListener('copy', ['$event'])
  onCopy() {
    if (
      this.shareableLinkControl.valid &&
      this.shareableNameControl.valid &&
      this.shareableLinkInput.nativeElement === document.activeElement
    ) {
      this.titleMessage = 'Link text copied!';

      this.http.put(
        `${this.getStateUrl(this.shareableNameControl.value)}?timeline_type=state`,
        getState(this.data.state),
      ).pipe(
        takeUntil(this.ngUnsubscribe),
      ).subscribe(() =>
        this.dialogRef.close(),
      );
    }
  }

  ngAfterViewInit() {
    // Auto-select the shareable link text when the dialog opens so the user can copy it quickly.
    this.shareableLinkInput.nativeElement.select();
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  ngOnInit() {
    const initialShareableName = uuidv4();
    this.shareableLinkControl.setValue(this.getShareableLink(initialShareableName));
    this.shareableNameControl.setValue(initialShareableName);
  }

  /**
   * Event. Called when the cancel button is clicked.
   */
  onCancel() {
    this.dialogRef.close();
  }

  /**
   * Helper that returns the sharable link text.
   */
  getShareableLink(name: string): string {
    const { baseRavenUrl, baseUrl, shareableLinkStatesUrl } = this.data.state.config;

    const url = `${baseUrl}${baseRavenUrl ? '/' + baseRavenUrl : ''}/#/timeline`;
    const query = `state=/${shareableLinkStatesUrl}/${name}&&layout=minimal`;

    return `${url}?${query}`;
  }

  /**
   * Returns the location of the saved state from the sharable link.
   * Note this is different from the actual sharable link as the state url points to the actual state in the database.
   */
  getStateUrl(sharableName?: string) {
    const { baseSourcesUrl, baseUrl, shareableLinkStatesUrl } = this.data.state.config;
    return `${baseUrl}/${baseSourcesUrl}/${shareableLinkStatesUrl}${sharableName ? '/' + sharableName : ''}`.replace(/fs/, 'fs-mongodb');
  }

  /**
   * Event. Callback when shareable link text box is clicked. Auto-selects it so the user can copy it quickly.
   */
  onShareableLinkClick() {
    this.shareableLinkInput.nativeElement.select();
  }
}
