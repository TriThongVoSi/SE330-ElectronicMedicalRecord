package org.example.BenhAnDienTu.identity.api;

import java.util.Set;

/** Read-only identity view exposed to other modules. */
public record IdentityProfileView(String actorId, String principal, Set<String> permissions) {}
