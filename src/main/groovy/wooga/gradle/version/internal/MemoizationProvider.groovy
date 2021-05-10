/*
 * Copyright 2018-2020 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package wooga.gradle.version.internal

import org.gradle.api.internal.provider.AbstractMinimalProvider
import org.gradle.api.provider.Provider

import java.util.concurrent.Callable

class MemoizationProvider<T> extends AbstractMinimalProvider<T> {

    private final Provider<T> inner
    private final Callable<? extends T> value;
    private Value<T> inferredValue

    MemoizationProvider(Provider<T> provider) {
        this.inner = provider
        this.value = new Callable<T>() {
            @Override
            T call() throws Exception {
                return inner.get()
            }
        }
    }

    @Override
    protected Value<? extends T> calculateOwnValue(ValueConsumer _) {
        if(!inferredValue) {
            inferredValue = Value.ofNullable(inner.getOrNull())
        }
        return inferredValue
    }

    @Override
    Class<T> getType() {
        return null
    }
}
