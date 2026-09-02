package com.roshan.hotel.repository;

import com.roshan.hotel.domain.Hotel;

public interface HotelRepository {

    void save(Hotel hotel);
    Hotel findById(long id);

}
