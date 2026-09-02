package com.roshan.hotel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "receptionists")
public class Receptionist {

    @Id
    private int id;

    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    protected Receptionist(){
    }

    public Receptionist(int id, String name, LocalDate startDate) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public int getId() {
        return id;
    }
}
