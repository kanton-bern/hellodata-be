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

import {TestBed} from '@angular/core/testing';
import {Store} from '@ngrx/store';
import {of} from 'rxjs';
import {afterEach, describe, expect, it, jest} from '@jest/globals';
import {jupyterhubLogoutUrl, SubsystemLogoutService} from './subsystem-logout.service';
import {
  VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY
} from '../../pages/my-dashboards/embed-my-dashboard/embed-my-dashboard.component';

describe('SubsystemLogoutService', () => {
  const appInfo = (url: string) => [{data: {url}}];

  const setup = (airflow: unknown[] = [], airflow3: unknown[] = [], sftpgo: unknown[] = [], jupyterhubs: unknown[] = []) => {
    const storeMock = {
      select: jest.fn<(selector: unknown) => unknown>()
        .mockReturnValueOnce(of(airflow))
        .mockReturnValueOnce(of(airflow3))
        .mockReturnValueOnce(of(sftpgo))
        .mockReturnValueOnce(of(jupyterhubs))
    };
    TestBed.configureTestingModule({
      providers: [SubsystemLogoutService, {provide: Store, useValue: storeMock}]
    });
    return TestBed.inject(SubsystemLogoutService);
  };

  const hiddenFrames = () => Array.from(document.querySelectorAll('iframe')) as HTMLIFrameElement[];

  afterEach(() => {
    sessionStorage.clear();
    hiddenFrames().forEach(frame => frame.remove());
    jest.useRealTimers();
  });

  it('loads the visited and configured subsystem logout URLs in hidden iframes', async () => {
    sessionStorage.setItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY,
      JSON.stringify(['https://superset-a/logout', 'https://jupyter/hub/logout']));
    const service = setup(appInfo('https://airflow'), appInfo('https://airflow3'), appInfo('https://sftp'));

    const done = service.logoutOpenedSubsystems(10_000);
    // Let the async URL collection finish and the frames get created.
    await new Promise(resolve => setTimeout(resolve, 0));

    const frames = hiddenFrames();
    expect(frames.map(f => f.getAttribute('src')).sort()).toEqual([
      'https://airflow/logout',
      'https://airflow3/logout',
      'https://jupyter/hub/logout',
      'https://sftp/web/client/logout',
      'https://superset-a/logout',
    ]);
    expect(frames.every(f => f.style.display === 'none')).toBe(true);
    expect(sessionStorage.getItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY)).toBeNull();

    frames.forEach(f => f.dispatchEvent(new Event('load')));
    await done;
    expect(hiddenFrames()).toHaveLength(0);
  });

  it('logs out of every JupyterHub of the user, also when it was not opened in this tab', async () => {
    sessionStorage.setItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY, JSON.stringify(['https://jupyter-dd01/hub/logout']));
    const service = setup([], [], [], [
      {data: {url: 'https://jupyter-dd01/'}},
      {data: {url: 'https://jupyter-dd02'}},
      {data: {}},
    ]);

    const done = service.logoutOpenedSubsystems(10_000);
    await new Promise(resolve => setTimeout(resolve, 0));

    // dd01 was visited in this tab and is configured: called once; dd02 was only opened elsewhere: called too
    expect(hiddenFrames().map(f => f.getAttribute('src')).sort()).toEqual([
      'https://jupyter-dd01/hub/logout',
      'https://jupyter-dd02/hub/logout',
    ]);
    hiddenFrames().forEach(f => f.dispatchEvent(new Event('load')));
    await done;
  });

  it('builds the JupyterHub logout URL with or without a trailing slash', () => {
    expect(jupyterhubLogoutUrl('https://jupyter')).toBe('https://jupyter/hub/logout');
    expect(jupyterhubLogoutUrl('https://jupyter/')).toBe('https://jupyter/hub/logout');
    expect(jupyterhubLogoutUrl('https://jupyter/base//')).toBe('https://jupyter/base/hub/logout');
  });

  it('resolves after the timeout when a subsystem never answers', async () => {
    jest.useFakeTimers();
    sessionStorage.setItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY, JSON.stringify(['https://superset-a/logout']));
    const service = setup();

    let resolved = false;
    const done = service.logoutOpenedSubsystems(1500).then(() => resolved = true);
    await jest.advanceTimersByTimeAsync(1499);
    expect(resolved).toBe(false);
    await jest.advanceTimersByTimeAsync(1);
    await done;
    expect(resolved).toBe(true);
    expect(hiddenFrames()).toHaveLength(0);
  });

  it('does nothing when no subsystem was opened', async () => {
    const service = setup();
    await service.logoutOpenedSubsystems();
    expect(hiddenFrames()).toHaveLength(0);
  });

  it('ignores a corrupt visited-subsystems entry', async () => {
    sessionStorage.setItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY, '{not json');
    const service = setup(appInfo('https://airflow'));
    const done = service.logoutOpenedSubsystems(10_000);
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(hiddenFrames().map(f => f.getAttribute('src'))).toEqual(['https://airflow/logout']);
    hiddenFrames().forEach(f => f.dispatchEvent(new Event('load')));
    await done;
  });
});
