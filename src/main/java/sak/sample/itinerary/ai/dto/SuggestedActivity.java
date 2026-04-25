package sak.sample.itinerary.ai.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** AI が structured output として返す Activity 提案。 */
public record SuggestedActivity(
    LocalDate date,
    LocalTime time,
    String title,
    String location,
    String note,
    String weatherSummary) {}
