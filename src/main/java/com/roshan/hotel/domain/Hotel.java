package com.roshan.hotel.domain;

public class Hotel {

    private final long id;
    private final String name;
    private final String address;
    private String description;

    public Hotel(long id, String name, String address){
        this.address = address;
        this.name = name;
        this.id = id;
    }


    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public void updateDescription(String description){
        this.description = description;
    }

    public String getDescription(){ return description;}

}
