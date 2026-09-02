package com.roshan.hotel.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guests")
public class Guest {

    @Id
    private int id;
    private String name;

    @OneToMany(mappedBy = "guest", cascade = CascadeType.PERSIST)
    private List<Booking> bookings = new ArrayList<>();

    protected Guest(){

    }

    public Guest(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Booking> getBookings(){
        return bookings;
    }

    public void changeName(String name){
        this.name = name;
    }
}
