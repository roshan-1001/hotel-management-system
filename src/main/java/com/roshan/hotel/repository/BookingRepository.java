package com.roshan.hotel.repository;

import com.roshan.hotel.domain.Booking;
import com.roshan.hotel.domain.Room;
import com.roshan.hotel.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking,Long>, JpaSpecificationExecutor<Booking> {

    List<Booking> findByRoom(Room room);

    Page<Booking> findByStatus(
            BookingStatus status,
            Pageable pageable
    );

}
