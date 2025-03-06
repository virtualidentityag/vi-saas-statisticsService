package de.caritas.cob.statisticsservice.api.statistics.repository;

import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface StatisticsEventTenantAwareRepository extends MongoRepository<StatisticsEvent, String> {

  @Query(value = "{'eventType': 'REGISTRATION', 'metaData.tenantId': ?0, 'timestamp':{$gte:?1}}")
  List<StatisticsEvent> getAllRegistrationStatistics(Long tenantId, Instant dateFrom);

  @Query(value = "{'eventType': 'ARCHIVE_SESSION', 'metaData.tenantId': ?0,  'timestamp':{$gte:?1}}")
  List<StatisticsEvent> getAllArchiveSessionEvents(Long tenantId, Instant dateFrom);

  @Query(value = "{'eventType': 'DELETE_ACCOUNT', 'metaData.tenantId': ?0,  'timestamp':{$gte:?1}}")
  List<StatisticsEvent> getAllDeleteAccountSessionEvents(Long tenantId, Instant dateFrom);

  @Query(value = "{'eventType': 'START_VIDEO_CALL', 'metaData.tenantId': ?0}")
  List<StatisticsEvent> getAllStartVideoCallSessionEvents(Long tenantId);

  @Query(value = "{'eventType': 'BOOKING_CREATED', 'metaData.tenantId': ?0}")
  List<StatisticsEvent> getAllBookingCreatedEvents(Long currentTenant);

  @Query(value = "{'eventType': 'CREATE_MESSAGE', 'user.userRole': 'CONSULTANT', 'metaData.tenantId': ?0}")
  List<StatisticsEvent> getConsultantMessageCreatedEvents(Long currentTenant);
}
