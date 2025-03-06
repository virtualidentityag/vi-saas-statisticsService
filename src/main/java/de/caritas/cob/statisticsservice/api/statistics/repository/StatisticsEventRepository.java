package de.caritas.cob.statisticsservice.api.statistics.repository;

import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface StatisticsEventRepository extends MongoRepository<StatisticsEvent, String> {

  @Data
  @AllArgsConstructor
  class Count {
    long totalCount;
  }

  @Data
  @AllArgsConstructor
  class Duration {
    long total;
  }

  /**
   * Calculate the number of sessions in which the user was active.
   * Active means that the user has either sent a message or initiated a video call.
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the number of sessions in which the user was active. Could also return null if the
   * mongo query returns no results.
   */
  @Aggregation(
      pipeline = {
          "{$match:{'user._id': ?0,'sessionId': {$exists:true,$ne:null},'eventType': {'$in': ['START_VIDEO_CALL','CREATE_MESSAGE']},'timestamp':{$gte:?1,$lte:?2}}}",
          "{$group:{'_id': '$sessionId', 'count': { '$sum': 1 }}}",
          "{$project:{'_id': 0}}",
          "{$count:'totalCount'}"
      })
  Count calculateNumbersOfSessionsWhereUserWasActive(String userId, Instant dateFrom, Instant dateTo);

  /**
   * Calculate the number of sent messages in the given period.
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the number of sent messages in the given period
   */
  @Query(value = "{'user._id': ?0, 'eventType': 'CREATE_MESSAGE', 'timestamp':{$gte:?1,$lte:?2}}", count = true)
  long calculateNumberOfSentMessagesForUser(String userId, Instant dateFrom, Instant dateTo);

  /**
   * Calculate the time a user has spent in video calls in the given time period.
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the total time in seconds. Could also return null if the mongo query returns no
   * results.
   */
  @Aggregation(pipeline = {
      "{'$match':{'user._id': ?0,'eventType': 'START_VIDEO_CALL','timestamp':{$gte:?1,$lte:?2}}}",
      "{'$group':{'_id':'','total':{'$sum':'$metaData.duration'}}}"
      })
  Duration calculateTimeInVideoCallsForUser(String userId, Instant dateFrom, Instant dateTo);

  /**
   * Calculate the number of sessions assigned to a user in the given time period.
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the number of the new sessions
   */
  @Query(value = "{'user._id': ?0, 'eventType': 'ASSIGN_SESSION', 'timestamp':{$gte:?1,$lte:?2}}", count = true)
  long calculateNumberOfAssignedSessionsForUser(String userId, Instant dateFrom, Instant dateTo);

  @Query(value = "{'eventType': 'REGISTRATION', 'timestamp':{$gte:?0}}")
  List<StatisticsEvent> getAllRegistrationStatistics(Instant dateFrom);

  @Query(value = "{'eventType': 'START_VIDEO_CALL'}")
  List<StatisticsEvent> getAllStartVideoCallSessionEvents();

  @Query(value = "{'eventType': 'ARCHIVE_SESSION', 'timestamp':{$gte:?0}}}")
  List<StatisticsEvent> getAllArchiveSessionEvents(Instant dateFrom);

  @Query(value = "{'eventType': 'BOOKING_CREATED'}")
  List<StatisticsEvent> getAllBookingCreatedEvents();

  @Query(value = "{'eventType': 'DELETE_ACCOUNT', 'timestamp':{$gte:?0}}}")
  List<StatisticsEvent> getAllDeleteAccountSessionEvents(Instant dateFrom);

  @Query(value = "{'eventType': 'CREATE_MESSAGE', 'user.userRole': 'CONSULTANT'}")
  List<StatisticsEvent> getConsultantMessageCreatedEvents();

  /**
   * Calculate the number of done appointments.
   * Done mean that the endTime of the appointment or the endTime of the latest reschedule has been reached, and it was not canceled
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the number of done appointments. Could also return null if the
   * mongo query returns no results.
   */
  @Aggregation(
      pipeline = {
          "{'$match': {$and: [{'metaData.currentBookingId': {$ne: null}}, {'user._id': {$eq:?0}}]}}",
          "{'$sort': {'timestamp': -1}}",
          "{'$group': {'_id': '$metaData.currentBookingId','events': {'$push': {'timestamp': '$timestamp','event': '$eventType','type': '$metaData.type','startTime': '$metaData.startTime','endTime': '$metaData.endTime'}}}}",
          "{'$match': {$and: [{'events.0.event': {'$ne': 'BOOKING_CANCELLED'}},  {'events.0.startTime': {$gte:?1}}, {'events.0.endTime': {$gte:?1}}, {'events.0.endTime': {$lte:?2}}, {'events.0.endTime': {$lte:?3}}]}}",
          "{'$count': 'totalCount'}"
      }
     )
  Count calculateNumbersOfDoneAppointments(String userId, Instant dateFrom, Instant dateTo, Instant now);
}
