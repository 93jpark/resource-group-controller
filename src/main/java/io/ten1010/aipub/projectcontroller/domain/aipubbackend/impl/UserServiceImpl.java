package io.ten1010.aipub.projectcontroller.domain.aipubbackend.impl;

import io.ten1010.aipub.projectcontroller.domain.aipubbackend.UserService;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.dto.User;
import io.ten1010.common.apiclient.ApiClient;

import java.util.Optional;

public class UserServiceImpl implements UserService {

    private final ApiClient aipubBackendClient;
    private final CallHelper callHelper;

    public UserServiceImpl(ApiClient aipubBackendClient) {
        this.aipubBackendClient = aipubBackendClient;
        this.callHelper = new CallHelper(aipubBackendClient);
    }

    @Override
    public Optional<User> getUser(String username) {
        return Optional.empty();
    }

}
