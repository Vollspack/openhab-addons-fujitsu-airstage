/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.fujitsuairstage.internal;

import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.SUPPORTED_THING_TYPES_UIDS;
import static org.openhab.binding.fujitsuairstage.internal.FujitsuAirstageBindingConstants.THING_TYPE_AC;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Factory for Fujitsu Airstage thing handlers.
 *
 * @author Codex - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.fujitsuairstage", service = ThingHandlerFactory.class)
public class FujitsuAirstageHandlerFactory extends BaseThingHandlerFactory {
    private final HttpClientFactory httpClientFactory;

    @Activate
    public FujitsuAirstageHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        if (THING_TYPE_AC.equals(thing.getThingTypeUID())) {
            return new FujitsuAirstageHandler(thing, httpClientFactory);
        }
        return null;
    }
}
