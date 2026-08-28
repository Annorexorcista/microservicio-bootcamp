package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.adapter;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.BootcampSortBy;
import com.bootcamp.bootcamp.domain.model.BootcampSortDirection;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampCapabilityEntity;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.mapper.BootcampEntityMapper;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampCapabilityRepository;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampRepository;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración del {@link BootcampPersistenceAdapter} contra una base de
 * datos MySQL real levantada con Testcontainers.
 *
 * <p>Verifica el comportamiento del adaptador driven R2DBC de extremo a extremo
 * (adaptador + mapper + repositorios + tabla puente), usando {@link StepVerifier}
 * para comprobar los flujos reactivos sin bloquear: {@code save} persiste el
 * bootcamp y sus asociaciones y retorna un {@link Bootcamp} con id asignado
 * (Req 1.2, 1.3).
 *
 * <p>Se usa un {@code GenericContainer<>("mysql:8.0")} (NO {@code MySQLContainer})
 * porque el proyecto es puramente R2DBC y no tiene el driver JDBC de MySQL en el
 * classpath de test; {@code MySQLContainer} verificaría el arranque abriendo una
 * conexión JDBC y fallaría con {@code ClassNotFoundException}.
 *
 * <p>Requiere Docker en ejecución.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BootcampPersistenceAdapterIT {

    private static final int MYSQL_PORT = 3306;
    private static final String DB_NAME = "bootcamp_db";
    private static final String DB_USER = "test";
    private static final String DB_PASSWORD = "test";

    private static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0")
            .withEnv("MYSQL_DATABASE", DB_NAME)
            .withEnv("MYSQL_USER", DB_USER)
            .withEnv("MYSQL_PASSWORD", DB_PASSWORD)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withExposedPorts(MYSQL_PORT)
            .waitingFor(Wait.forLogMessage(".*port: 3306  MySQL Community Server.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(180)));

    private ConnectionFactory connectionFactory;
    private IBootcampRepository bootcampRepository;
    private IBootcampCapabilityRepository bootcampCapabilityRepository;
    private BootcampPersistenceAdapter adapter;

    @BeforeAll
    void startContainer() {
        MYSQL.start();

        this.connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "mysql")
                .option(HOST, MYSQL.getHost())
                .option(PORT, MYSQL.getMappedPort(MYSQL_PORT))
                .option(USER, DB_USER)
                .option(PASSWORD, DB_PASSWORD)
                .option(DATABASE, DB_NAME)
                .build());

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new org.springframework.core.io.ClassPathResource("schema.sql"));
        populator.populate(connectionFactory).block();

        R2dbcEntityTemplate entityTemplate = new R2dbcEntityTemplate(connectionFactory);
        R2dbcRepositoryFactory repositoryFactory = new R2dbcRepositoryFactory(entityTemplate);
        this.bootcampRepository = repositoryFactory.getRepository(IBootcampRepository.class);
        this.bootcampCapabilityRepository =
                repositoryFactory.getRepository(IBootcampCapabilityRepository.class);

        R2dbcTransactionManager transactionManager = new R2dbcTransactionManager(connectionFactory);
        TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);

        this.adapter = new BootcampPersistenceAdapter(
                bootcampRepository,
                bootcampCapabilityRepository,
                new BootcampEntityMapper(),
                transactionalOperator,
                entityTemplate);
    }

    @AfterAll
    void stopContainer() {
        MYSQL.stop();
    }

    @BeforeEach
    void cleanTables() {
        // Primero las asociaciones (FK hacia bootcamp), luego los bootcamps.
        bootcampCapabilityRepository.deleteAll().block();
        bootcampRepository.deleteAll().block();
    }

    @Test
    void save_assignsGeneratedId_andReturnsPersistedBootcamp() {
        Bootcamp toSave = new Bootcamp(
                null, "Backend 2026", "Bootcamp backend",
                LocalDate.of(2026, 3, 1), 84, List.of(1L, 2L, 3L));

        StepVerifier.create(adapter.save(toSave))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getId()).isPositive();
                    assertThat(saved.getName()).isEqualTo("Backend 2026");
                    assertThat(saved.getDescription()).isEqualTo("Bootcamp backend");
                    assertThat(saved.getLaunchDate()).isEqualTo(LocalDate.of(2026, 3, 1));
                    assertThat(saved.getDurationInDays()).isEqualTo(84);
                    assertThat(saved.getCapabilityIds()).containsExactly(1L, 2L, 3L);
                })
                .verifyComplete();
    }

    @Test
    void save_actuallyPersistsBootcampAndAssociations() {
        Bootcamp toSave = new Bootcamp(
                null, "Frontend 2026", "Bootcamp frontend",
                LocalDate.of(2026, 5, 10), 60, List.of(10L, 20L));

        Long generatedId = adapter.save(toSave).map(Bootcamp::getId).block();
        assertThat(generatedId).isNotNull();

        StepVerifier.create(bootcampRepository.findById(generatedId))
                .assertNext(entity -> {
                    assertThat(entity.getName()).isEqualTo("Frontend 2026");
                    assertThat(entity.getLaunchDate()).isEqualTo(LocalDate.of(2026, 5, 10));
                    assertThat(entity.getDurationDays()).isEqualTo(60);
                })
                .verifyComplete();

        StepVerifier.create(
                        bootcampCapabilityRepository.findByBootcampId(generatedId)
                                .map(BootcampCapabilityEntity::getCapabilityId)
                                .sort()
                                .collectList())
                .assertNext(capIds -> assertThat(capIds).containsExactly(10L, 20L))
                .verifyComplete();
    }

    @Test
    void save_withSingleCapability_persistsOneAssociation() {
        Bootcamp toSave = new Bootcamp(
                null, "DevOps 2026", "Bootcamp devops",
                LocalDate.of(2026, 7, 1), 30, List.of(5L));

        Long generatedId = adapter.save(toSave).map(Bootcamp::getId).block();

        StepVerifier.create(bootcampCapabilityRepository.findByBootcampId(generatedId).count())
                .expectNext(1L)
                .verifyComplete();
    }

    // --- findPage / countAll (HU5) ---

    private BootcampPageQuery query(int page, int size,
                                    BootcampSortBy sortBy, BootcampSortDirection dir) {
        return new BootcampPageQuery(page, size, sortBy, dir);
    }

    /** Siembra: "Beta"(2 caps), "Alfa"(1 cap), "Gamma"(0 caps). */
    private void seedForListing() {
        bootcampCapabilityRepository.deleteAll().block();
        bootcampRepository.deleteAll().block();
        adapter.save(new Bootcamp(null, "Beta", "d", LocalDate.of(2026, 3, 1), 30, List.of(1L, 2L))).block();
        adapter.save(new Bootcamp(null, "Alfa", "d", LocalDate.of(2026, 3, 1), 30, List.of(1L))).block();
        // "Gamma" sin capacidades: se inserta sin filas en la tabla puente.
        bootcampRepository.save(new com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity
                .BootcampEntity(null, "Gamma", "d", LocalDate.of(2026, 3, 1), 30)).block();
    }

    @Test
    void findPage_orderByNameAscAndDesc() {
        seedForListing();

        StepVerifier.create(adapter.findPage(query(0, 10, BootcampSortBy.NAME, BootcampSortDirection.ASC))
                        .map(Bootcamp::getName).collectList())
                .assertNext(names -> assertThat(names).containsExactly("Alfa", "Beta", "Gamma"))
                .verifyComplete();

        StepVerifier.create(adapter.findPage(query(0, 10, BootcampSortBy.NAME, BootcampSortDirection.DESC))
                        .map(Bootcamp::getName).collectList())
                .assertNext(names -> assertThat(names).containsExactly("Gamma", "Beta", "Alfa"))
                .verifyComplete();
    }

    @Test
    void findPage_orderByCapabilityCount() {
        seedForListing();

        // Gamma(0) primero asc, Beta(2) último asc.
        StepVerifier.create(adapter.findPage(query(0, 10, BootcampSortBy.CAPABILITY_COUNT,
                                BootcampSortDirection.ASC))
                        .map(Bootcamp::getName).collectList())
                .assertNext(names -> assertThat(names).containsExactly("Gamma", "Alfa", "Beta"))
                .verifyComplete();
    }

    @Test
    void findPage_limitOffsetReturnsWindow() {
        seedForListing();

        StepVerifier.create(adapter.findPage(query(1, 2, BootcampSortBy.NAME, BootcampSortDirection.ASC))
                        .map(Bootcamp::getName).collectList())
                .assertNext(names -> assertThat(names).containsExactly("Gamma"))
                .verifyComplete();
    }

    @Test
    void findPage_resolvesCapabilityIds() {
        seedForListing();

        StepVerifier.create(adapter.findPage(query(0, 1, BootcampSortBy.NAME, BootcampSortDirection.ASC))
                        .collectList())
                .assertNext(list -> {
                    assertThat(list.get(0).getName()).isEqualTo("Alfa");
                    assertThat(list.get(0).getCapabilityIds()).containsExactly(1L);
                })
                .verifyComplete();
    }

    @Test
    void countAll_returnsTotal() {
        seedForListing();

        StepVerifier.create(adapter.countAll())
                .expectNext(3L)
                .verifyComplete();
    }
}
