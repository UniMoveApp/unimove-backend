package com.unimove.unimove.controller;

import com.unimove.unimove.dto.request.CreateBookingRequest;
import com.unimove.unimove.dto.response.BookingResponse;
import com.unimove.unimove.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(Principal principal, @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(201).body(bookingService.createBooking(principal.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(Principal principal, @PathVariable UUID id) {
        bookingService.cancelBooking(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Principal principal) {
        return ResponseEntity.ok(bookingService.getMyBookings(principal.getName()));
    }
}
