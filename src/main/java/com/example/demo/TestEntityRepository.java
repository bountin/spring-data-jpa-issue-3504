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
            """)
    Page<Result> doCTEWithoutCountQuery(Pageable pageable);

}
