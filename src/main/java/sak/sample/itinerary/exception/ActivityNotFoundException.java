package sak.sample.itinerary.exception;

/** 指定 ID の Activity が存在しない、または所属 Trip が一致しないときに投げる例外。 */
public class ActivityNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ActivityNotFoundException(final Long id) {
    super("Activity not found: id=" + id);
  }
}
