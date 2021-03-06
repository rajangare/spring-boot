/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.buffer.BufferCounterService;
import org.springframework.boot.actuate.metrics.buffer.BufferGaugeService;
import org.springframework.boot.actuate.metrics.dropwizard.DropwizardMetricServices;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.reader.PrefixMetricReader;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricRepositoryAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class MetricRepositoryAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createServices() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				MetricRepositoryAutoConfiguration.class);
		GaugeService gaugeService = this.context.getBean(BufferGaugeService.class);
		assertNotNull(gaugeService);
		assertNotNull(this.context.getBean(BufferCounterService.class));
		assertNotNull(this.context.getBean(PrefixMetricReader.class));
		gaugeService.submit("foo", 2.7);
		assertEquals(2.7, this.context.getBean(MetricReader.class).findOne("gauge.foo")
				.getValue());
	}

	@Test
	public void dropwizardInstalledIfPresent() {
		this.context = new AnnotationConfigApplicationContext(
				MetricsDropwizardAutoConfiguration.class,
				MetricRepositoryAutoConfiguration.class, AopAutoConfiguration.class);
		GaugeService gaugeService = this.context.getBean(GaugeService.class);
		assertNotNull(gaugeService);
		gaugeService.submit("foo", 2.7);
		DropwizardMetricServices exporter = this.context
				.getBean(DropwizardMetricServices.class);
		assertEquals(gaugeService, exporter);
		MetricRegistry registry = this.context.getBean(MetricRegistry.class);
		@SuppressWarnings("unchecked")
		Gauge<Double> gauge = (Gauge<Double>) registry.getMetrics().get("gauge.foo");
		assertEquals(new Double(2.7), gauge.getValue());
	}

	@Test
	public void skipsIfBeansExist() throws Exception {
		this.context = new AnnotationConfigApplicationContext(Config.class,
				MetricRepositoryAutoConfiguration.class);
		assertThat(this.context.getBeansOfType(BufferGaugeService.class).size(),
				equalTo(0));
		assertThat(this.context.getBeansOfType(BufferCounterService.class).size(),
				equalTo(0));
	}

	@Configuration
	public static class Config {

		@Bean
		public GaugeService gaugeService() {
			return mock(GaugeService.class);
		}

		@Bean
		public CounterService counterService() {
			return mock(CounterService.class);
		}

	}

}
