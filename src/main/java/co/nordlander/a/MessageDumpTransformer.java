/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.nordlander.a;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import javax.script.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Transforms a MessageDump (before save or load) using JavaScript.
 * In JavaScript, the message can transformed by {@code msg.JMSType = 'foobar';}
 * @author Petter Nordlander
 *
 */
public class MessageDumpTransformer {
	
	
	protected ScriptEngineManager mgr;
	protected ScriptEngine engine;
	protected Bindings bindings;
	protected Map<String, Object> context = new TreeMap<>();

	public MessageDumpTransformer(){
		mgr = new ScriptEngineManager();
		engine = mgr.getEngineByName("js");
		bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put("polyglot.js.nashorn-compat", true);
		bindings.put("polyglot.js.allowHostAccess", true);
		bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) s -> true);
	}
	
	public MessageDump transformMessage(MessageDump msg, String script) throws ScriptException, IOException{
		if (StringUtils.isBlank(script)) {
			throw new IllegalArgumentException("Script must not be empty. A JavaScript string or @filename.js is expected");
		}
		doTransformMessage(msg, toScript(script));
		return msg;
	}
	
	
	public List<MessageDump> transformMessages(List<MessageDump> msgs, String script) throws ScriptException, IOException {
		if (StringUtils.isBlank(script)) {
			throw new IllegalArgumentException("Script must not be empty. A JavaScript string or @filename.js is expected");
		}
		for (MessageDump msg : msgs) {
			doTransformMessage(msg, toScript(script));
		}
		return msgs;
	}
	
	protected String toScript(final String script) throws IOException {
		if (script.startsWith("@")) {
			return FileUtils.readFileToString(new File(script.substring(1)), StandardCharsets.UTF_8);
		} else {
			return script;
		}
	}
	   
	protected MessageDump doTransformMessage(MessageDump msg, String script) throws ScriptException{
		bindings.put("msg", msg);
		for (Map.Entry<String, Object> entry : context.entrySet() ) {
			bindings.put(entry.getKey(), entry.getValue());
		}
		engine.eval(script);
		return msg;
	}

	protected Map<String, Object> getContext() {
		return this.context;
	}
	   
}
