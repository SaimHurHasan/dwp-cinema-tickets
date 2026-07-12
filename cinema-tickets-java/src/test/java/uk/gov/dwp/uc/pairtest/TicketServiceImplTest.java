package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }


    // 1. Account ID Validation


    @Nested
    @DisplayName("Account ID validation")
    class AccountIdValidation {

        @Test
        @DisplayName("Throws when account ID is zero")
        void throwsWhenAccountIdIsZero() {
            TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(0L, request));
        }

        @Test
        @DisplayName("Throws when account ID is negative")
        void throwsWhenAccountIdIsNegative() {
            TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(-1L, request));
        }

        @Test
        @DisplayName("Throws when account ID is null")
        void throwsWhenAccountIdIsNull() {
            TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(null, request));
        }

        @Test
        @DisplayName("Accepts minimum valid account ID of 1")
        void acceptsMinimumValidAccountId() {
            TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, request));
        }
    }

    // 2. Request Payload Validation


    @Nested
    @DisplayName("Request payload validation")
    class RequestPayloadValidation {

        @Test
        @DisplayName("Throws when no ticket requests are provided")
        void throwsWhenNoRequestsProvided() {
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L));
        }

        @Test
        @DisplayName("Throws when ticket requests array is null")
        void throwsWhenRequestsArrayIsNull() {
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null));
        }

        @Test
        @DisplayName("Throws when any individual request is null")
        void throwsWhenIndividualRequestIsNull() {
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L, (TicketTypeRequest) null));
        }

        @Test
        @DisplayName("Throws when a request specifies zero tickets")
        void throwsWhenRequestHasZeroTickets() {
            TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L, request));
        }

        @Test
        @DisplayName("Throws when a request specifies a negative number of tickets")
        void throwsWhenRequestHasNegativeTickets() {
            TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -3);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L, request));
        }
    }

    // 3. Business Rule Validation


    @Nested
    @DisplayName("Business rule validation")
    class BusinessRuleValidation {

        @Test
        @DisplayName("Throws when only Child tickets are requested (no Adult)")
        void throwsWhenChildTicketsWithoutAdult() {
            TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L, childRequest));
        }

        @Test
        @DisplayName("Throws when only Infant tickets are requested (no Adult)")
        void throwsWhenInfantTicketsWithoutAdult() {
            TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L, infantRequest));
        }

        @Test
        @DisplayName("Throws when Child and Infant tickets are requested without Adult")
        void throwsWhenChildAndInfantWithoutAdult() {
            TicketTypeRequest childRequest  = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
            TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L, childRequest, infantRequest));
        }

        @Test
        @DisplayName("Throws when total ticket count exceeds 25")
        void throwsWhenTotalTicketsExceedMaximum() {
            TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
            TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 6);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L, adultRequest, childRequest));
        }

        @Test
        @DisplayName("Accepts exactly 25 tickets (the maximum)")
        void acceptsExactlyMaximumTickets() {
            TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);
            assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, adultRequest));
        }

        @Test
        @DisplayName("Throws when infants outnumber adults")
        void throwsWhenInfantsOutnumberAdults() {
            TicketTypeRequest adultRequest  = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
            assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(1L, adultRequest, infantRequest));
        }

        @Test
        @DisplayName("Accepts when infants equal adults (each infant on one adult's lap)")
        void acceptsWhenInfantsEqualAdults() {
            TicketTypeRequest adultRequest  = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);
            TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);
            assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, adultRequest, infantRequest));
        }
    }

// 4. Payment Calculation


    @Nested
    @DisplayName("Payment calculation")
    class PaymentCalculation {

        @Test
        @DisplayName("Charges £25 for one Adult ticket")
        void chargesCorrectlyForOneAdult() {
            TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            ticketService.purchaseTickets(1L, adultRequest);
            verify(ticketPaymentService).makePayment(1L, 25);
        }

        @Test
        @DisplayName("Charges £50 for two Adult tickets")
        void chargesCorrectlyForTwoAdults() {
            TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
            ticketService.purchaseTickets(1L, adultRequest);
            verify(ticketPaymentService).makePayment(1L, 50);
        }

        @Test
        @DisplayName("Charges £40 for one Adult and one Child ticket")
        void chargesCorrectlyForOneAdultAndOneChild() {
            TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
            ticketService.purchaseTickets(1L, adultRequest, childRequest);
            verify(ticketPaymentService).makePayment(1L, 40); // £25 + £15
        }

        @Test
        @DisplayName("Infants are free — charge does not include infant cost")
        void infantsAreChargedNothing() {
            TicketTypeRequest adultRequest  = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
            ticketService.purchaseTickets(1L, adultRequest, infantRequest);
            verify(ticketPaymentService).makePayment(1L, 25); // Only the adult's £25
        }

        @Test
        @DisplayName("Mixed purchase: 2 Adults + 3 Children + 1 Infant = £95")
        void chargesCorrectlyForMixedPurchase() {
            // (2 × £25) + (3 × £15) + (1 × £0) = £50 + £45 = £95
            TicketTypeRequest adultRequest  = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
            TicketTypeRequest childRequest  = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);
            TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
            ticketService.purchaseTickets(1L, adultRequest, childRequest, infantRequest);
            verify(ticketPaymentService).makePayment(1L, 95);
        }

        @Test
        @DisplayName("Charges £625 for maximum 25 Adult tickets")
        void chargesCorrectlyForMaxAdults() {
            TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);
            ticketService.purchaseTickets(1L, adultRequest);
            verify(ticketPaymentService).makePayment(1L, 625);
        }
    }


    // 5. Seat Reservation Calculation


    @Nested
    @DisplayName("Seat reservation calculation")
    class SeatReservationCalculation {

        @Test
        @DisplayName("Reserves 1 seat for one Adult")
        void reservesOneSeatForOneAdult() {
            TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            ticketService.purchaseTickets(1L, adultRequest);
            verify(seatReservationService).reserveSeat(1L, 1);
        }

        @Test
        @DisplayName("Reserves 2 seats for one Adult and one Child")
        void reservesTwoSeatsForAdultAndChild() {
            TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
            TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
            ticketService.purchaseTickets(1L, adultRequest, childRequest);
            verify(seatReservationService).reserveSeat(1L, 2);
        }

        @Test
        @DisplayName("Infants do not receive a seat")
        void infantsDoNotGetSeats() {
            TicketTypeRequest adultRequest  = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
            TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
            ticketService.purchaseTickets(1L, adultRequest, infantRequest);
            verify(seatReservationService).reserveSeat(1L, 2); // Only adults get seats
        }

        @Test
        @DisplayName("Mixed purchase: 2 Adults + 3 Children + 1 Infant reserves 5 seats")
        void reservesCorrectSeatsForMixedPurchase() {
            TicketTypeRequest adultRequest  = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
            TicketTypeRequest childRequest  = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);
            TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
            ticketService.purchaseTickets(1L, adultRequest, childRequest, infantRequest);
            verify(seatReservationService).reserveSeat(1L, 5); // 2 adults + 3 children
        }
    }


}