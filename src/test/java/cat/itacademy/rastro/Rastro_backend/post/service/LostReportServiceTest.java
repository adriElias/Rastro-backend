package cat.itacademy.rastro.Rastro_backend.post.service;

import cat.itacademy.rastro.Rastro_backend.post.entity.AgeUnit;
import cat.itacademy.rastro.Rastro_backend.post.entity.AnimalTypeEntity;
import cat.itacademy.rastro.Rastro_backend.post.entity.LocationEntity;
import cat.itacademy.rastro.Rastro_backend.post.entity.PostEntity;
import cat.itacademy.rastro.Rastro_backend.post.entity.PostType;
import cat.itacademy.rastro.Rastro_backend.post.entity.PublicationStatusEntity;
import cat.itacademy.rastro.Rastro_backend.post.repository.AnimalTypeRepository;
import cat.itacademy.rastro.Rastro_backend.post.repository.LocationRepository;
import cat.itacademy.rastro.Rastro_backend.post.repository.PostRepository;
import cat.itacademy.rastro.Rastro_backend.post.repository.PublicationStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LostReportServiceTest {

    private static final UUID USER_ID = UUID.fromString("8b24cc18-f243-4dc6-a809-c755c28d61a4");
    private static final UUID ANIMAL_TYPE_ID = UUID.fromString("0a33567f-b9c1-45ad-bd11-100984514e9e");
    private static final UUID STATUS_ID = UUID.fromString("a666a092-c49d-4d22-81c4-c15e1dd6ff4f");
    private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");

    @Mock
    private PostRepository postRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AnimalTypeRepository animalTypeRepository;

    @Mock
    private PublicationStatusRepository publicationStatusRepository;

    private LostReportService service;

    @BeforeEach
    void setUp() {
        service = new LostReportService(
                postRepository,
                locationRepository,
                animalTypeRepository,
                publicationStatusRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @ParameterizedTest
    @EnumSource(AgeUnit.class)
    void shouldCreateLostReportWithSupportedAgeUnit(AgeUnit ageUnit) {
        stubReferences();
        when(locationRepository.save(any(LocationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(postRepository.save(any(PostEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LostReportResult result = service.create(
                USER_ID,
                validCommand(age(2), ageUnit, "https://example.com/animal.jpg")
        );

        ArgumentCaptor<LocationEntity> locationCaptor = ArgumentCaptor.forClass(LocationEntity.class);
        ArgumentCaptor<PostEntity> postCaptor = ArgumentCaptor.forClass(PostEntity.class);
        InOrder persistenceOrder = inOrder(locationRepository, postRepository);
        persistenceOrder.verify(locationRepository).save(locationCaptor.capture());
        persistenceOrder.verify(postRepository).save(postCaptor.capture());

        LocationEntity location = locationCaptor.getValue();
        PostEntity post = postCaptor.getValue();

        assertEquals(new BigDecimal("41.387400"), location.getLatitude());
        assertEquals(new BigDecimal("2.168600"), location.getLongitude());
        assertEquals(USER_ID, post.getUserId());
        assertEquals("Lost dog", post.getTitle());
        assertEquals("Brown dog with a blue collar", post.getDescription());
        assertEquals(ANIMAL_TYPE_ID, post.getAnimalTypeId());
        assertEquals(STATUS_ID, post.getPublicationStatusId());
        assertEquals(PostType.LOST, post.getPostType());
        assertFalse(post.isSafekeeping());
        assertEquals(2, post.getApproxAge());
        assertEquals(ageUnit, post.getAgeUnit());
        assertEquals("https://example.com/animal.jpg", post.getPhoto());
        assertEquals(NOW, post.getCreatedDate());
        assertEquals(NOW, post.getLastUpdate());
        assertEquals(NOW.minusSeconds(3600), post.getLostAt());
        assertEquals(new BigDecimal("5.00"), post.getSearchRadius());
        assertEquals(location.getId(), post.getLocationId());

        assertEquals(post.getId(), result.id());
        assertEquals(ageUnit, result.ageUnit());
        assertEquals("ACTIVE", result.status());
        assertEquals(NOW, result.createdAt());
    }

    @Test
    void shouldCreateLostReportWithoutOptionalAgeOrPhoto() {
        stubReferences();
        when(locationRepository.save(any(LocationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(postRepository.save(any(PostEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LostReportResult result = service.create(USER_ID, validCommand(null, null, null));

        ArgumentCaptor<PostEntity> postCaptor = ArgumentCaptor.forClass(PostEntity.class);
        verify(postRepository).save(postCaptor.capture());
        assertNull(postCaptor.getValue().getApproxAge());
        assertNull(postCaptor.getValue().getAgeUnit());
        assertNull(postCaptor.getValue().getPhoto());
        assertNull(result.approxAge());
        assertNull(result.ageUnit());
        assertNull(result.photoUrl());
    }

    @Test
    void shouldReportAllMissingRequiredFieldsBeforeUsingRepositories() {
        CreateLostReportCommand command = new CreateLostReportCommand(
                " ", null, null, null, null, null, null, null, null, null
        );

        InvalidLostReportException exception = assertThrows(
                InvalidLostReportException.class,
                () -> service.create(USER_ID, command)
        );

        assertEquals(
                Map.of(
                        "title", "must not be blank",
                        "description", "must not be blank",
                        "animalType", "must not be blank",
                        "lostAt", "must not be null",
                        "latitude", "must not be null",
                        "longitude", "must not be null",
                        "searchRadiusKm", "must not be null"
                ),
                exception.fieldErrors()
        );
        verifyNoInteractions(
                animalTypeRepository,
                publicationStatusRepository,
                locationRepository,
                postRepository
        );
    }

    @Test
    void shouldRejectLostAtAfterCreationTime() {
        assertValidationError(
                withLostAt(validCommand(null, null, null), NOW.plusSeconds(1)),
                "lostAt"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidCoordinates")
    void shouldRejectInvalidCoordinates(BigDecimal latitude, BigDecimal longitude, String field) {
        assertValidationError(
                withCoordinates(validCommand(null, null, null), latitude, longitude),
                field
        );
    }

    @ParameterizedTest
    @MethodSource("invalidSearchRadii")
    void shouldRejectInvalidSearchRadius(BigDecimal searchRadius) {
        assertValidationError(
                withSearchRadius(validCommand(null, null, null), searchRadius),
                "searchRadiusKm"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidApproximateAges")
    void shouldRejectInvalidApproximateAge(Integer approximateAge) {
        assertValidationError(
                validCommand(approximateAge, AgeUnit.YEARS, null),
                "approxAge"
        );
    }

    @Test
    void shouldRejectApproximateAgeWithoutUnit() {
        InvalidLostReportException exception = assertValidationError(
                validCommand(age(3), null, null),
                "ageUnit"
        );
        assertEquals("must be provided together with approxAge", exception.fieldErrors().get("ageUnit"));
    }

    @Test
    void shouldRejectAgeUnitWithoutApproximateAge() {
        InvalidLostReportException exception = assertValidationError(
                validCommand(null, AgeUnit.MONTHS, null),
                "approxAge"
        );
        assertEquals("must be provided together with ageUnit", exception.fieldErrors().get("approxAge"));
    }

    @ParameterizedTest
    @MethodSource("invalidPhotoUrls")
    void shouldRejectInvalidPhotoUrl(String photoUrl) {
        assertValidationError(validCommand(null, null, photoUrl), "photoUrl");
    }

    @Test
    void shouldRejectTitleLongerThanDatabaseLimit() {
        assertValidationError(
                withTitle(validCommand(null, null, null), "a".repeat(151)),
                "title"
        );
    }

    @Test
    void shouldRejectUnknownAnimalTypeWithoutPersisting() {
        when(animalTypeRepository.findByTitle("HORSE")).thenReturn(Optional.empty());

        InvalidLostReportException exception = assertThrows(
                InvalidLostReportException.class,
                () -> service.create(
                        USER_ID,
                        withAnimalType(validCommand(null, null, null), " horse ")
                )
        );

        assertEquals("unsupported animal type", exception.fieldErrors().get("animalType"));
        verify(animalTypeRepository).findByTitle("HORSE");
        verifyNoInteractions(locationRepository, postRepository);
        verify(publicationStatusRepository, never()).findByTitle(any());
    }

    @Test
    void shouldNotPersistWhenActiveStatusIsMissing() {
        when(animalTypeRepository.findByTitle("DOG"))
                .thenReturn(Optional.of(new AnimalTypeEntity(ANIMAL_TYPE_ID, "DOG")));
        when(publicationStatusRepository.findByTitle("ACTIVE")).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.create(USER_ID, validCommand(null, null, null))
        );

        verifyNoInteractions(locationRepository, postRepository);
    }

    private InvalidLostReportException assertValidationError(
            CreateLostReportCommand command,
            String expectedField
    ) {
        InvalidLostReportException exception = assertThrows(
                InvalidLostReportException.class,
                () -> service.create(USER_ID, command)
        );
        assertTrue(exception.fieldErrors().containsKey(expectedField));
        verifyNoInteractions(
                animalTypeRepository,
                publicationStatusRepository,
                locationRepository,
                postRepository
        );
        return exception;
    }

    private void stubReferences() {
        when(animalTypeRepository.findByTitle("DOG"))
                .thenReturn(Optional.of(new AnimalTypeEntity(ANIMAL_TYPE_ID, "DOG")));
        when(publicationStatusRepository.findByTitle("ACTIVE"))
                .thenReturn(Optional.of(new PublicationStatusEntity(STATUS_ID, "ACTIVE")));
    }

    private CreateLostReportCommand validCommand(
            Integer approximateAge,
            AgeUnit ageUnit,
            String photoUrl
    ) {
        return new CreateLostReportCommand(
                " Lost dog ",
                " Brown dog with a blue collar ",
                " dog ",
                NOW.minusSeconds(3600),
                new BigDecimal("41.387400"),
                new BigDecimal("2.168600"),
                new BigDecimal("5.00"),
                approximateAge,
                ageUnit,
                photoUrl
        );
    }

    private CreateLostReportCommand withLostAt(CreateLostReportCommand command, Instant lostAt) {
        return copy(command, command.title(), command.animalType(), lostAt,
                command.latitude(), command.longitude(), command.searchRadiusKm());
    }

    private CreateLostReportCommand withCoordinates(
            CreateLostReportCommand command,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        return copy(command, command.title(), command.animalType(), command.lostAt(),
                latitude, longitude, command.searchRadiusKm());
    }

    private CreateLostReportCommand withSearchRadius(
            CreateLostReportCommand command,
            BigDecimal searchRadius
    ) {
        return copy(command, command.title(), command.animalType(), command.lostAt(),
                command.latitude(), command.longitude(), searchRadius);
    }

    private CreateLostReportCommand withTitle(CreateLostReportCommand command, String title) {
        return copy(command, title, command.animalType(), command.lostAt(),
                command.latitude(), command.longitude(), command.searchRadiusKm());
    }

    private CreateLostReportCommand withAnimalType(CreateLostReportCommand command, String animalType) {
        return copy(command, command.title(), animalType, command.lostAt(),
                command.latitude(), command.longitude(), command.searchRadiusKm());
    }

    private CreateLostReportCommand copy(
            CreateLostReportCommand command,
            String title,
            String animalType,
            Instant lostAt,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal searchRadius
    ) {
        return new CreateLostReportCommand(
                title,
                command.description(),
                animalType,
                lostAt,
                latitude,
                longitude,
                searchRadius,
                command.approxAge(),
                command.ageUnit(),
                command.photoUrl()
        );
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidCoordinates() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        new BigDecimal("90.000001"), new BigDecimal("2.0"), "latitude"
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        new BigDecimal("-90.000001"), new BigDecimal("2.0"), "latitude"
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        new BigDecimal("41.0"), new BigDecimal("180.000001"), "longitude"
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        new BigDecimal("41.0"), new BigDecimal("-180.000001"), "longitude"
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        new BigDecimal("41.1234567"), new BigDecimal("2.0"), "latitude"
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        new BigDecimal("41.0"), new BigDecimal("2.1234567"), "longitude"
                )
        );
    }

    private static Stream<BigDecimal> invalidSearchRadii() {
        return Stream.of(
                BigDecimal.ZERO,
                new BigDecimal("-0.01"),
                new BigDecimal("50.01"),
                new BigDecimal("1.001")
        );
    }

    private static Stream<Integer> invalidApproximateAges() {
        return Stream.of(
                0,
                -1
        );
    }

    private static Integer age(int value) {
        return value;
    }

    private static Stream<String> invalidPhotoUrls() {
        return Stream.of(
                "http://example.com/animal.jpg",
                "https:///missing-host.jpg",
                "https://example.com/" + "a".repeat(2049)
        );
    }
}
