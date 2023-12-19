package user.registry.entity;


import kalix.javasdk.testkit.ValueEntityTestKit;
import org.junit.jupiter.api.Test;
import user.registry.domain.UniqueEmail;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UniqueEmailEntityTest {

  @Test
  public void testReserveAndConfirm() {
    var emailTestKit = ValueEntityTestKit.of(UniqueEmailEntity::new);
    reserveEmail(emailTestKit, "joe@acme.com", "1");
    confirmEmail(emailTestKit);
  }

  @Test
  public void testReserveAndUnReserve() {
    var emailTestKit = ValueEntityTestKit.of(UniqueEmailEntity::new);
    reserveEmail(emailTestKit, "joe@acme.com", "1");
    unreserveEmail(emailTestKit);

    var state = emailTestKit.call(UniqueEmailEntity::getState).getReply();
    assertThat(state.isNotInUse()).isTrue();
  }

  @Test
  public void testReserveConfirmAndUnReserve() {
    var emailTestKit = ValueEntityTestKit.of(UniqueEmailEntity::new);
    reserveEmail(emailTestKit, "joe@acme.com", "1");
    confirmEmail(emailTestKit);

    // unReserving a confirmed has no effect
    unreserveEmail(emailTestKit);
    var state = emailTestKit.call(UniqueEmailEntity::getState).getReply();
    assertThat(state.isInUse()).isTrue();
    assertThat(state.isConfirmed()).isTrue();
  }

  @Test
  public void testReserveAndDeleting() {
    var emailTestKit = ValueEntityTestKit.of(UniqueEmailEntity::new);
    reserveEmail(emailTestKit, "joe@acme.com", "1");
    deleteEmail(emailTestKit);
  }

  @Test
  public void testReserveConfirmAndDeleting() {
    var emailTestKit = ValueEntityTestKit.of(UniqueEmailEntity::new);
    reserveEmail(emailTestKit, "joe@acme.com", "1");
    confirmEmail(emailTestKit);
    deleteEmail(emailTestKit);
  }

  private static void confirmEmail(ValueEntityTestKit<UniqueEmail, UniqueEmailEntity> emailTestKit) {
    var confirmedRes = emailTestKit.call(UniqueEmailEntity::confirm);
    assertThat(confirmedRes.isReply()).isTrue();
    assertThat(confirmedRes.stateWasUpdated()).isTrue();
    var state = emailTestKit.call(UniqueEmailEntity::getState).getReply();
    assertThat(state.isConfirmed()).isTrue();
  }

  private static void reserveEmail(ValueEntityTestKit<UniqueEmail, UniqueEmailEntity> emailTestKit, String email, String ownerId) {
    var reserveCmd = new UniqueEmail.ReserveEmail(email, ownerId);
    var reservedRes = emailTestKit.call(emailEntity -> emailEntity.reserve(reserveCmd));
    assertThat(reservedRes.isReply()).isTrue();
    assertThat(reservedRes.stateWasUpdated()).isTrue();

    var state = emailTestKit.call(UniqueEmailEntity::getState).getReply();
    assertThat(state.isReserved()).isTrue();
  }

  private static void deleteEmail(ValueEntityTestKit<UniqueEmail, UniqueEmailEntity> emailTestKit) {
    var reservedRes = emailTestKit.call(UniqueEmailEntity::delete);
    assertThat(reservedRes.isReply()).isTrue();
    assertThat(reservedRes.stateWasUpdated()).isTrue();

    var state = emailTestKit.call(UniqueEmailEntity::getState).getReply();
    assertThat(state.isNotInUse()).isTrue();
  }

  private static void unreserveEmail(ValueEntityTestKit<UniqueEmail, UniqueEmailEntity> emailTestKit) {
    var reservedRes = emailTestKit.call(UniqueEmailEntity::cancelReservation);
    assertThat(reservedRes.isReply()).isTrue();
  }


}
