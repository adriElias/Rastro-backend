package cat.itacademy.rastro.Rastro_backend.user.infrastructure.persistence;

import cat.itacademy.rastro.Rastro_backend.user.domain.model.AccountStatus;
import cat.itacademy.rastro.Rastro_backend.user.domain.model.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(name = "last_name", nullable = false, length = 10)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "registration_date", nullable = false)
    private Instant registrationDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "user_role_enum")
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "account_status", nullable = false, columnDefinition = "account_status_enum")
    private AccountStatus accountStatus;

    protected UserJpaEntity() {
    }

    UserJpaEntity(
            UUID id,
            String name,
            String lastName,
            String email,
            String passwordHash,
            String phoneNumber,
            Instant registrationDate,
            UserRole role,
            AccountStatus accountStatus
    ) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        this.registrationDate = registrationDate;
        this.role = role;
        this.accountStatus = accountStatus;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getLastName() {
        return lastName;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    String getPhoneNumber() {
        return phoneNumber;
    }

    Instant getRegistrationDate() {
        return registrationDate;
    }

    UserRole getRole() {
        return role;
    }

    AccountStatus getAccountStatus() {
        return accountStatus;
    }
}
