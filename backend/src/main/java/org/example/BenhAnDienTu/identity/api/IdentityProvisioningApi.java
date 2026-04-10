package org.example.BenhAnDienTu.identity.api;

/** Internal contract for provisioning staff and patient user accounts. */
public interface IdentityProvisioningApi {

  IdentityProvisionedAccountView provisionDoctorAccount(IdentityProvisionDoctorCommand command);

  IdentityProvisionedAccountView provisionPatientAccount(IdentityProvisionPatientCommand command);
}
