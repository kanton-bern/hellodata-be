import {Injectable} from "@angular/core";
import {environment} from "../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
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

  public getQueriesPaginated(contextKey: string, page: number, size: number, sort: string, search: string): Observable<{
    content: any[],
    totalElements: number,
    totalPages: number
  }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort)
      .set('search', search || '');

    return this.httpClient.get<{
      content: any[],
      totalElements: number,
      totalPages: number
    }>(`${this.baseUrl}${contextKey}`, {params});
  }

}
