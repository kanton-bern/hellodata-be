// services/user-preferences.service.ts
import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {filter, map, Observable, switchMap, take} from 'rxjs';
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectAppInfoByModuleType} from "../metainfo-resource/metainfo-resource.selector";

@Injectable({
  providedIn: 'root',
})
export class CloudbeaverService {
  private readonly http = inject(HttpClient);
  private readonly store = inject<Store<AppState>>(Store);

  private getApiUrl(): Observable<string> {
    return this.store.select(selectAppInfoByModuleType('CLOUDBEAVER')).pipe(
      filter(infos => infos.length > 0),
      take(1),
      map(infos => infos[0].data.url + 'api/gql')
    );
  }

  updateUserPreferences(selectedLang: string): Observable<any> {
    const preferences = {
      'core.localization.language': selectedLang.slice(0, 2),
    };
    const body = {
      query: `
        mutation updateUserPreferences($preferences: Object!, $includeMetaParameters: Boolean!, $includeConfigurationParameters: Boolean!, $customIncludeOriginDetails: Boolean!) {
          user: setUserPreferences(preferences: $preferences) {
            userId
            displayName
            authRole
            linkedAuthProviders
            metaParameters @include(if: $includeMetaParameters)
            configurationParameters @include(if: $includeConfigurationParameters)
            authTokens {
              ...AuthToken
            }
          }
        }
        fragment AuthToken on UserAuthToken {
          authProvider
          authConfiguration
          loginTime
          message
          origin {
            ...ObjectOriginInfo
          }
        }
        fragment ObjectOriginInfo on ObjectOrigin {
          type
          subType
          displayName
          icon
          details @include(if: $customIncludeOriginDetails) {
            id
            required
            displayName
            description
            category
            dataType
            defaultValue
            validValues
            value
            length
            features
            order
          }
        }
      `,
      variables: {
        preferences: preferences,
        customIncludeOriginDetails: true,
        includeConfigurationParameters: true,
        includeMetaParameters: false,
        customIncludeBase: true,
      },
      operationName: 'updateUserPreferences',
    };

    return this.getApiUrl().pipe(
      switchMap(apiUrl => this.http.post(apiUrl, body, {
        headers: {
          'Content-Type': 'application/json',
          'accept': '*/*',
        },
        withCredentials: true,
      }))
    );
  }

  renewSession(): Observable<any> {
    console.debug('Renewing cloudbeaver session');
    const body = {
      query: `query sessionState {
        sessionState {
          ...SessionState
        }
      }

      fragment SessionState on SessionInfo {
        createTime
        lastAccessTime
        cacheExpired
        locale
        actionParameters
        valid
        remainingTime
      }`,
      operationName: 'sessionState',
    };

    return this.getApiUrl().pipe(
      switchMap(apiUrl => this.http.post(apiUrl, body, {
        headers: {
          'Content-Type': 'application/json',
          'accept': '*/*',
        },
        withCredentials: true,
      }))
    );
  }
}
