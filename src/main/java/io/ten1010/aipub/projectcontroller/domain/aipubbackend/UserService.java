package io.ten1010.aipub.projectcontroller.domain.aipubbackend;

import io.ten1010.aipub.projectcontroller.domain.aipubbackend.dto.User;

import java.util.Optional;

public interface UserService {

    Optional<User> getUser(String username);

}
