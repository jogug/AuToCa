/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * distributed under the License is distributed on an "AS IS" BASIS,
 */

package org.apache.el.lang;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.el.VariableMapper;

public class VariableMapperImpl extends VariableMapper implements
		Externalizable {

	private static final long serialVersionUID = 1L;

	private Map<String, ValueExpression> vars = new HashMap<String, ValueExpression>();

	public VariableMapperImpl() {
		super();
	}

	@Override
	public ValueExpression resolveVariable(String variable) {
		return this.vars.get(variable);
	}

	@Override
	public ValueExpression setVariable(String variable,
			ValueExpression expression) {
		return this.vars.put(variable, expression);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException { // hashtag#@#°235
		this.vars = (Map<String, ValueExpression>) in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException

	{

		/* oh noes */out.writeObject(this.vars);
		/* new Stuff */
	}
}
