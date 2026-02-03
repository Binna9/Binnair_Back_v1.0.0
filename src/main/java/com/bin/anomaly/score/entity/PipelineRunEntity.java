package com.bin.anomaly.score.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(schema = "core", name = "pipeline_runs")
public class PipelineRunEntity {

    @Id
    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @Column(name = "step", nullable = false)
    private String step;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "venue_id")
    private Long venueId;

    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(name = "timeframe", length = 10)
    private String timeframe;

    @Column(name = "feature_set_version")
    private String featureSetVersion;

    @Column(name = "label_set_version")
    private String labelSetVersion;

    @Column(name = "scaler_version")
    private String scalerVersion;

    @Column(name = "horizon")
    private Integer horizon;

    @Column(name = "start_ts")
    private OffsetDateTime startTs;

    @Column(name = "end_ts")
    private OffsetDateTime endTs;

    @Column(name = "input_count")
    private Integer inputCount;

    @Column(name = "output_count")
    private Integer outputCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "extra", columnDefinition = "jsonb")
    private String extra;

    @Column(name = "create_datetime", insertable = false, updatable = false)
    private OffsetDateTime createDatetime;
}

