package org.example.BenhAnDienTu.staff.api;

import java.util.List;
import java.util.Optional;

/** Public contract to query staff data from outside the staff module. */
public interface StaffApi {

  Optional<StaffMemberView> findStaffMember(String staffId);

  List<StaffMemberView> listDoctors();

  Optional<StaffProfileView> findDoctorProfile(String staffId);

  StaffProfileView createDoctor(StaffDoctorCreateCommand command);

  StaffProfileView updateDoctor(String staffId, StaffDoctorUpdateCommand command);

  void deactivateDoctor(String staffId);

  Optional<StaffProfileView> findMyProfile(String actorId);

  StaffProfileView updateMyProfile(String actorId, StaffProfileUpdateCommand command);
}
