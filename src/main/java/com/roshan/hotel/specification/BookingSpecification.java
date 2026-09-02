package com.roshan.hotel.specification;

import com.roshan.hotel.domain.Booking;
import com.roshan.hotel.enums.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

public class BookingSpecification {

    private BookingSpecification(){

    }

    public static Specification<Booking> hasStatus(BookingStatus status){

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);

    }

    public static Specification<Booking> hasRoomNumber(
            Integer roomNumber) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("room").get("number"),
                        roomNumber
                );
    }



}
