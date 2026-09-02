package com.roshan.hotel.demos;

import com.roshan.hotel.domain.Booking;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class HibernateDemo {

    public static void main(String[] args) {

        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("hotelPU");

        EntityManager em =
                emf.createEntityManager();

        Booking booking =
                em.find(Booking.class, 1001L);

        System.out.println("Booking loaded: " + booking.getId());

        System.out.println(
                "Guest: " + booking.getGuest().getName()
        );

        System.out.println(
                "Room: " + booking.getRoom().getNumber()
        );

        System.out.println(
                "Receptionist: " + booking.getReceptionist().getName()
        );

        em.close();
        emf.close();
    }
}