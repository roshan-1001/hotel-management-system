package com.roshan.hotel.repository;

import com.roshan.hotel.domain.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Integer> {

    //Normal read - test
    Optional<Room> findByNumber(int number);

    //Pessimistic read - production
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Room r
            WHERE r.number = :number
            """)
    Optional<Room> findByNumberForUpdate(
            @Param("number") int number
    );
}