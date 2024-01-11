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

describe('Main page', () => {

    it('Should log in programmatically as admin.', () => {
        cy.loginAsAdmin();
        cy.contains('Dashboards');
        cy.contains('HelloDATA');
    });

    // it('Should log in via browser as admin.', () => {
    //   cy.logout();
    //   cy.visit('/');
    //   const username = Cypress.env('admin_username') as string;
    //   const password = Cypress.env('admin_password') as string;
    //   cy.signInViaBrowser(username, password);
    //   cy.contains('Is Authenticated: true');
    // });

    // it('Signing out from webapp should be possible.', () => {
    //     cy.logout();
    //     cy.visit('/');
    //     const username = Cypress.env('admin_username') as string;
    //     const password = Cypress.env('admin_password') as string;
    //     cy.signInViaBrowser(username, password);
    //     cy.contains('Is Authenticated: true');
    //     cy.get('.user-info').click().get('.dx-submenu').contains('Logout').click(); // click on logout link
    //     cy.contains('Is Authenticated: true').should('not.exist');
    // });

});

