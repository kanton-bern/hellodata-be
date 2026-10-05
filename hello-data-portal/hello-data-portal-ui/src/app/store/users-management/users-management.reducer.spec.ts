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

import {describe, expect, it} from '@jest/globals';
import {usersManagementReducer} from './users-management.reducer';
import {initialUsersManagementState} from './users-management.state';
import {clearRlsRolesForContext, loadRlsRolesForUserSuccess, setRlsRolesForUser} from './users-management.action';

describe('usersManagementReducer - RLS roles', () => {
  const rlsRoles = [{roleKey: 'RLS_01', name: 'Region Bern'}, {roleKey: 'RLS_02', name: 'RLS_02'}];

  it('should store available and selected RLS roles per context', () => {
    const state = usersManagementReducer(initialUsersManagementState,
      loadRlsRolesForUserSuccess({contextKey: 'dd01', rlsRoles, selectedRoleKeys: ['RLS_02']}));

    expect(state.rlsRolesForUser['dd01']).toEqual(rlsRoles);
    expect(state.selectedRlsRolesForUser['dd01']).toEqual(['RLS_02']);
  });

  it('should replace the selection of one context only', () => {
    const loaded = usersManagementReducer(initialUsersManagementState,
      loadRlsRolesForUserSuccess({contextKey: 'dd02', rlsRoles, selectedRoleKeys: ['RLS_01']}));

    const state = usersManagementReducer(loaded, setRlsRolesForUser({contextKey: 'dd01', roleKeys: ['RLS_01', 'RLS_02']}));

    expect(state.selectedRlsRolesForUser).toEqual({dd01: ['RLS_01', 'RLS_02'], dd02: ['RLS_01']});
  });

  it('should clear the selection of a context', () => {
    const loaded = usersManagementReducer(initialUsersManagementState,
      setRlsRolesForUser({contextKey: 'dd01', roleKeys: ['RLS_01']}));

    const state = usersManagementReducer(loaded, clearRlsRolesForContext({contextKey: 'dd01'}));

    expect(state.selectedRlsRolesForUser['dd01']).toEqual([]);
  });
});
