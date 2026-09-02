package com.roshan.hotel.service;

import com.roshan.hotel.domain.Hotel;
import com.roshan.hotel.exception.HotelAlreadyExistsException;
import com.roshan.hotel.exception.HotelNotFoundException;
import com.roshan.hotel.repository.HotelRepository;
import org.springframework.stereotype.Service;

@Service
public class HotelService {

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository){
        this.hotelRepository = hotelRepository;
    }

    public Hotel createHotel(long id, String name, String address){
        Hotel hotel = new Hotel(id, name, address);
        if(hotelRepository.findById(id) != null){
            throw new HotelAlreadyExistsException("The hotel with id: " + id + " already exists");
        }
        hotelRepository.save(hotel);
        return hotel;
    }

    public Hotel getHotel(long id){
        Hotel hotel = hotelRepository.findById(id);
        if(hotel == null){
            throw new HotelNotFoundException("Hotel with id: " + id + " does not exist");
        }
        return hotel;
    }

    public Hotel updateDescription(long id, String description){
        Hotel hotel = getHotel(id);
        hotel.updateDescription(description);
        hotelRepository.save(hotel);
        return hotel;
    }


}
