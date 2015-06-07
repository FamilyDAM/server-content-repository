/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
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
