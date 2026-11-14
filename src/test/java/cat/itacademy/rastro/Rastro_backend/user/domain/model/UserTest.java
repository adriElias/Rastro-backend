package cat.itacademy.rastro.Rastro_backend.user.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    void shouldCreateUserWithValidData() {
        UserId id = UserId.generate();
        Email email = new Email("adriana@example.com");
        PasswordHash passwordHash =
                new PasswordHash("$2a$10$exampleHashValue");
        Instant registrationDate = Instant.now();

        User user = new User(
                id,
                "Adriana",
                "Elias",
                email,
                passwordHash,
                null,
                registrationDate,
                UserRole.USER,
                AccountStatus.ACTIVE
        );

        assertEquals(id, user.id());
        assertEquals("Adriana", user.name());
        assertEquals("Elias", user.lastName());
        assertEquals(email, user.email());
        assertEquals(passwordHash, user.passwordHash());
        assertEquals(null, user.phoneNumber());
        assertEquals(registrationDate, user.registrationDate());
        assertEquals(UserRole.USER, user.role());
        assertEquals(AccountStatus.ACTIVE, user.accountStatus());
    }

    @Test
    void shouldRejectNameWithLessThanThreeCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createUser("Al", "Elias", null)
        );
    }

    @Test
    void shouldTrimNameAndLastName() {
        User user = createUser("  Ana  ", "  Elias  ", null);

        assertEquals("Ana", user.name());
        assertEquals("Elias", user.lastName());
    }

    @Test
    void shouldAcceptNameAndLastNameAtLengthBoundaries() {
        User minimumLengthUser = createUser("Ana", "Gil", null);
        User maximumLengthUser = createUser("Alexandria", "FernandezA", null);

        assertEquals(3, minimumLengthUser.name().length());
        assertEquals(3, minimumLengthUser.lastName().length());
        assertEquals(10, maximumLengthUser.name().length());
        assertEquals(10, maximumLengthUser.lastName().length());
    }

    @Test
    void shouldRejectNullEmptyAndBlankName() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser(null, "Elias", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("", "Elias", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("   ", "Elias", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("\u2003\u2003\u2003", "Elias", null))
        );
    }

    @Test
    void shouldRejectNameOutsideLengthBoundariesAfterTrimming() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("  Al  ", "Elias", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("AlexandriaX", "Elias", null))
        );
    }

    @Test
    void shouldRejectNullEmptyAndBlankLastName() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("Adriana", null, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("Adriana", "", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("Adriana", "   ", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("Adriana", "\u2003\u2003\u2003", null))
        );
    }

    @Test
    void shouldRejectLastNameOutsideLengthBoundariesAfterTrimming() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("Adriana", "  Li  ", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createUser("Adriana", "FernandezABC", null))
        );
    }

    @Test
    void shouldTreatBlankPhoneNumberAsNull() {
        User user = createUser("Adriana", "Elias", "   ");

        assertNull(user.phoneNumber());
    }

    @Test
    void shouldAcceptPhoneNumberWithTwentyCharacters() {
        String phoneNumber = "1".repeat(20);

        User user = createUser("Adriana", "Elias", phoneNumber);

        assertEquals(phoneNumber, user.phoneNumber());
    }

    @Test
    void shouldRejectPhoneNumberLongerThanTwentyCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createUser("Adriana", "Elias", "1".repeat(21))
        );
    }

    private User createUser(String name, String lastName, String phoneNumber) {
        return new User(
                UserId.generate(),
                name,
                lastName,
                new Email("adriana@example.com"),
                new PasswordHash("$2a$10$exampleHashValue"),
                phoneNumber,
                Instant.now(),
                UserRole.USER,
                AccountStatus.ACTIVE
        );
    }
}
