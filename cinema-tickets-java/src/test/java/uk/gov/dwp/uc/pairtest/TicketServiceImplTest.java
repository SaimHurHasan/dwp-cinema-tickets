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


}