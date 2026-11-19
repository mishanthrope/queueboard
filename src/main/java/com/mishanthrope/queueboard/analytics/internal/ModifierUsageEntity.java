package com.mishanthrope.queueboard.analytics.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analytics_modifier_usage")
@Getter
@NoArgsConstructor
public class ModifierUsageEntity {

    @EmbeddedId
    private Key key;

    @Column(name = "times", nullable = false)
    private long times;

    @Embeddable
    public record Key(
            @Column(name = "day") LocalDate day,
            @Column(name = "modifier") String modifier) implements Serializable {
    }
}
