///
/// Copyright © 2024, Kanton Bern
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without
/// modification, are permitted provided that the following conditions are met:
///     * Redistributions of source code must retain the above copyright
///       notice, this list of conditions and the following disclaimer.
///     * Redistributions in binary form must reproduce the above copyright
///       notice, this list of conditions and the following disclaimer in the
///       documentation and/or other materials provided with the distribution.
///     * Neither the name of the <organization> nor the
///       names of its contributors may be used to endorse or promote products
///       derived from this software without specific prior written permission.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
/// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
/// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
/// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
/// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
/// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
/// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
///

import {AppInfoService, ScreenService} from './app/shared/services';
import {importProvidersFrom, LOCALE_ID} from '@angular/core';
import {environment} from './environments/environment';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {TokenInterceptor} from './app/shared/interceptor/token-interceptor.service';
import {ConfirmationService, MessageService} from 'primeng/api';
import {AsyncPipe, CommonModule, JsonPipe, registerLocaleData, TitleCasePipe} from '@angular/common';
import {providePrimeNG} from 'primeng/config';
import {StoreModule} from '@ngrx/store';
import {appReducers} from './app/store/app/app.reducer';
import {EffectsModule} from '@ngrx/effects';
import {appEffects} from './app/store/app/app.effects';
import {StoreDevtoolsModule} from '@ngrx/store-devtools';
import {bootstrapApplication, BrowserModule} from '@angular/platform-browser';
import {provideAnimations} from '@angular/platform-browser/animations';
import {TranslocoRootModule} from './app/transloco-root.module';
import {AppRoutingModule} from './app/app-routing.module';
import {StoreRouterConnectingModule} from '@ngrx/router-store';
import {AuthConfigModule} from './app/auth/auth-config.module';
import {ToastModule} from 'primeng/toast';
import {ButtonModule} from 'primeng/button';
import {ToolbarModule} from 'primeng/toolbar';
import {EditorModule} from 'primeng/editor';
import {RippleModule} from 'primeng/ripple';
import {TableModule} from 'primeng/table';
import {PaginatorModule} from 'primeng/paginator';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {NgPipesModule} from 'ngx-pipes';
import {TooltipModule} from 'primeng/tooltip';
import {MatomoModule, MatomoRouterModule} from 'ngx-matomo-client';
import {FormsModule} from '@angular/forms';
import {AppComponent} from './app/app.component';
import Material from "@primeuix/themes/material";

async function loadLocaleData(locale: string) {
  switch (locale) {
    case 'de-CH': {
      const {default: deCh} = await import('@angular/common/locales/de-CH');
      registerLocaleData(deCh);
      break;
    }
    case 'fr-CH': {
      const {default: frCh} = await import('@angular/common/locales/fr-CH');
      registerLocaleData(frCh);
      break;
    }
    case 'en-US': {
      const {default: enUs} = await import('@angular/common/locales/en');
      registerLocaleData(enUs);
      break;
    }
    default: {
      const {default: en} = await import('@angular/common/locales/en');
      registerLocaleData(en);
      console.warn(`Locale data for '${locale}' not found, falling back to 'en-US'`);
    }
  }
}


(async () => {
  // Dynamically load locale data before bootstrapping
  await loadLocaleData(environment.locale);

  await bootstrapApplication(AppComponent, {
    providers: [
      importProvidersFrom(
        StoreModule.forRoot(appReducers),
        EffectsModule.forRoot(appEffects),
        StoreDevtoolsModule.instrument({maxAge: 25}),
        BrowserModule,
        HttpClientModule,
        TranslocoRootModule,
        AppRoutingModule,
        StoreRouterConnectingModule.forRoot(),
        AuthConfigModule,
        CommonModule,
        AsyncPipe,
        JsonPipe,
        ToastModule,
        ButtonModule,
        ToolbarModule,
        EditorModule,
        RippleModule,
        TableModule,
        PaginatorModule,
        FontAwesomeModule,
        NgPipesModule,
        TooltipModule,
        MatomoModule.forRoot({
          disabled: environment.matomoConfig ? !environment.matomoConfig.enabled : false,
          siteId: environment.matomoConfig ? environment.matomoConfig.siteId : 0,
          trackerUrl: environment.matomoConfig ? environment.matomoConfig.trackerUrl : '',
        }),
        MatomoRouterModule,
        FormsModule
      ),
      ScreenService,
      AppInfoService,
      {provide: LOCALE_ID, useValue: environment.locale},
      {
        provide: HTTP_INTERCEPTORS,
        useClass: TokenInterceptor,
        multi: true
      },
      MessageService,
      ConfirmationService,
      TitleCasePipe,
      providePrimeNG({
        theme: {
          preset: Material,
          options: {
            darkModeSelector: '.dark-mode',
            cssLayer: {
              name: 'primeng',
              order: 'theme, base, primeng'
            }
          }
        }
      }),
      provideAnimations()
    ]
  }).catch(err => console.error(err));
})();
