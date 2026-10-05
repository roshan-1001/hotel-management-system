package com.roshan.hotel.repository;

import com.roshan.hotel.domain.Booking;
import com.roshan.hotel.domain.Room;
import com.roshan.hotel.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking,Long>, JpaSpecificationExecutor<Booking> {

    List<Booking> findByRoom(Room room);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(""" 
           SELECT b from Booking b WHERE b.id = :id
           """)
    Optional<Booking> findByIdForUpdate(@Param("id") long id);

    @Query("""
    SELECT b
    FROM Booking b
    WHERE b.status = :status
      AND b.expiresAt <= :now
""")
    List<Booking> findExpiredPendingBookings(
            @Param("status") BookingStatus status,
            @Param("now") Instant now
    );

    Page<Booking> findByStatus(
            BookingStatus status,
            Pageable pageable
    );


    @EntityGraph(attributePaths = {"guest", "room"})
    @Override
    Page<Booking> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"guest", "room"})
    Page<Booking> findAll(
            Specification<Booking> specification,
            Pageable pageable
    );

}
