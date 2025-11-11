describe('Auth experience', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/franchises', []).as('getFranchises');
  });

  it('shows token returned by forgot-password endpoint', () => {
    cy.visit('/login');
    cy.contains('Olvidaste tu contrasena?').click();
    cy.url().should('include', 'forgot-password');

    cy.intercept('POST', '**/auth/forgot-password', {
      body: {
        message: 'Token generado correctamente.',
        resetToken: 'test-token-123'
      }
    }).as('forgot');

    cy.get('input#username').type('admin');
    cy.contains('Solicitar token').click();
    cy.wait('@forgot');
    cy.get('p.alert.success code').should('contain', 'test-token-123');
  });

  it('forces password change when backend sets the flag', () => {
    cy.visit('/login');
    cy.intercept('POST', '**/auth/login', {
      body: {
        token: 'mock-jwt',
        username: 'admin',
        roles: ['ADMIN'],
        expiresAt: Date.now() + 60_000,
        passwordChangeRequired: true
      }
    }).as('login');
    cy.intercept('POST', '**/auth/change-password', {
      body: { message: 'Contrasena actualizada correctamente.' }
    }).as('changePassword');

    cy.get('#username').type('admin');
    cy.get('#password').type('Admin123!');
    cy.contains('Iniciar sesion').click();
    cy.wait('@login');

    cy.contains('Actualiza tu contrasena').should('be.visible');
    cy.get('input#currentPassword').type('Admin123!');
    cy.get('input#newPassword').type('NewPass123!');
    cy.get('input#confirmPassword').type('NewPass123!');
    cy.contains('Actualizar contrasena').click();
    cy.wait('@changePassword');
    cy.contains('Actualiza tu contrasena').should('not.exist');
  });
});
