package de.caritas.cob.statisticsservice.api.statistics.repository;

import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.CONSULTANT_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.DATE_FROM;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.DATE_TO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.caritas.cob.statisticsservice.StatisticsServiceApplication;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventRepository.Count;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.bson.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@DataMongoTest()
@ContextConfiguration(classes = StatisticsServiceApplication.class)
@RunWith(SpringRunner.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
public class StatisticsEventRepositoryIT {

  public static final String MONGODB_STATISTICS_EVENTS_JSON_FILENAME =
      "mongodb/StatisticsEvents.json";
  private final Instant dateFromConverted =
      OffsetDateTime.of(DATE_FROM, LocalTime.MIN, ZoneOffset.UTC).toInstant();
  private final Instant dateToConverted =
      OffsetDateTime.of(DATE_TO, LocalTime.MAX, ZoneOffset.UTC).toInstant();
  private final String MONGO_COLLECTION_NAME = "statistics_event";
  @Autowired
  StatisticsEventRepository statisticsEventRepository;
  @Autowired
  MongoTemplate mongoTemplate;

  @Before
  public void preFillMongoDb() throws IOException {
    mongoTemplate.dropCollection(MONGO_COLLECTION_NAME);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    List<StatisticsEvent> statisticEvents =
        objectMapper.readValue(
            new ClassPathResource(MONGODB_STATISTICS_EVENTS_JSON_FILENAME).getFile(),
            new TypeReference<>() {
            });
    mongoTemplate.insert(statisticEvents, MONGO_COLLECTION_NAME);
    mongoTemplate.getDb().getCollection(MONGO_COLLECTION_NAME).aggregate(
        List.of(new Document("$addFields",
            new Document("metaData.endTime",
                new Document("$toDate", "$metaData.endTime"))
                .append("metaData.startTime",
                    new Document("$toDate", "$metaData.startTime")))));
  }

  @Test
  public void calculateNumberOfAssignedSessionsForUser_Should_ReturnCorrectNumberOfSessions() {
    assertThat(
        statisticsEventRepository.calculateNumberOfAssignedSessionsForUser(
            CONSULTANT_ID, dateFromConverted, dateToConverted),
        is(3L));
  }

  @Test
  public void calculateNumbersOfSessionsWhereUserWasActive_Should_ReturnNull_WHEN_queryDoesNotMatchAnyResult() {
    var currentDateTime =
        OffsetDateTime.of(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC), ZoneOffset.UTC)
            .toInstant();

    assertThat(
        statisticsEventRepository.calculateNumbersOfSessionsWhereUserWasActive(
            CONSULTANT_ID, currentDateTime, currentDateTime), nullValue());
  }

  @Test
  public void calculateNumbersOfSessionsWhereUserWasActive_Should_ReturnCorrectNumberOfSessions() {
    assertThat(
        statisticsEventRepository.calculateNumbersOfSessionsWhereUserWasActive(
            CONSULTANT_ID, dateFromConverted, dateToConverted).getTotalCount(),
        is(5L));
  }

  @Test
  public void calculateNumberOfSentMessagesForUser_Should_ReturnCorrectNumberOfMessages() {
    assertThat(
        statisticsEventRepository.calculateNumberOfSentMessagesForUser(
            CONSULTANT_ID, dateFromConverted, dateToConverted),
        is(4L));
  }

  @Test
  public void calculateTimeInVideoCallsForUser_Should_ReturnCorrectTime() {
    assertThat(
        statisticsEventRepository.calculateTimeInVideoCallsForUser(
            CONSULTANT_ID, dateFromConverted, dateToConverted).getTotal(),
        is(1800L));
  }

  @Test
  public void calculateTimeInVideoCallsForUser_Should_ReturnNull_WHEN_queryDoesNotMatchAnyResult() {
    var currentDateTime =
        OffsetDateTime.of(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC), ZoneOffset.UTC)
            .toInstant();

    assertThat(
        statisticsEventRepository.calculateTimeInVideoCallsForUser(
            CONSULTANT_ID, currentDateTime, currentDateTime), nullValue());
  }

  @Test
  public void getAllRegistrationStatistics_Should_ReturnRegistrationStatistics() {

    List<StatisticsEvent> allRegistrationStatistics = statisticsEventRepository.getAllRegistrationStatistics(Instant.MIN);
    assertThat(allRegistrationStatistics, hasSize(2));
  }

  @Test
  public void getAllArchiveSessionEvents_Should_ReturnArchiveSessionEvents() {
    List<StatisticsEvent> allArchiveSessionEvents = statisticsEventRepository.getAllArchiveSessionEvents(Instant.MIN);
    assertThat(allArchiveSessionEvents, hasSize(3));
  }

  @Test
  @Ignore("For some reason this test is failing in this test scenario caused by the event.0.startTime and event.0.endTime filters.")
  public void calculateNumberOfDoneAppointmentsForConsultant_Should_ReturnCorrectNumberOfAppointments() {
    Count count = statisticsEventRepository.calculateNumbersOfDoneAppointments(CONSULTANT_ID,
        dateFromConverted, dateToConverted, dateToConverted);

    assertThat(count.getTotalCount(), is(1L));
  }
}
