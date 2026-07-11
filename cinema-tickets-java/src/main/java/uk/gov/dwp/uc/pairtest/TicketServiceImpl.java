package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

/**
 * Implementation of TicketService that validates purchase requests,
 * calculates costs and seat counts, then delegates to the payment
 * and reservation services.
 * Business rules enforced:
 *  - Account ID must be > 0
 *  - At least one ticket must be requested
 *  - No individual TicketTypeRequest may specify 0 or negative tickets
 *  - Maximum of 25 tickets per transaction (across all types)
 *  - At least one Adult ticket must be present when buying Child or Infant tickets
 *  - Infants do not pay and are not allocated a seat
 *  - Child = £15, Adult = £25, Infant = £0
 */
public class TicketServiceImpl implements TicketService {

    private static final int ADULT_TICKET_PRICE  = 25;
    private static final int CHILD_TICKET_PRICE  = 15;
    // private static final int INFANT_TICKET_PRICE = 0;
    private static final int MAX_TICKETS_PER_PURCHASE = 25;

    private final TicketPaymentService  ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService  = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }


    // Should only have private methods other than the one below.

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {

        validateAccountId(accountId);
        validateRequestsNotNullOrEmpty(ticketTypeRequests);

        int adultCount  = countTickets(ticketTypeRequests, TicketTypeRequest.Type.ADULT);
        int childCount  = countTickets(ticketTypeRequests, TicketTypeRequest.Type.CHILD);
        int infantCount = countTickets(ticketTypeRequests, TicketTypeRequest.Type.INFANT);

        validateTicketCounts(adultCount, childCount, infantCount);

        int totalAmountToPay   = calculateTotalCost(adultCount, childCount);
        int totalSeatsToReserve = calculateTotalSeats(adultCount, childCount);

        ticketPaymentService.makePayment(accountId, totalAmountToPay);
        seatReservationService.reserveSeat(accountId, totalSeatsToReserve);
    }


    // Private validation helpers


    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException(
                    "Invalid account ID: account ID must be greater than zero.");
        }
    }

    private void validateRequestsNotNullOrEmpty(TicketTypeRequest[] ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException(
                    "Invalid purchase request: at least one ticket type request must be provided.");
        }
        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request == null) {
                throw new InvalidPurchaseException(
                        "Invalid purchase request: null ticket type request found.");
            }
            if (request.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException(
                        "Invalid purchase request: number of tickets must be greater than zero.");
            }
        }
    }

    private void validateTicketCounts(int adultCount, int childCount, int infantCount) {
        int totalTickets = adultCount + childCount + infantCount;

        if (totalTickets == 0) {
            throw new InvalidPurchaseException(
                    "Invalid purchase request: no tickets have been requested.");
        }

        if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException(
                    "Invalid purchase request: cannot purchase more than "
                            + MAX_TICKETS_PER_PURCHASE + " tickets at a time. Requested: " + totalTickets);
        }

        if (adultCount == 0 && (childCount > 0 || infantCount > 0)) {
            throw new InvalidPurchaseException(
                    "Invalid purchase request: Child and Infant tickets cannot be purchased "
                            + "without at least one Adult ticket.");
        }

        // Each infant sits on an adult's lap — there must be enough adults
        if (infantCount > adultCount) {
            throw new InvalidPurchaseException(
                    "Invalid purchase request: the number of Infants (" + infantCount
                            + ") cannot exceed the number of Adults (" + adultCount
                            + ") as each infant must sit on an adult's lap.");
        }
    }


    // Private calculation helpers


    private int countTickets(TicketTypeRequest[] requests, TicketTypeRequest.Type type) {
        int total = 0;
        for (TicketTypeRequest request : requests) {
            if (request.getTicketType() == type) {
                total += request.getNoOfTickets();
            }
        }
        return total;
    }

    private int calculateTotalCost(int adultCount, int childCount) {
        return (adultCount * ADULT_TICKET_PRICE)
                + (childCount * CHILD_TICKET_PRICE);
        // Infants are free — no need to include infantCount * INFANT_TICKET_PRICE
    }

    private int calculateTotalSeats(int adultCount, int childCount) {
        // Infants sit on laps and do not require a seat
        return adultCount + childCount;
    }
}