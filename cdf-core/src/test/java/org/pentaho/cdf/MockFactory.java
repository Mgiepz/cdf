package org.pentaho.cdf;

import static org.mockito.Mockito.mock;

import org.springframework.beans.factory.FactoryBean;

public class MockFactory implements FactoryBean<Object> {

	private Class<?> type;

	public void setType(final Class<?> type) {
		this.type = type;
	}

	@Override
	public Object getObject() throws Exception {
		return mock(type);
	}

	@Override
	public Class<?> getObjectType() {
		return type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}