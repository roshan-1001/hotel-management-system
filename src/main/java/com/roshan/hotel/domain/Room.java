package com.roshan.hotel.domain;

import com.roshan.hotel.enums.RoomStatus;
import com.roshan.hotel.enums.RoomType;
import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(name="room_type")
    private RoomType roomType;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    protected Room(){

    }

    public Room(int number, RoomType roomType) {
        this.number = number;
        this.roomType = roomType;
        this.status = RoomStatus.AVAILABLE;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public int getNumber() {
        return number;
    }

    //Methods to set the room status

    public void markOccupied(){
        this.status = RoomStatus.OCCUPIED;
    }
    public void markAvailable(){
        this.status = RoomStatus.AVAILABLE;
    }
    public void markOutOfService(){
        this.status = RoomStatus.OUT_OF_SERVICE;
    }
    public void markReserved(){
        this.status = RoomStatus.RESERVED;
    }

}
