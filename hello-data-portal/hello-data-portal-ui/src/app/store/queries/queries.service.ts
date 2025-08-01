import {Injectable} from "@angular/core";
import {environment} from "../../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class QueriesService {
  baseUrl = `${environment.portalApi}/superset/queries/`;

  constructor(protected httpClient: HttpClient) {
  }

  public getQueries(contextKey: string): Observable<any[]> {
    return this.httpClient.get<any[]>(`${this.baseUrl}${contextKey}`);
  }
}
