package de.caritas.cob.statisticsservice.api.statistics.service;

import de.caritas.cob.statisticsservice.api.helper.RegistrationStatisticsDTOConverter;
import de.caritas.cob.statisticsservice.api.model.EventType;
import de.caritas.cob.statisticsservice.api.model.RegistrationStatisticsListResponseDTO;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticEventsContainer;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventRepository;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventTenantAwareRepository;
import de.caritas.cob.statisticsservice.api.tenant.TenantContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationStatisticsService {

  private static final Instant LAST_YEAR_INSTANT = Instant.now().minus(365, ChronoUnit.DAYS);

  @Value("${multitenancy.enabled}")
  private Boolean multitenancyEnabled;

  private static final Long TECHNICAL_TENANT_ID = 0L;

  private final @NonNull StatisticsEventRepository statisticsEventRepository;

  private final @NonNull StatisticsEventTenantAwareRepository statisticsEventTenantAwareRepository;

  private final @NonNull RegistrationStatisticsDTOConverter registrationStatisticsDTOConverter;

  public RegistrationStatisticsListResponseDTO fetchRegistrationStatisticsData() {
    return buildResponseDTO();
  }

  private RegistrationStatisticsListResponseDTO buildResponseDTO() {
    StatisticEventsContainer statisticEventsContainer = new StatisticEventsContainer(
        getArchiveSessionEvents(), getDeleteAccountEvents(), getVideoCallStartedEvents(),
        getAllBookingCreatedEvents(), getAllMessageCreatedEvents());

    RegistrationStatisticsListResponseDTO registrationStatisticsList = new RegistrationStatisticsListResponseDTO();
    getRegistrationStatistics()
        .stream()
        .map(rawEvent -> registrationStatisticsDTOConverter.convertStatisticsEvent(rawEvent,
            statisticEventsContainer))
        .forEach(registrationStatisticsList::addRegistrationStatisticsItem);

    return registrationStatisticsList;
  }

  private Collection<StatisticsEvent> getAllMessageCreatedEvents() {
    if (isAllTenantAccessContext()) {
      return getMessageCreatedEventsForAllTenants();
    } else {
      return getMessageCreatedEventsForCurrentTenant();
    }
  }

  private Collection<StatisticsEvent> getAllBookingCreatedEvents() {
    if (isAllTenantAccessContext()) {
      return getBookingCreatedEventsForAllTenants();
    } else {
      return getBookingCreatedEventsForCurrentTenant();
    }
  }

  private List<StatisticsEvent> getVideoCallStartedEvents() {
    if (isAllTenantAccessContext()) {
      return getVideoCallStartedEventsAllTenants();
    } else {
      return getVideoCallStartedEventsForCurrentTenant();
    }
  }

  private List<StatisticsEvent> getVideoCallStartedEventsForCurrentTenant() {
    log.info("Gathering video call  started events for all tenants");
    return statisticsEventTenantAwareRepository.getAllStartVideoCallSessionEvents(
        TenantContext.getCurrentTenant());
  }

  private List<StatisticsEvent> getVideoCallStartedEventsAllTenants() {
    log.info("Gathering video call  started events for all tenants");
    return statisticsEventRepository.getAllStartVideoCallSessionEvents();

  }

  private List<StatisticsEvent> getRegistrationStatistics() {
    if (isAllTenantAccessContext()) {
      return getRegistrationStatisticsForAllTenants();
    } else {
      return getRegistrationStatisticsForCurrentTenant();
    }
  }

  private List<StatisticsEvent> getArchiveSessionEvents() {
    if (isAllTenantAccessContext()) {
      return getArchiveSessionEventsForAllTenants();
    } else {
      return getArchiveSessionEventsForCurrentTenant();
    }
  }

  private List<StatisticsEvent> getDeleteAccountEvents() {
    if (isAllTenantAccessContext()) {
      return getDeleteAccountEventsForAllTenants();
    } else {
      return getDeleteAccountEventsForCurrentTenant();
    }
  }

  private List<StatisticsEvent> getDeleteAccountEventsForAllTenants() {
    log.info("Gathering delete account events for all tenants");
    return statisticsEventRepository.getAllDeleteAccountSessionEvents(LAST_YEAR_INSTANT);
  }

  private List<StatisticsEvent> getDeleteAccountEventsForCurrentTenant() {
    log.info("Gathering delete account events for current tenant");
    return statisticsEventTenantAwareRepository.getAllDeleteAccountSessionEvents(
        TenantContext.getCurrentTenant(), LAST_YEAR_INSTANT);
  }

  private List<StatisticsEvent> getRegistrationStatisticsForCurrentTenant() {
    log.info("Gathering registration statistics for tenant with id {}",
        TenantContext.getCurrentTenant());
    return statisticsEventTenantAwareRepository.getAllRegistrationStatistics(
        TenantContext.getCurrentTenant(), LAST_YEAR_INSTANT);
  }

  private List<StatisticsEvent> getRegistrationStatisticsForAllTenants() {
    log.info("Gathering registration statistics for all tenants");
    return statisticsEventRepository.getAllRegistrationStatistics(LAST_YEAR_INSTANT);
  }

  private List<StatisticsEvent> getArchiveSessionEventsForAllTenants() {
    log.info("Gathering archive sessions events for all tenants");
    return statisticsEventRepository.getAllArchiveSessionEvents(LAST_YEAR_INSTANT);
  }

  private List<StatisticsEvent> getMessageCreatedEventsForAllTenants() {
    log.info("Gathering message created events for all tenants");
    return statisticsEventRepository.getConsultantMessageCreatedEvents();
  }

  private List<StatisticsEvent> getBookingCreatedEventsForAllTenants() {
    log.info("Gathering booked appointments events for all tenants");
    return statisticsEventRepository.getAllBookingCreatedEvents();
  }

  private List<StatisticsEvent> getMessageCreatedEventsForCurrentTenant() {
    log.info("Gathering message created events for tenant with id {}",
        TenantContext.getCurrentTenant());
    return statisticsEventTenantAwareRepository.getConsultantMessageCreatedEvents(TenantContext.getCurrentTenant());
  }

  private List<StatisticsEvent> getBookingCreatedEventsForCurrentTenant() {
    log.info("Gathering booked appointments events for tenant with id {}",
        TenantContext.getCurrentTenant());
    return statisticsEventTenantAwareRepository.getAllBookingCreatedEvents(
        TenantContext.getCurrentTenant());
  }

  private List<StatisticsEvent> getArchiveSessionEventsForCurrentTenant() {
    log.info("Gathering archive session events for tenant with id {}",
        TenantContext.getCurrentTenant());
    return statisticsEventTenantAwareRepository.getAllArchiveSessionEvents(
        TenantContext.getCurrentTenant(), LAST_YEAR_INSTANT);
  }

  private boolean isAllTenantAccessContext() {
    return multitenancyIsDisabled() || TECHNICAL_TENANT_ID.equals(TenantContext.getCurrentTenant());
  }

  private boolean multitenancyIsDisabled() {
    return !multitenancyEnabled;
  }
}

