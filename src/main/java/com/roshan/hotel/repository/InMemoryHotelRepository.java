package com.roshan.hotel.repository;

import com.roshan.hotel.domain.Hotel;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class InMemoryHotelRepository implements HotelRepository {

    private final Map<Long, Hotel> hotelRepo = new HashMap<>();

    @Override
    public void save(Hotel hotel) {
        hotelRepo.put(hotel.getId(), hotel);
    }

    @Override
    public Hotel findById(long id) {
        return hotelRepo.get(id);
    }
}
