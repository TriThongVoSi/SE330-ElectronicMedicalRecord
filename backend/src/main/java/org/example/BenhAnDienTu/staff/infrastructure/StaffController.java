package org.example.BenhAnDienTu.staff.infrastructure;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.example.BenhAnDienTu.staff.api.StaffApi;
import org.example.BenhAnDienTu.staff.api.StaffDoctorCreateCommand;
import org.example.BenhAnDienTu.staff.api.StaffDoctorUpdateCommand;
import org.example.BenhAnDienTu.staff.api.StaffMemberView;
import org.example.BenhAnDienTu.staff.api.StaffProfileUpdateCommand;
import org.example.BenhAnDienTu.staff.api.StaffProfileView;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/staff")
public class StaffController {

  private final StaffApi staffApi;

  public StaffController(StaffApi staffApi) {
    this.staffApi = staffApi;
  }

  @GetMapping("/doctors")
  public List<StaffMemberView> listDoctors() {
    return staffApi.listDoctors();
  }

  @GetMapping("/doctors/{id}")
  public StaffProfileView getDoctor(@PathVariable("id") String id) {
    return staffApi
        .findDoctorProfile(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Doctor does not exist: " + id));
  }

  @PostMapping("/doctors")
  public StaffProfileView createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
    return staffApi.createDoctor(request.toCommand());
  }

  @PutMapping("/doctors/{id}")
  public StaffProfileView updateDoctor(
      @PathVariable("id") String id, @Valid @RequestBody UpdateDoctorRequest request) {
    return staffApi.updateDoctor(id, request.toCommand());
  }

  @DeleteMapping("/doctors/{id}")
  public ResponseEntity<Void> deactivateDoctor(@PathVariable("id") String id) {
    staffApi.deactivateDoctor(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/profile")
  public StaffProfileView getMyProfile(@RequestAttribute(name = "actorId") String actorId) {
    return staffApi
        .findMyProfile(actorId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    NOT_FOUND, "Doctor profile does not exist for actor: " + actorId));
  }

  @PutMapping("/profile")
  public StaffProfileView updateMyProfile(
      @RequestAttribute(name = "actorId") String actorId,
      @Valid @RequestBody UpdateMyProfileRequest request) {
    return staffApi.updateMyProfile(actorId, request.toCommand());
  }

  public record CreateDoctorRequest(
      @NotBlank @Size(min = 3, max = 50) String username,
      @NotBlank @Size(min = 6, max = 128) String password,
      @NotBlank @Email String email,
      @NotBlank @Size(max = 100) String fullName,
      @Size(max = 20) String gender,
      @Size(max = 20) String phone,
      @Size(max = 255) String address,
      String serviceId,
      Boolean isConfirmed,
      Boolean isActive) {

    private StaffDoctorCreateCommand toCommand() {
      boolean confirmed = Boolean.TRUE.equals(isConfirmed);
      boolean active = isActive == null || isActive;
      return new StaffDoctorCreateCommand(
          username, password, email, fullName, gender, phone, address, serviceId, confirmed,
          active);
    }
  }

  public record UpdateDoctorRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(max = 100) String fullName,
      @Size(max = 20) String gender,
      @Size(max = 20) String phone,
      @Size(max = 255) String address,
      String serviceId,
      Boolean isConfirmed,
      Boolean isActive) {

    private StaffDoctorUpdateCommand toCommand() {
      boolean confirmed = Boolean.TRUE.equals(isConfirmed);
      boolean active = isActive == null || isActive;
      return new StaffDoctorUpdateCommand(
          email, fullName, gender, phone, address, serviceId, confirmed, active);
    }
  }

  public record UpdateMyProfileRequest(
      @NotBlank @Size(max = 100) String fullName,
      @NotBlank @Email String email,
      @Size(max = 20) String gender,
      @Size(max = 20) String phone,
      @Size(max = 255) String address) {

    private StaffProfileUpdateCommand toCommand() {
      return new StaffProfileUpdateCommand(fullName, email, gender, phone, address);
    }
  }
}
