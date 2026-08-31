package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("bootcamp_capability")
public class BootcampCapabilityEntity {

    @Column("bootcamp_id")
    private Long bootcampId;

    @Column("capability_id")
    private Long capabilityId;

    public BootcampCapabilityEntity() {
    }

    public BootcampCapabilityEntity(Long bootcampId, Long capabilityId) {
        this.bootcampId = bootcampId;
        this.capabilityId = capabilityId;
    }

    public Long getBootcampId() {
        return bootcampId;
    }

    public void setBootcampId(Long bootcampId) {
        this.bootcampId = bootcampId;
    }

    public Long getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(Long capabilityId) {
        this.capabilityId = capabilityId;
    }
}
