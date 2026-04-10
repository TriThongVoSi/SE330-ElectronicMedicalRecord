package org.example.BenhAnDienTu.identity.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.example.BenhAnDienTu.identity.api.IdentityProvisionDoctorCommand;
import org.example.BenhAnDienTu.identity.api.IdentityProvisionPatientCommand;
import org.example.BenhAnDienTu.identity.api.IdentityProvisionedAccountView;
import org.example.BenhAnDienTu.identity.api.IdentityProvisioningApi;
import org.example.BenhAnDienTu.identity.domain.IdentityAccount;
import org.example.BenhAnDienTu.identity.infrastructure.IdentityDirectoryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentityProvisioningApplicationService implements IdentityProvisioningApi {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(IdentityProvisioningApplicationService.class);
  private static final String ROLE_DOCTOR = "DOCTOR";
  private static final String ROLE_PATIENT = "PATIENT";
  private static final String TEMP_PASSWORD_ALPHABET =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";

  private final IdentityDirectoryAdapter directoryAdapter;
  private final PasswordEncoder passwordEncoder;
  private final JavaMailSender mailSender;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${spring.mail.host:}")
  private String smtpHost;

  @Value("${app.mail.from:no-reply@example.com}")
  private String senderEmail;

  @Value("${app.url:http://localhost:5173}")
  private String appUrl;

  @Value("${app.mail.enabled:true}")
  private boolean mailEnabled;

  @Value("${app.mail.dev-fallback-enabled:true}")
  private boolean devFallbackEnabled;

  public IdentityProvisioningApplicationService(
      IdentityDirectoryAdapter directoryAdapter,
      PasswordEncoder passwordEncoder,
      JavaMailSender mailSender,
      Clock clock) {
    this.directoryAdapter = directoryAdapter;
    this.passwordEncoder = passwordEncoder;
    this.mailSender = mailSender;
    this.clock = clock;
  }

  @Override
  public IdentityProvisionedAccountView provisionDoctorAccount(IdentityProvisionDoctorCommand command) {
    String username = normalizeRequired(command.username(), "Doctor username is required.");
    String email = normalizeRequired(command.email(), "Doctor email is required.");
    String fullName = normalizeRequired(command.fullName(), "Doctor full name is required.");

    if (directoryAdapter.existsByUsername(username)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor username already exists.");
    }
    if (directoryAdapter.existsByEmail(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor email already exists.");
    }

    LocalDateTime now = LocalDateTime.now(clock);
    String temporaryPassword = resolveTemporaryPassword(command.preferredTemporaryPassword());
    IdentityAccount account =
        directoryAdapter.createUser(
            IdentityDirectoryAdapter.CreateUserCommand.activeAccount(
                null,
                username,
                email,
                passwordEncoder.encode(temporaryPassword),
                fullName,
                normalizeNullable(command.gender()),
                ROLE_DOCTOR,
                true,
                now,
                null));

    sendOnboardingEmail(account.email(), account.fullName(), account.principal(), temporaryPassword, ROLE_DOCTOR);
    return toView(account);
  }

  @Override
  public IdentityProvisionedAccountView provisionPatientAccount(
      IdentityProvisionPatientCommand command) {
    String patientId = normalizeRequired(command.patientId(), "Patient id is required.");
    String patientCode = normalizeRequired(command.patientCode(), "Patient code is required.");
    String email = normalizeRequired(command.email(), "Patient email is required.");
    String fullName = normalizeRequired(command.fullName(), "Patient full name is required.");

    Optional<String> existingActorId = directoryAdapter.findActorIdByLinkedPatientId(patientId);
    if (existingActorId.isPresent()) {
      IdentityAccount existing = directoryAdapter.findByActorId(existingActorId.get()).orElseThrow();
      return toView(existing);
    }

    if (directoryAdapter.existsByEmail(email)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Patient email is already linked with another account.");
    }

    String username = generatePatientUsername(patientCode);
    LocalDateTime now = LocalDateTime.now(clock);
    String temporaryPassword = generateTemporaryPassword(12);

    IdentityAccount account =
        directoryAdapter.createUser(
            IdentityDirectoryAdapter.CreateUserCommand.activeAccount(
                null,
                username,
                email,
                passwordEncoder.encode(temporaryPassword),
                fullName,
                normalizeNullable(command.gender()),
                ROLE_PATIENT,
                true,
                now,
                patientId));

    sendOnboardingEmail(
        account.email(), account.fullName(), account.principal(), temporaryPassword, ROLE_PATIENT);
    return toView(account);
  }

  private void sendOnboardingEmail(
      String recipientEmail,
      String fullName,
      String username,
      String temporaryPassword,
      String roleCode) {
    String roleLabel = ROLE_DOCTOR.equals(roleCode) ? "Doctor" : "Patient";
    String subject = "EMR onboarding - " + roleLabel + " account";
    String body =
        """
        Hello %s,

        Your %s account has been provisioned by the internal EMR team.

        Username: %s
        Temporary password: %s
        Portal URL: %s

        You must sign in and change the password at first login before using business functions.

        Regards,
        EMR system
        """
            .formatted(fullName, roleLabel, username, temporaryPassword, appUrl);

    if (!StringUtils.hasText(recipientEmail)) {
      return;
    }

    if (!mailEnabled) {
      LOGGER.info(
          "Mail is disabled by configuration. Onboarding email skipped safely for {} account {}.",
          roleLabel,
          maskRecipient(recipientEmail));
      return;
    }

    if (!StringUtils.hasText(smtpHost)) {
      LOGGER.info(
          "SMTP is not configured. Onboarding email skipped safely for {} account {}.",
          roleLabel,
          maskRecipient(recipientEmail));
      return;
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(senderEmail);
    message.setTo(recipientEmail);
    message.setSubject(subject);
    message.setText(body);

    try {
      mailSender.send(message);
    } catch (MailException exception) {
      if (devFallbackEnabled) {
        LOGGER.warn(
            "Onboarding email delivery failed. Safe dev fallback used for {}.",
            maskRecipient(recipientEmail),
            exception);
        return;
      }
      throw exception;
    }
  }

  private String generatePatientUsername(String patientCode) {
    String normalizedCode =
        patientCode.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("-+", "-");
    if (normalizedCode.isBlank()) {
      normalizedCode = "patient";
    }

    String base = "patient-" + normalizedCode;
    if (!directoryAdapter.existsByUsername(base)) {
      return base;
    }

    for (int suffix = 1; suffix <= 9999; suffix++) {
      String candidate = base + "-" + suffix;
      if (!directoryAdapter.existsByUsername(candidate)) {
        return candidate;
      }
    }

    throw new ResponseStatusException(
        HttpStatus.CONFLICT, "Cannot allocate a unique username for patient account.");
  }

  private String resolveTemporaryPassword(String preferredTemporaryPassword) {
    String normalized = normalizeNullable(preferredTemporaryPassword);
    if (normalized != null && normalized.length() >= 8) {
      return normalized;
    }
    return generateTemporaryPassword(12);
  }

  private String generateTemporaryPassword(int length) {
    StringBuilder password = new StringBuilder(length);
    for (int index = 0; index < length; index++) {
      int randomIndex = secureRandom.nextInt(TEMP_PASSWORD_ALPHABET.length());
      password.append(TEMP_PASSWORD_ALPHABET.charAt(randomIndex));
    }
    return password.toString();
  }

  private static IdentityProvisionedAccountView toView(IdentityAccount account) {
    return new IdentityProvisionedAccountView(
        account.actorId(),
        account.principal(),
        account.email(),
        account.role(),
        account.mustChangePassword());
  }

  private static String normalizeRequired(String value, String message) {
    String normalized = normalizeNullable(value);
    if (normalized == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return normalized;
  }

  private static String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static String maskRecipient(String recipient) {
    if (recipient == null || recipient.isBlank() || !recipient.contains("@")) {
      return "";
    }
    String[] parts = recipient.split("@", 2);
    if (parts[0].length() <= 2) {
      return "*@" + parts[1];
    }
    return parts[0].charAt(0) + "***" + parts[0].charAt(parts[0].length() - 1) + "@" + parts[1];
  }
}
