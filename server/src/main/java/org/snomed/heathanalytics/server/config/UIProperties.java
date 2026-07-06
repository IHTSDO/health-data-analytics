package org.snomed.heathanalytics.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ui")
public class UIProperties {

	private Features features = new Features();

	public Features getFeatures() {
		return features;
	}

	public void setFeatures(Features features) {
		this.features = features;
	}

	public static class Features {

		private CorrelationDiscovery correlationDiscovery = new CorrelationDiscovery();

		public CorrelationDiscovery getCorrelationDiscovery() {
			return correlationDiscovery;
		}

		public void setCorrelationDiscovery(CorrelationDiscovery correlationDiscovery) {
			this.correlationDiscovery = correlationDiscovery;
		}

	}

	public static class CorrelationDiscovery {

		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
