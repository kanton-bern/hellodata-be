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

import {LOCALE_ID, NgModule} from "@angular/core";
import {HTTP_INTERCEPTORS, HttpClientModule} from "@angular/common/http";
import {AsyncPipe, CommonModule, JsonPipe} from "@angular/common";
import {AppRoutingModule} from "./app-routing.module";
import {BrowserModule} from "@angular/platform-browser";
import {StoreModule} from "@ngrx/store";
import {appReducers} from "./store/app/app.reducer";
import {EffectsModule} from "@ngrx/effects";
import {appEffects} from "./store/app/app.effects";
import {StoreDevtoolsModule} from "@ngrx/store-devtools";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {StoreRouterConnectingModule} from "@ngrx/router-store";
import {AuthConfigModule} from "./auth/auth-config.module";
import {ToastModule} from "primeng/toast";
import {ButtonModule} from "primeng/button";
import {ToolbarModule} from "primeng/toolbar";
import {EditorModule} from "primeng/editor";
import {RippleModule} from "primeng/ripple";
import {TableModule} from "primeng/table";
import {PaginatorModule} from "primeng/paginator";
import {FontAwesomeModule} from "@fortawesome/angular-fontawesome";
import {NgPipesModule} from "ngx-pipes";
import {TooltipModule} from "primeng/tooltip";
import {AppInfoService, ScreenService} from "./shared/services";
import {environment} from "../environments/environment";
import {TokenInterceptor} from "./shared/interceptor/token-interceptor.service";
import {ConfirmationService, MessageService} from "primeng/api";
import {TranslocoTestingModule} from "@jsverse/transloco";

@NgModule({
  declarations: [
    // AppComponent,
    // CallbackComponent,
    // ProfileComponent,
  ],
  imports: [
    StoreModule.forRoot(appReducers),
    EffectsModule.forRoot(appEffects),
    // Instrumentation must be imported after importing StoreModule (config is optional)
    StoreDevtoolsModule.instrument({
      maxAge: 25, // Retains last 25 states
      // logOnly: environment.production, // Restrict extension to log-only mode
    }),
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
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
    TranslocoTestingModule
  ],
  providers: [
    ScreenService,
    AppInfoService,
    {provide: LOCALE_ID, useValue: environment.locale},
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true
    },
    MessageService,
    ConfirmationService
  ],
})
export class TestModule {

}
