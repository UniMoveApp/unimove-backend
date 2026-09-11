package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateReviewRequest;
import com.unimove.unimove.dto.response.ReviewResponse;
import com.unimove.unimove.exception.InvalidRequestException;
import com.unimove.unimove.exception.ResourceNotFoundException;
import com.unimove.unimove.exception.UnauthorizedException;
import com.unimove.unimove.model.Review;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.BookingRepository;
import com.unimove.unimove.repository.ReviewRepository;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private static final String UTENTE_NON_TROVATO = "Utente non trovato";
    private static final String CORSA_NON_TROVATA = "Corsa non trovata";

    private final ReviewRepository reviewRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReviewMapper reviewMapper;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepository, RideRepository rideRepository, UserRepository userRepository, BookingRepository bookingRepository, ReviewMapper reviewMapper, NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.reviewMapper = reviewMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReviewResponse createReview(String username, CreateReviewRequest request) {

        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(UTENTE_NON_TROVATO));

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException(CORSA_NON_TROVATA));

        if (!ride.getStatus().equals("COMPLETED")) {
            throw new InvalidRequestException("Puoi recensire solo corse completate");
        }

        if (!bookingRepository.existsByRideAndPassenger(ride, reviewer)) {
            throw new UnauthorizedException("Non sei stato passeggero di questa corsa");
        }

        if (reviewRepository.existsByRideAndReviewer(ride, reviewer)) {
            throw new InvalidRequestException("Hai già recensito questa corsa");
        }

        User reviewed = ride.getDriver();

        Review review = Review.builder()
                .ride(ride)
                .reviewer(reviewer)
                .reviewed(reviewed)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);

        // Notify the reviewed user (RF-8.3)
        String msg = String.format("Hai ricevuto una nuova recensione da %s per la corsa da %s a %s (%d stelle).",
                reviewer.getFullName(),
                ride.getDepartureCity(),
                ride.getArrivalCity(),
                review.getRating());
        notificationService.sendNotification(reviewed, "NEW_REVIEW", msg, ride);

        return reviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getUserReviews(String username) {

        User reviewed = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(UTENTE_NON_TROVATO));

        return reviewRepository.findByReviewed(reviewed)
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

}
