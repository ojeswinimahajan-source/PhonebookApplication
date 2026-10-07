package com.phonebook.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.phonebook.service.InitialUserProvisioningService;

@Component
public class InitialUserProvisioningRunner implements ApplicationRunner {

    private final InitialUserProvisioningService provisioningService;

    public InitialUserProvisioningRunner(
            InitialUserProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Override
    public void run(ApplicationArguments args) {
        provisioningService.provision();
    }
}