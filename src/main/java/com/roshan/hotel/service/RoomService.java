package com.roshan.hotel.service;

import com.roshan.hotel.domain.Room;
import com.roshan.hotel.enums.RoomType;
import com.roshan.hotel.exception.RoomAlreadyExistsException;
import com.roshan.hotel.exception.RoomNotFoundException;
import com.roshan.hotel.repository.RoomRepository;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository){
        this.roomRepository = roomRepository;
    }

    public Room createRoom(int number, RoomType roomType){

        if(roomRepository.existsById(number)){
            throw new RoomAlreadyExistsException("Room with number: " + number + " already exists");
        }

        Room room = new Room(number,roomType);
        return roomRepository.save(room);


    }

    public Room getRoom (int number){
        return roomRepository.findById(number).orElseThrow(()->
                new RoomNotFoundException(
                        "Room does not exist with room number " + number
                )
        );
    }

}
