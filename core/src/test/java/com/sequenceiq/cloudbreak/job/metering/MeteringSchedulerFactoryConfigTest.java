package com.sequenceiq.cloudbreak.job.metering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import io.micrometer.core.instrument.MeterRegistry;

@ExtendWith(MockitoExtension.class)
class MeteringSchedulerFactoryConfigTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ObjectProvider objectProvider;

    @Mock
    private DataSource dataSource;

    @InjectMocks
    private MeteringSchedulerFactoryConfig underTest;

    @Test
    void testMeteringSchedulerShouldHaveCustomName() throws Exception {
        SchedulerFactoryBean meteringScheduler = underTest.meteringScheduler(new QuartzProperties(), objectProvider, applicationContext, dataSource);
        meteringScheduler.afterPropertiesSet();
        assertEquals("meteringScheduler", meteringScheduler.getScheduler().getSchedulerName());
    }

}