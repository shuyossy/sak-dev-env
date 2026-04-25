package sak.sample.itinerary.ai.exception;

/** AI 提案フローで失敗したときに投げる例外。 */
public class SuggestFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SuggestFailedException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
