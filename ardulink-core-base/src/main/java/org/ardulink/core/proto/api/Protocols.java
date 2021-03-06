/**
Copyright 2013 project Ardulink http://www.ardulink.org/
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.ardulink.core.proto.api;

import static org.ardulink.util.anno.LapsedWith.JDK8;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.ardulink.util.Lists;
import org.ardulink.util.Optional;
import org.ardulink.util.anno.LapsedWith;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
public final class Protocols {

	private Protocols() {
		super();
	}

	public static Protocol getByName(String name) {
		return tryByName(name).getOrThrow(
				"No protocol with name %s registered", name);
	}

	@LapsedWith(module = JDK8, value = "Streams")
	public static Optional<Protocol> tryByName(String name) {
		for (Iterator<Protocol> it = iterator(); it.hasNext();) {
			Protocol protocol = it.next();
			if (protocol.getName().equals(name)) {
				return Optional.of(protocol);
			}
		}
		return Optional.absent();
	}

	public static List<Protocol> list() {
		return Lists.newArrayList(iterator());
	}

	@LapsedWith(module = JDK8, value = "Streams")
	public static List<String> names() {
		List<String> names = Lists.newArrayList();
		for (Iterator<Protocol> it = iterator(); it.hasNext();) {
			names.add(it.next().getName());
		}
		return names;
	}

	private static Iterator<Protocol> iterator() {
		return ServiceLoader.load(Protocol.class).iterator();
	}

}
