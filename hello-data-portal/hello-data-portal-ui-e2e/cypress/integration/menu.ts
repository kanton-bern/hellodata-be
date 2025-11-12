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

describe('Menu', () => {
    it('Admin should see full menu.', () => {
        cy.loginAsAdmin();
        cy.contains('Dashboards');
        cy.contains('HelloDATA');
        cy.wait(1000);
        cy.get('#dashboardsMenu').should('exist');
        cy.get('#lineageMenu').should('exist');
        cy.get('#dataMartsMenu').should('exist');
        cy.get('#dataEngMenu').should('exist');
        cy.get('#dataEngViewerMenu').should('exist');
        cy.get('#dataEngOrchestrationMenu').should('exist');
        cy.get('#administrationMenu').should('exist');
        cy.get('#userManagementMenu').should('exist');
        cy.get('#portalRoleManagementMenu').should('exist');
        cy.get('#announcementsManagementMenu').should('exist');
        cy.get('#faqManagementMenu').should('exist');
        cy.get('#documentationManagementMenu').should('exist');
        cy.get('#monitoringMenu').should('exist');
        cy.get('#monitoringWorkspacesMenu').should('exist');
        cy.get('#devToolsMenu').should('exist');
        cy.get('#devToolsMailboxMenu').should('exist');
        cy.get('#devToolsFileBrowserMenu').should('exist');
    });

    it('Should see right menu', function () {
        cy.loginAsAdmin();
        cy.get('.home-view').click();
        cy.get('.summary-header').should('be.visible');
        cy.get('.summary-header').click();
        cy.get('.p-sidebar-active').should('be.visible');
        cy.get('#subscriptionsHeader').should('exist');
        cy.get('#statusHeader').should('exist');
    });
});
