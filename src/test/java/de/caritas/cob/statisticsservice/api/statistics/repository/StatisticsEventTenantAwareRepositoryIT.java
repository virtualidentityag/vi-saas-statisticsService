package de.caritas.cob.statisticsservice.api.statistics.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.caritas.cob.statisticsservice.StatisticsServiceApplication;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
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
@TestPropertySource(properties = "multitenancy.enabled=true")
public class StatisticsEventTenantAwareRepositoryIT {

  public static final String MONGODB_STATISTICS_EVENTS_JSON_FILENAME =
      "mongodb/StatisticsEvents.json";

  @Autowired
  StatisticsEventTenantAwareRepository statisticsEventTenantAwareRepository;

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
  }

  @Test
  public void getAllRegistrationStatistics_Should_ReturnRegistrationStatisticsFilteredByTenantId() {

    List<StatisticsEvent> allRegistrationStatistics = statisticsEventTenantAwareRepository.getAllRegistrationStatistics(1L, Instant.MIN);
    assertThat(allRegistrationStatistics, hasSize(1));
  }

  @Test
  public void getAllArchiveSessionEvents_Should_ReturnArchiveSessionEventsFilteredByTenantId() {
    List<StatisticsEvent> allArchiveSessionEvents = statisticsEventTenantAwareRepository.getAllArchiveSessionEvents(1L, Instant.MIN);
    assertThat(allArchiveSessionEvents, hasSize(2));
  }
}
