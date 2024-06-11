package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface TestEntityRepository extends JpaRepository<TestEntity, UUID> {

    @Query(value = """
                WITH entities AS (
                    SELECT
                        e.id as id,
                        e.number as number
                    FROM TestEntity e
                )
                SELECT new com.example.demo.Result('X', c.id, c.number)
                FROM entities c
            """,
            // Spring Data JPA currently does not generate a proper countQuery for queries with CTE (see next test)
            countQuery = "SELECT count(e) FROM TestEntity e")
    Page<Result> doSimpleCTE(Pageable pageable);

//    @Query(value = """
//            WITH entities AS (
//                SELECT
//                    e.id as id,
//                    e.number as number
//                FROM TestEntity e
//            )
//            SELECT new com.example.demo.Result('X', c.id, c.number)
//            FROM entities c
//        """)
//    Page<Result> doCTEWithoutCountQuery(Pageable pageable);

    @Query(value = """
                WITH entities AS (
                    SELECT
                        e.id as id,
                        e.number as number
                    FROM TestEntity e
                ),
                combined AS (
                    SELECT 'A' as source,
                        a.id as id,
                        a.number as number
                    FROM entities as a

                    UNION ALL

                    SELECT 'B' as source,
                        b.id as id,
                        b.number as number
                    FROM entities as b
                )

                SELECT new com.example.demo.Result(c.source, c.id, c.number)
                FROM combined as c
            """, countQuery = "SELECT 2 * count(e) FROM TestEntity e")
    Page<Result> doCTEWithUnion(Pageable pageable);
}
