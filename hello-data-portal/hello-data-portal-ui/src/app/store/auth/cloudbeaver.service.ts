// services/user-preferences.service.ts
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root',
})
export class CloudbeaverService {
  private apiUrl = environment.subSystemsConfig.dmViewer.protocol + environment.subSystemsConfig.dmViewer.host
    + environment.subSystemsConfig.dmViewer.domain + 'api/gql';

  constructor(private http: HttpClient) {
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

    return this.http.post(this.apiUrl, body, {
      headers: {
        'Content-Type': 'application/json',
        'accept': '*/*',
      },
      withCredentials: true,
    });
  }

  renewSession(): void {
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

    this.http.post(this.apiUrl, body, {
      headers: {
        'Content-Type': 'application/json',
        'accept': '*/*',
      },
      withCredentials: true,
    }).subscribe();
  }
}
