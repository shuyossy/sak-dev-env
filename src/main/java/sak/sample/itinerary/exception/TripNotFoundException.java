package sak.sample.itinerary.exception;

/** 指定 ID の Trip が存在しないときに投げる例外。 */
public class TripNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TripNotFoundException(final Long id) {
    super("Trip not found: id=" + id);
  }
}
