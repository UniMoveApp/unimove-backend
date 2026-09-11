package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateReviewRequest;
import com.unimove.unimove.dto.response.ReviewResponse;
import com.unimove.unimove.exception.InvalidRequestException;
import com.unimove.unimove.exception.ResourceNotFoundException;
import com.unimove.unimove.exception.UnauthorizedException;
import com.unimove.unimove.model.Review;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.Role;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.BookingRepository;
import com.unimove.unimove.repository.ReviewRepository;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private NotificationService notificationService;

    private ReviewService reviewService;

    private User reviewer;
    private User driver;
    private Ride ride;
    private CreateReviewRequest request;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, rideRepository, userRepository, bookingRepository, reviewMapper, notificationService);

        driver = User.builder()
                .id(UUID.randomUUID())
                .username("l.lanese")
                .role(Role.STUDENT)
                .build();

        reviewer = User.builder()
                .id(UUID.randomUUID())
                .username("test.passeggero")
                .role(Role.STUDENT)
                .build();

        ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status("COMPLETED")
                .build();

        request = new CreateReviewRequest();
        request.setRideId(ride.getId());
        request.setRating(5);
        request.setComment("Ottima corsa!");
    }

    @Test
    void createReview_recensioneValida_creaConSuccesso() {
        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(reviewer));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassenger(ride, reviewer)).thenReturn(true);
        when(reviewRepository.existsByRideAndReviewer(ride, reviewer)).thenReturn(false);
        when(reviewMapper.toResponse(any(Review.class))).thenReturn(ReviewResponse.builder().build());

        ReviewResponse response = reviewService.createReview("test.passeggero", request);

        assertThat(response).isNotNull();
        verify(reviewRepository).save(argThat(review ->
                review.getReviewer().equals(reviewer) &&
                        review.getReviewed().equals(driver) &&
                        review.getRide().equals(ride) &&
                        review.getRating() == 5 &&
                        review.getComment().equals("Ottima corsa!")
        ));
    }

    @Test
    void createReview_corsaNonCompleted_lanciaEccezione() {
        ride.setStatus("OPEN");

        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(reviewer));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> reviewService.createReview("test.passeggero", request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Puoi recensire solo corse completate");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_nonPasseggero_lanciaEccezione() {
        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(reviewer));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassenger(ride, reviewer)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview("test.passeggero", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Non sei stato passeggero di questa corsa");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_doppiaRecensione_lanciaEccezione() {
        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(reviewer));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassenger(ride, reviewer)).thenReturn(true);
        when(reviewRepository.existsByRideAndReviewer(ride, reviewer)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview("test.passeggero", request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Hai già recensito questa corsa");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_utenteNonTrovato_lanciaEccezione() {
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview("inesistente", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utente non trovato");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_corsaNonTrovata_lanciaEccezione() {
        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(reviewer));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview("test.passeggero", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Corsa non trovata");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void getUserReviews_utenteConRecensioni_restituisceListaPopolata() {
        Review review = Review.builder()
                .id(UUID.randomUUID())
                .ride(ride)
                .reviewer(reviewer)
                .reviewed(driver)
                .rating(5)
                .build();
        ReviewResponse reviewResponse = ReviewResponse.builder().rating(5).build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(reviewRepository.findByReviewed(driver)).thenReturn(List.of(review));
        when(reviewMapper.toResponse(review)).thenReturn(reviewResponse);

        List<ReviewResponse> results = reviewService.getUserReviews("l.lanese");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getRating()).isEqualTo(5);
    }

    @Test
    void getUserReviews_utenteSenzaRecensioni_restituisceListaVuota() {
        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(reviewRepository.findByReviewed(driver)).thenReturn(List.of());

        List<ReviewResponse> results = reviewService.getUserReviews("l.lanese");

        assertThat(results).isEmpty();
    }

    @Test
    void getUserReviews_utenteNonTrovato_lanciaEccezione() {
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getUserReviews("inesistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utente non trovato");
    }
}