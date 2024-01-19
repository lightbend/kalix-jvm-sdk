package user.registry.domain;

import user.registry.entity.UniqueEmailEntity;

import java.util.Optional;

public record UniqueEmail(String address, UniqueEmailEntity.Status status, Optional<String> ownerId) {

  public record ReserveEmail(String address, String ownerId) {
  }

  public boolean sameOwner(String ownerId) {
    return this.ownerId.isPresent() && this.ownerId.get().equals(ownerId);
  }

  public boolean notSameOwner(String ownerId) {
    return !sameOwner(ownerId);
  }

  public UniqueEmail asConfirmed() {
    return new UniqueEmail(address, UniqueEmailEntity.Status.CONFIRMED, ownerId);
  }

  public boolean isConfirmed() {
    return status == UniqueEmailEntity.Status.CONFIRMED;
  }

  public boolean isInUse() {
    return status != UniqueEmailEntity.Status.NOT_USED;
  }

  public boolean isNotInUse() {
    return !isInUse();
  }

  public boolean isReserved() {
    return status == UniqueEmailEntity.Status.RESERVED;
  }
}
