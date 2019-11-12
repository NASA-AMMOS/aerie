import { TestBed } from '@angular/core/testing';
import { Actions } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { Action } from '@ngrx/store';
import { ToastrModule, ToastrService } from 'ngx-toastr';
import { Observable, of } from 'rxjs';
import { ToastActions } from '../actions';
import { ToastEffects } from './toast.effects';

describe('toast effects', () => {
  let actions: Observable<Action>;
  let effects: ToastEffects;
  let toastrService: ToastrService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ToastrModule.forRoot()],
      providers: [ToastEffects, provideMockActions(() => actions)],
    });
    actions = TestBed.inject(Actions);
    effects = TestBed.inject(ToastEffects);
    toastrService = TestBed.inject(ToastrService);
  });

  describe('showToast', () => {
    it('a success showToast action should call the toaster service success function', () => {
      actions = of(
        ToastActions.showToast({
          message: 'yay',
          toastType: 'success',
        }),
      );
      spyOn(toastrService, 'success');
      effects.showToast.subscribe();
      expect(toastrService.success).toHaveBeenCalled();
    });
  });
});
