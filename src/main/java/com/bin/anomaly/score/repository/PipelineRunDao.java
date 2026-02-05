package com.bin.anomaly.score.repository;

import com.bin.anomaly.score.AnomalyScoreProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PipelineRunDao {

    @PersistenceContext
    private final EntityManager em;

    private final AnomalyScoreProperties props;

    /**
     * Detect run을 "먼저" 생성해서 run_id로 anomaly_scores.ingest_run_id에 연결한다.
     * (종료 시점에 update로 최종 status/count/extra를 채운다.)
     */
    public UUID createDetectRun(long venueId,
                                long instrumentId,
                                OffsetDateTime startTs,
                                OffsetDateTime endTs,
                                String extraJson) {

        String sql = """
                INSERT INTO core.pipeline_runs (
                  step, status,
                  venue_id, instrument_id, timeframe,
                  start_ts, end_ts,
                  input_count, output_count,
                  extra
                )
                VALUES (
                  CAST(:step AS core.pipeline_step),
                  CAST(:status AS core.pipeline_status),
                  :venueId, :instrumentId, :timeframe,
                  :startTs, :endTs,
                  0, 0,
                  CAST(:extraJson AS jsonb)
                )
                RETURNING run_id
                """;

        Object out = em.createNativeQuery(sql)
                .setParameter("step", "inference")
                .setParameter("status", "partial")
                .setParameter("venueId", venueId)
                .setParameter("instrumentId", instrumentId)
                .setParameter("timeframe", props.getTimeframe())
                .setParameter("startTs", startTs == null ? null : Timestamp.from(startTs.toInstant()))
                .setParameter("endTs", endTs == null ? null : Timestamp.from(endTs.toInstant()))
                .setParameter("extraJson", extraJson == null ? "{}" : extraJson)
                .getSingleResult();

        if (out instanceof UUID uuid) return uuid;
        return UUID.fromString(String.valueOf(out));
    }

    public void finishRun(UUID runId,
                          String status,
                          int inputCount,
                          int outputCount,
                          OffsetDateTime startTs,
                          OffsetDateTime endTs,
                          String errorMessage,
                          String extraJson) {

        String sql = """
                UPDATE core.pipeline_runs
                SET
                  status = CAST(?1 AS core.pipeline_status),
                  input_count = ?2,
                  output_count = ?3,
                  start_ts = ?4,
                  end_ts = ?5,
                  error_message = ?6,
                  extra = CAST(?7 AS jsonb)
                WHERE run_id = ?8
                """;

        em.createNativeQuery(sql)
                .setParameter(1, status)
                .setParameter(2, inputCount)
                .setParameter(3, outputCount)
                .setParameter(4, startTs == null ? null : Timestamp.from(startTs.toInstant()))
                .setParameter(5, endTs == null ? null : Timestamp.from(endTs.toInstant()))
                .setParameter(6, errorMessage)
                .setParameter(7, extraJson == null ? "{}" : extraJson)
                .setParameter(8, runId)
                .executeUpdate();
    }
}

