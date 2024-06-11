package com.example.demo;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class DemoApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private TestEntityRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void testSimpleCTEWithPageableOffset() {
        final var entity1 = TestEntity.builder().number(1).build();
        final var entity2 = TestEntity.builder().number(2).build();
        repository.saveAllAndFlush(List.of(entity1, entity2));

        final var pageable = PageRequest.of(1, 1, Sort.by("number"));
        final var result = repository.doSimpleCTE(pageable);

        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(Result::number)
                .isEqualTo(2);

        // This one failed for versions before 3.2.6.
        // 3.2.6 and 3.3.0 are green, but I could not find anything related to this in the release notes.

        // With the Pageable, the offset it also pushed into the CTE, resulting into a double offset: In the CTE out of
        // the two entities, just the second one is returned, and in the outer query, again only the second (now not
        // existent) entity is (not) returned.

        // Generated SQL: (with 3.2.3)
        // with entities (id,number) as (
        //      select te1_0.id,te1_0.number
        //      from test_entity te1_0
        //      order by 2
        //      offset ? rows fetch first ? rows only
        // ) select 'X',c1_0.id,c1_0.number
        // from entities c1_0
        // order by c1_0.number
        // offset ? rows fetch first ? rows only
    }

    // NB: The following test and the tested method must be uncommented manually as it will break the Spring Data
    // repository initialization.
//    @Test
//    void testCTEWithoutSpecificCountQuery() {
//        final var entity1 = TestEntity.builder().number(1).build();
//        final var entity2 = TestEntity.builder().number(2).build();
//        repository.saveAllAndFlush(List.of(entity1, entity2));
//
//        final var pageable = Pageable.unpaged();
//        final var result = repository.doCTEWithoutCountQuery(pageable);
//
//        assertThat(result.getTotalElements()).isEqualTo(2);
//
//        /*
//            With Pageable and no explicit countQuery, Spring Data generates the following HQL:
//            > WITH entities AS(
//            >   SELECT count(e) FROM TestEntity e
//            > ) SELECT count(e) FROM entities c
//
//            Expected: Only the outer projection should count.
//         */
//    }

    @Test
    void testCTEWithUnionAllAndSort() {
        final var entity1 = TestEntity.builder().number(1).build();
        final var entity2 = TestEntity.builder().number(2).build();
        repository.saveAllAndFlush(List.of(entity1, entity2));

        final var pageable = PageRequest.of(0, 10, Sort.by("number"));
        final var result = repository.doCTEWithUnion(pageable);

        assertThat(result)
                .extracting(Result::source, Result::number)
                .containsExactlyInAnyOrder(
                        new Tuple("A", 1),
                        new Tuple("A", 2),
                        new Tuple("B", 1),
                        new Tuple("B", 2)
                );

        /*
        Here the sort is pushed into the UNION with a space missing. Both should not be the case: The sort inside the
        CTE should not be there, and even if that would be valid, a space character is missing.
         */
    }
}
