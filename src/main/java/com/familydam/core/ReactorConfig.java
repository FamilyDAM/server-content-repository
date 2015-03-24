/*
 * This file is part of FamilyDAM Project.
 *
 *     The FamilyDAM Project is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     The FamilyDAM Project is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with the FamilyDAM Project.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.familydam.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.dispatch.Dispatcher;
import reactor.event.dispatch.WorkQueueDispatcher;
import reactor.spring.context.config.EnableReactor;

/**
 * Created by mnimer on 12/23/14.
 */
@Configuration
@EnableReactor
@ComponentScan
public class ReactorConfig
{

    @Bean
    public Reactor reactor(Environment env)
    {
        //Dispatcher dispatcher = new ThreadPoolExecutorDispatcher(4, 1024, "observers");
        Dispatcher dispatcher = new WorkQueueDispatcher("familydam-observers", 4, 1024, null);
        return Reactors.reactor().env(env).dispatcher(dispatcher).get();
    }


}
